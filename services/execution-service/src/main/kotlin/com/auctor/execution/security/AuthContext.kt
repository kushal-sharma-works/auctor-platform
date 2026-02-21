package com.auctor.execution.security

import io.ktor.server.auth.jwt.*

data class AuthContext(
    val subject: String,
    val email: String?,
    val roles: List<String>,
    val rawToken: String?
)

fun JWTPrincipal.toAuthContext(rawToken: String?): AuthContext =
    AuthContext(
        subject = payload.subject ?: "unknown",
        email = payload.getClaim("email")?.asString()?.trim()?.ifBlank { null },
        roles = run {
            // Intentional for this project's local/demo model:
            // all authenticated users get EXECUTOR by default.
            val claimRoles = payload.getClaim("roles")?.asList(String::class.java) ?: emptyList()
            (claimRoles + "EXECUTOR").distinct()
        },
        rawToken = rawToken
    )
