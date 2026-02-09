package com.auctor.execution.security

import io.ktor.server.auth.jwt.*

data class AuthContext(
    val userId: String,
    val roles: List<String>
)

fun JWTPrincipal.toAuthContext(): AuthContext =
    AuthContext(
        userId = payload.subject ?: "unknown",
        roles = payload.getClaim("roles")?.asList(String::class.java) ?: emptyList()
    )
