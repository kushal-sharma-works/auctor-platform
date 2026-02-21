package com.auctor.execution.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant

object TestTokenGenerator {
    fun generateToken(
        subject: String = "test-user",
        roles: List<String> = listOf("VIEWER", "EXECUTOR"),
        issuer: String = envOrDefault("EXECUTION_JWT_ISSUER", "auctor-auth"),
        audience: String = envOrDefault("EXECUTION_JWT_AUDIENCE", "execution-service"),
        secret: String = envOrDefault("EXECUTION_JWT_SECRET", "dev-secret-change-later-dev-secret-change-later"),
        expiresAt: Instant = Instant.now().plusSeconds(3600)
    ): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(subject)
            .withClaim("roles", roles)
            .withIssuedAt(Instant.now())
            .withExpiresAt(expiresAt)
            .sign(Algorithm.HMAC256(secret))
    }

    private fun envOrDefault(key: String, fallback: String): String {
        val value = System.getenv(key)
        return if (value.isNullOrBlank()) fallback else value
    }
}
