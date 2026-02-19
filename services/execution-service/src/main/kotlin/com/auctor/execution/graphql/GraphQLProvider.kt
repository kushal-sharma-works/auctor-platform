package com.auctor.execution.graphql

import com.auctor.execution.cache.CacheService
import com.auctor.execution.domain.ExecutionEngine
import com.auctor.execution.domain.ExecutionRepository
import com.auctor.execution.domain.AuditRepository
import com.auctor.execution.domain.Execution
import com.auctor.execution.domain.AuditEvent
import com.auctor.execution.domain.ExecutionNotFoundException
import com.auctor.execution.domain.StateTransitionRequest
import com.auctor.execution.domain.ExecutionId
import com.auctor.execution.security.AuthContext
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.execution.AsyncExecutionStrategy
import graphql.schema.DataFetcher
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.future
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.CompletableFuture

private val logger = LoggerFactory.getLogger("GraphQLProvider")

/**
 * GraphQL provider using graphql-java library.
 * Serves GraphQL queries and mutations for workflows and executions.
 */
class GraphQLProvider(
    private val cacheService: CacheService,
    private val executionEngine: ExecutionEngine,
    private val executionRepository: ExecutionRepository,
    private val auditRepository: AuditRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val graphQL: GraphQL = build()

    private class AuthzException(message: String) : RuntimeException(message)
    private class GraphQlClientException(message: String) : RuntimeException(message)

    private fun requireRole(authContext: AuthContext?, requiredRole: String) {
        if (authContext == null) {
            throw AuthzException("FORBIDDEN: missing auth context")
        }
        val roles = authContext.roles
        val effectiveRoles = (roles + "EXECUTOR").distinct()
        if (effectiveRoles.contains("ADMIN") || effectiveRoles.contains(requiredRole)) {
            return
        }
        if (requiredRole == "VIEWER" && effectiveRoles.contains("EXECUTOR")) {
            return
        }
        throw AuthzException("FORBIDDEN: missing role $requiredRole")
    }

    private fun authContextFromEnv(env: graphql.schema.DataFetchingEnvironment): AuthContext? {
        val context = env.getContext<Map<String, Any>?>()
        return context?.get("authContext") as? AuthContext
    }

    private fun resolveActor(authContext: AuthContext?): String {
        val email = authContext?.email?.trim()
        if (!email.isNullOrBlank()) {
            return email
        }
        val subject = authContext?.subject?.trim()
        return if (!subject.isNullOrBlank() && subject != "unknown") subject else "graphql-user"
    }

    private fun throwGraphQlOperationException(operation: String, e: Exception): Nothing {
        if (e is CancellationException) {
            throw e
        }

        when (e) {
            is AuthzException,
            is GraphQlClientException -> throw e
            is ExecutionNotFoundException -> throw GraphQlClientException(e.message ?: "$operation failed")
            is IllegalArgumentException,
            is IllegalStateException -> throw GraphQlClientException(e.message ?: "$operation failed")
            else -> {
                logger.error("Unexpected error in GraphQL operation: $operation", e)
                throw RuntimeException("$operation failed due to a temporary internal issue")
            }
        }
    }

    private fun build(): GraphQL {
        val schemaStream = javaClass.classLoader.getResourceAsStream("graphql/schema.graphqls")
            ?: throw IllegalStateException("GraphQL schema file not found")
        val document = SchemaParser().parse(schemaStream)

        val wiring = RuntimeWiring.newRuntimeWiring()
            // Workflow Query
            .type("Query") { type ->
                type.dataFetcher("getWorkflow", getWorkflowFetcher())
                // Execution Queries
                type.dataFetcher("listExecutions", listExecutionsFetcher())
                type.dataFetcher("getExecution", getExecutionFetcher())
                type.dataFetcher("getAuditTrail", getAuditTrailFetcher())
            }
            // Mutations
            .type("Mutation") { type ->
                type.dataFetcher("startExecution", startExecutionFetcher())
                type.dataFetcher("advanceExecution", advanceExecutionFetcher())
            }
            // Type resolvers for nested data
            .type("Execution") { type ->
                type.dataFetcher("auditEvents", auditEventsFetcher())
            }
            .build()

        val schema = SchemaGenerator().makeExecutableSchema(document, wiring)

        return GraphQL.newGraphQL(schema)
            .queryExecutionStrategy(AsyncExecutionStrategy())
            .build()
    }

    /**
     * Workflow fetcher (existing).
     */
    private fun getWorkflowFetcher(): DataFetcher<CompletableFuture<Map<String, Any?>?>> =
        DataFetcher { env ->
            val id = env.getArgument<String?>("id") ?: ""
            val version = env.getArgument<Int?>("version") ?: 1
            val context = env.getContext<Map<String, Any>?>()
            val authContext = authContextFromEnv(env)
            requireRole(authContext, "VIEWER")
            val authHeader = authContext?.rawToken ?: (context?.get("authorization") as? String)

            scope.future {
                val workflow = cacheService.getWorkflowCached(id, version, authHeader)
                workflow?.let {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "version" to it.version,
                        "states" to it.states.toList()
                    )
                }
            }
        }

    /**
     * List executions fetcher.
     */
    private fun listExecutionsFetcher(): DataFetcher<CompletableFuture<Map<String, Any?>>> =
        DataFetcher { env ->
            val limit = env.getArgument<Int?>("limit") ?: 20
            val offset = env.getArgument<Int?>("offset") ?: 0
            val authContext = authContextFromEnv(env)
            requireRole(authContext, "VIEWER")

            scope.future {
                try {
                    val executions = executionRepository.findAll(limit, offset)
                    val total = executions.size // Note: In production, you'd want a count method

                    mapOf(
                        "limit" to limit,
                        "offset" to offset,
                        "total" to total,
                        "items" to executions.map { formatExecution(it) }
                    )
                } catch (e: Exception) {
                    throwGraphQlOperationException("Failed to list executions", e)
                }
            }
        }

    /**
     * Get single execution fetcher.
     */
    private fun getExecutionFetcher(): DataFetcher<CompletableFuture<Map<String, Any?>?>> =
        DataFetcher { env ->
            val executionId = env.getArgument<String>("id")
            val authContext = authContextFromEnv(env)
            requireRole(authContext, "VIEWER")

            scope.future {
                try {
                    val execution = executionRepository.findById(ExecutionId(executionId))
                    execution?.let { formatExecution(it) }
                        ?: throw ExecutionNotFoundException("Execution $executionId not found")
                } catch (e: Exception) {
                    throwGraphQlOperationException("Failed to fetch execution $executionId", e)
                }
            }
        }

    /**
     * Get audit trail for execution.
     */
    private fun getAuditTrailFetcher(): DataFetcher<CompletableFuture<List<Map<String, Any?>>>> =
        DataFetcher { env ->
            val executionId = env.getArgument<String>("executionId")
            val authContext = authContextFromEnv(env)
            requireRole(authContext, "VIEWER")

            scope.future {
                try {
                    val auditEvents = auditRepository.findByExecutionId(executionId)
                    auditEvents.map { formatAuditEvent(it) }
                } catch (e: Exception) {
                    throwGraphQlOperationException("Failed to fetch audit trail for $executionId", e)
                }
            }
        }

    /**
     * Nested audit events fetcher for Execution type.
     */
    private fun auditEventsFetcher(): DataFetcher<CompletableFuture<List<Map<String, Any?>>>> =
        DataFetcher { env ->
            val execution = env.getSource<Map<String, Any?>>()
            val executionId = execution["id"] as? String ?: return@DataFetcher CompletableFuture.completedFuture(emptyList())
            val authContext = authContextFromEnv(env)
            requireRole(authContext, "VIEWER")

            scope.future {
                try {
                    val auditEvents = auditRepository.findByExecutionId(executionId)
                    auditEvents.map { formatAuditEvent(it) }
                } catch (e: Exception) {
                    throwGraphQlOperationException("Failed to fetch audit events for $executionId", e)
                }
            }
        }

    /**
     * Start execution mutation.
     */
    private fun startExecutionFetcher(): DataFetcher<CompletableFuture<Map<String, Any?>>> =
        DataFetcher { env ->
            val input = env.getArgument<Map<String, Any?>>("input")
            val workflowId = input?.get("workflowId") as? String ?: ""
            val workflowVersion = (input?.get("workflowVersion") as? Number)?.toInt() ?: 1
            @Suppress("UNCHECKED_CAST")
            val inputList = input?.get("input") as? List<Map<String, Any?>> ?: emptyList()
            val inputMap = inputList.associate { item ->
                val key = item["key"] as? String ?: ""
                val value = item["value"] as? String ?: ""
                key to value
            }
            val context = env.getContext<Map<String, Any>?>()
            val authContext = authContextFromEnv(env)
            requireRole(authContext, "EXECUTOR")
            val authHeader = authContext?.rawToken ?: (context?.get("authorization") as? String)
            val actor = resolveActor(authContext)

            scope.future {
                try {
                    val execution = executionEngine.startExecution(
                        workflowId = workflowId,
                        workflowVersion = workflowVersion,
                        input = inputMap,
                        actor = actor,
                        authHeader = authHeader
                    )
                    formatExecution(execution)
                } catch (e: Exception) {
                    throwGraphQlOperationException("Failed to start execution", e)
                }
            }
        }

    /**
     * Advance execution mutation.
     */
    private fun advanceExecutionFetcher(): DataFetcher<CompletableFuture<Map<String, Any?>>> =
        DataFetcher { env ->
            val executionId = env.getArgument<String>("executionId")
            val input = env.getArgument<Map<String, String>?>("input")
            val correlationId = input?.get("correlationId") as? String ?: UUID.randomUUID().toString()
            val context = env.getContext<Map<String, Any>?>()
            val authContext = authContextFromEnv(env)
            requireRole(authContext, "EXECUTOR")
            val authHeader = authContext?.rawToken ?: (context?.get("authorization") as? String)
            val actor = resolveActor(authContext)

            scope.future {
                try {
                    val stateTransitionRequest = StateTransitionRequest(
                        executionId = ExecutionId(executionId),
                        actor = actor,
                        correlationId = correlationId
                    )
                    val execution = executionEngine.advanceExecution(stateTransitionRequest, authHeader)
                    formatExecution(execution)
                } catch (e: Exception) {
                    throwGraphQlOperationException("Failed to advance execution $executionId", e)
                }
            }
        }

    /**
     * Helper to format execution as GraphQL response.
     */
    private fun formatExecution(execution: Execution): Map<String, Any?> {
        return mapOf(
            "id" to execution.id.value,
            "workflowId" to execution.workflowId,
            "workflowVersion" to execution.workflowVersion,
            "currentState" to execution.currentState,
            "status" to formatStatus(execution.status),
            "input" to execution.input.map { (key, value) ->
                mapOf<String, String>("key" to key, "value" to value)
            }.toList(),
            "auditEvents" to emptyList<Map<String, Any?>>(),
            "createdAt" to execution.createdAt.toString(),
            "updatedAt" to execution.updatedAt.toString()
        )
    }

    /**
     * Helper to format ExecutionStatus sealed class.
     */
    private fun formatStatus(status: com.auctor.execution.domain.ExecutionStatus): Map<String, Any?> {
        return when (status) {
            is com.auctor.execution.domain.ExecutionStatus.Running -> 
                mapOf("type" to "RUNNING", "reason" to null)
            is com.auctor.execution.domain.ExecutionStatus.Completed -> 
                mapOf("type" to "COMPLETED", "reason" to null)
            is com.auctor.execution.domain.ExecutionStatus.Failed -> 
                mapOf("type" to "FAILED", "reason" to status.reason)
            is com.auctor.execution.domain.ExecutionStatus.Suspended -> 
                mapOf("type" to "SUSPENDED", "reason" to null)
        }
    }

    /**
     * Helper to format audit event as GraphQL response.
     */
    private fun formatAuditEvent(event: AuditEvent): Map<String, Any?> {
        return mapOf(
            "id" to event.id,
            "executionId" to event.executionId,
            "eventType" to event.eventType.name,
            "actor" to event.actor,
            "details" to buildDetails(event),
            "timestamp" to event.timestamp.toString()
        )
    }

    /**
     * Build details string from AuditEvent fields.
     */
    private fun buildDetails(event: AuditEvent): String {
        val details = mutableMapOf<String, Any?>()
        if (event.fromState != null) details["fromState"] = event.fromState
        if (event.toState != null) details["toState"] = event.toState
        if (event.policyId != null) details["policyId"] = event.policyId
        if (event.policyResult != null) details["policyResult"] = event.policyResult
        if (event.explanation != null) details["explanation"] = event.explanation
        return details.toString()
    }

    fun execute(
        query: String,
        variables: Map<String, Any?>?,
        context: Map<String, Any>?
    ): Map<String, Any?> {
        val input = ExecutionInput.newExecutionInput()
            .query(query)
            .variables(variables ?: emptyMap())
            .context(context)
            .build()

        val result = try {
            graphQL.execute(input)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            logger.error("GraphQL execution failed unexpectedly", t)
            return mapOf(
                "data" to null,
                "errors" to listOf(
                    mapOf<String, Any?>(
                        "message" to "Request failed while processing GraphQL operation",
                        "locations" to emptyList<Map<String, Int>>()
                    )
                )
            )
        }

        val errors = if (result.errors.isEmpty()) {
            null
        } else {
            result.errors.map { error ->
                val locationsMap: List<Map<String, Int>>? = error.locations?.map { loc ->
                    mapOf<String, Int>(
                        "line" to loc.line,
                        "column" to loc.column
                    )
                }
                mapOf<String, Any?>(
                    "message" to error.message,
                    "locations" to (locationsMap ?: emptyList<Map<String, Int>>())
                )
            }
        }

        return mapOf(
            "data" to result.getData<Any?>(),
            "errors" to errors
        )
    }
}
