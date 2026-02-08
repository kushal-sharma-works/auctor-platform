package com.auctor.execution.graphql

import com.auctor.execution.cache.CacheService
import com.auctor.execution.grpc.WorkflowDto
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
import java.util.concurrent.CompletableFuture

/**
 * GraphQL provider using graphql-java library.
 * Serves GraphQL queries for workflow definitions.
 */
class GraphQLProvider(
    private val cacheService: CacheService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val graphQL: GraphQL = build()

    private fun build(): GraphQL {
        val schemaStream = javaClass.classLoader.getResourceAsStream("graphql/schema.graphqls")
            ?: throw IllegalStateException("GraphQL schema file not found")
        val document = SchemaParser().parse(schemaStream)

        val wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query") { type ->
                type.dataFetcher("getWorkflow", getWorkflowFetcher())
            }
            .build()

        val schema = SchemaGenerator().makeExecutableSchema(document, wiring)

        return GraphQL.newGraphQL(schema)
            .queryExecutionStrategy(AsyncExecutionStrategy())
            .build()
    }

    /**
     * Async data fetcher for workflows.
     */
    private fun getWorkflowFetcher(): DataFetcher<CompletableFuture<WorkflowDto?>> =
        DataFetcher { env ->
            val id = env.getArgument<String?>("id") ?: ""
            val version = env.getArgument<Int?>("version") ?: 1
            val context = env.getContext<Map<String, Any>?>()
            val authHeader = context?.get("authorization") as? String

            scope.future {
                cacheService.getWorkflowCached(id, version, authHeader)
            }
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

        return mapOf(
            "data" to result.getData<Any?>(),
            "errors" to result.errors.takeIf { it.isNotEmpty() }
        )
    }
}
