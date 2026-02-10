package com.auctor.execution.security

import io.ktor.server.auth.jwt.*

val JWTPrincipal.subjectId: String
    get() = payload.subject ?: "unknown"

val JWTPrincipal.roles: List<String>
    get() = payload.getClaim("roles")?.asList(String::class.java) ?: emptyList()
