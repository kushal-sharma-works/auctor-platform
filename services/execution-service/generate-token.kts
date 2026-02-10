#!/usr/bin/env kotlin

@file:DependsOn("com.auth0:java-jwt:4.4.0")

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.io.File

fun readConfigValue(key: String): String? {
    val file = File("src/main/resources/application.conf")
    if (!file.exists()) return null
    return file.readLines()
        .firstOrNull { it.trim().startsWith("$key") }
        ?.substringAfter("=")
        ?.trim()
        ?.trim('"')
}

fun generateToken(): String {
    val issuer = System.getenv("EXECUTION_JWT_ISSUER")
        ?: readConfigValue("issuer")
        ?: "auctor-auth"
    val audience = System.getenv("EXECUTION_JWT_AUDIENCE")
        ?: readConfigValue("audience")
        ?: "execution-service"
    val secret = System.getenv("EXECUTION_JWT_SECRET")
        ?: readConfigValue("secret")
        ?: "dev-secret-change-later"
    val subject = System.getenv("EXECUTION_JWT_SUBJECT") ?: "test-user"

    return JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withSubject(subject)
        .withClaim("roles", listOf("VIEWER", "EXECUTOR"))
        .sign(Algorithm.HMAC256(secret))
}

println(generateToken())
