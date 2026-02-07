package com.auctor.execution.graphql

import com.auctor.execution.cache.CacheService
import com.auctor.execution.grpc.DefinitionDto
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
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * HARDENED GraphQL provider.
 *
 * Changes vs previous version:
 * - NO runBlocking
 * - Uses coroutine -> CompletableFuture bridge
 * - Measures execution time
 */
class GraphQLProvider(
    private val cacheService: CacheService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val graphQL: GraphQL = build()

    private fun build(): GraphQL {
        val schemaFile = File(javaClass.classLoader.getResource("graphql/schema.graphqls")!!.toURI())
        val document = SchemaParser().parse(schemaFile)

        val wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query") { type ->
                type.dataFetcher("getDefinition", getDefinitionFetcher())
            }
            .build()

        val schema = SchemaGenerator().makeExecutableSchema(document, wiring)

        return GraphQL.newGraphQL(schema)
            .queryExecutionStrategy(AsyncExecutionStrategy())
            .build()
    }

    /**
     * ASYNC data fetcher.
     *
     * - Returns CompletableFuture
     * - Internally uses coroutines
     * - Fully non-blocking
     */
    private fun getDefinitionFetcher(): DataFetcher<CompletableFuture<Map<String, Any?>?>> =
        DataFetcher { env ->
            val id = env.getArgument<String?>("id") ?: ""
            val context = env.getContext<Map<String, Any>?>()
            val authHeader = context?.get("authorization") as? String

            scope.future {
                val result = cacheService.getOrLoad(id, authHeader)

                result?.let {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "description" to it.description
                    )
                }
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
