package com.auctor.execution.security

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.util.*

val AuthContextKey = AttributeKey<AuthContext>("auth-context")

val AuthContextPlugin = createApplicationPlugin(name = "AuthContextPlugin") {
    onCall { call ->
        val principal = call.principal<JWTPrincipal>() ?: return@onCall
        val rawToken = call.request.headers["Authorization"]
        call.attributes.put(AuthContextKey, principal.toAuthContext(rawToken))
    }
}

fun ApplicationCall.authContextOrNull(): AuthContext? =
    if (attributes.contains(AuthContextKey)) attributes[AuthContextKey] else null
