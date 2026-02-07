package com.auctor.execution.graphql

import com.apurebase.kgraphql.GraphQL
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import com.auctor.execution.grpc.DefinitionGrpcClient
import io.ktor.server.application.*

fun Application.configureGraphQL() {

    val grpcClient = DefinitionGrpcClient()
    val resolver = DefinitionQueryResolver(grpcClient)

    install(GraphQL) {
        playground = true

        schema {
            configureSchema(resolver)
        }
    }
}

private fun SchemaBuilder.configureSchema(
    resolver: DefinitionQueryResolver
) {
    query("definition") {
        resolver { ctx: GraphQLContext, id: String ->
            resolver.definition(ctx, id)
        }
    }
}
