package com.auctor.execution.graphql

import com.auctor.execution.grpc.DefinitionGrpcClient

class DefinitionQueryResolver(
    private val grpcClient: DefinitionGrpcClient
) {

    suspend fun definition(
        ctx: GraphQLContext,
        id: String
    ) = grpcClient.getDefinition(
        id = id,
        authHeader = "Bearer ${ctx.jwtPrincipal.subject}"
    )
}
