#!/usr/bin/env kotlin

@file:DependsOn("com.auth0:java-jwt:4.4.0")

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

fun generateToken(): String {
    return JWT.create()
        .withIssuer("auctor-auth")
        .withAudience("execution-service")
        .withSubject("test-user")
        .withClaim("roles", listOf("EXECUTOR"))
        .sign(Algorithm.HMAC256("dev-secret-change-later"))
}

println(generateToken())
