package com.auctor.execution.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.config.*

object JwtConfig {

    private const val defaultSecret = "dev-secret-change-later"

    data class Settings(
        val issuer: String,
        val audience: String,
        val secret: String
    )

    fun buildVerifier(config: ApplicationConfig, developmentMode: Boolean): JWTVerifier {
        val settings = loadSettings(config)
        if (!developmentMode && settings.secret == defaultSecret) {
            throw IllegalStateException("JWT secret must be configured for non-dev environments")
        }

        val algorithm = Algorithm.HMAC256(settings.secret)
        return JWT.require(algorithm)
            .withIssuer(settings.issuer)
            .withAudience(settings.audience)
            .build()
    }

    private fun loadSettings(config: ApplicationConfig): Settings {
        val issuer = System.getenv("EXECUTION_JWT_ISSUER")
            ?: config.propertyOrNull("ktor.jwt.issuer")?.getString()
            ?: "auctor-auth"
        val audience = System.getenv("EXECUTION_JWT_AUDIENCE")
            ?: config.propertyOrNull("ktor.jwt.audience")?.getString()
            ?: "execution-service"
        val secret = System.getenv("EXECUTION_JWT_SECRET")
            ?: config.propertyOrNull("ktor.jwt.secret")?.getString()
            ?: defaultSecret

        return Settings(issuer = issuer, audience = audience, secret = secret)
    }
}
