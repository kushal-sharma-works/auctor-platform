package com.auctor.execution.graphql

import com.auctor.execution.cache.CacheService
import com.auctor.execution.domain.ExecutionEngine
import com.auctor.execution.domain.ExecutionRepository
import com.auctor.execution.domain.AuditRepository
import com.auctor.execution.domain.Execution
import com.auctor.execution.domain.AuditEvent
import com.auctor.execution.domain.StateTransitionRequest
import com.auctor.execution.domain.ExecutionId
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.execution.AsyncExecutionStrategy
import graphql.schema.DataFetcher
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val graphQL: GraphQL = build()

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
            val authHeader = context?.get("authorization") as? String

            scope.future {
                val workflow = cacheService.getWorkflowCached(id, version, authHeader)
                workflow?.let {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "version" to it.version,
                        "states" to it.states
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
                    logger.error("Error listing executions", e)
                    mapOf(
                        "limit" to limit,
                        "offset" to offset,
                        "total" to 0,
                        "items" to emptyList<Map<String, Any?>>()
                    )
                }
            }
        }

    /**
     * Get single execution fetcher.
     */
    private fun getExecutionFetcher(): DataFetcher<CompletableFuture<Map<String, Any?>?>> =
        DataFetcher { env ->
            val executionId = env.getArgument<String>("id")

            scope.future {
                try {
                    val execution = executionRepository.findById(ExecutionId(executionId))
                    execution?.let { formatExecution(it) }
                } catch (e: Exception) {
                    logger.error("Error fetching execution $executionId", e)
                    null
                }
            }
        }

    /**
     * Get audit trail for execution.
     */
    private fun getAuditTrailFetcher(): DataFetcher<CompletableFuture<List<Map<String, Any?>>>> =
        DataFetcher { env ->
            val executionId = env.getArgument<String>("executionId")

            scope.future {
                try {
                    val auditEvents = auditRepository.findByExecutionId(executionId)
                    auditEvents.map { formatAuditEvent(it) }
                } catch (e: Exception) {
                    logger.error("Error fetching audit trail for $executionId", e)
                    emptyList()
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

            scope.future {
                try {
                    val auditEvents = auditRepository.findByExecutionId(executionId)
                    auditEvents.map { formatAuditEvent(it) }
                } catch (e: Exception) {
                    logger.error("Error fetching audit events for $executionId", e)
                    emptyList()
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
            val authHeader = context?.get("authorization") as? String

            scope.future {
                try {
                    val execution = executionEngine.startExecution(
                        workflowId = workflowId,
                        workflowVersion = workflowVersion,
                        input = inputMap,
                        actor = "graphql-user",
                        authHeader = authHeader
                    )
                    formatExecution(execution)
                } catch (e: Exception) {
                    logger.error("Error starting execution", e)
                    mapOf(
                        "id" to UUID.randomUUID().toString(),
                        "workflowId" to workflowId,
                        "workflowVersion" to workflowVersion,
                        "currentState" to "ERROR",
                        "status" to mapOf("type" to "ERROR", "reason" to e.message),
                        "input" to emptyList<Map<String, String>>(),
                        "auditEvents" to emptyList<Map<String, Any?>>(),
                        "createdAt" to "",
                        "updatedAt" to ""
                    )
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
            val authHeader = context?.get("authorization") as? String

            scope.future {
                try {
                    val stateTransitionRequest = StateTransitionRequest(
                        executionId = ExecutionId(executionId),
                        actor = "graphql-user",
                        correlationId = correlationId
                    )
                    val execution = executionEngine.advanceExecution(stateTransitionRequest, authHeader)
                    formatExecution(execution)
                } catch (e: Exception) {
                    logger.error("Error advancing execution $executionId", e)
                    mapOf(
                        "id" to executionId,
                        "workflowId" to "",
                        "workflowVersion" to 0,
                        "currentState" to "ERROR",
                        "status" to mapOf("type" to "ERROR", "reason" to e.message),
                        "input" to emptyList<Map<String, String>>(),
                        "auditEvents" to emptyList<Map<String, Any?>>(),
                        "createdAt" to "",
                        "updatedAt" to ""
                    )
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
                mapOf("key" to key, "value" to value)
            },
            "auditEvents" to emptyList<Map<String, Any?>>(), // Will be populated by nested resolver
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

        val result = graphQL.execute(input)

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
