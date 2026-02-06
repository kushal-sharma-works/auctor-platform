package com.auctor.execution.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm

object JwtConfig {

    private const val issuer = "auctor-auth"
    private const val audience = "execution-service"
    private const val secret = "dev-secret-change-later"

    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier =
        JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
}
