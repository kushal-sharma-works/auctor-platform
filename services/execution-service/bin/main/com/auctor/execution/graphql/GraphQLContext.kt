package com.auctor.execution.graphql

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

data class GraphQLContext(
    val jwtPrincipal: JWTPrincipal
)

fun ApplicationCall.graphqlContext(): GraphQLContext {
    val jwtPrincipal = this.principal<JWTPrincipal>()
        ?: error("Unauthenticated GraphQL access")

    return GraphQLContext(jwtPrincipal)
}
