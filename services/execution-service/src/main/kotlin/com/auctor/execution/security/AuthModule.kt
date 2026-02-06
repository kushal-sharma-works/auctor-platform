package com.auctor.execution.security

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuth() {

    install(Authentication) {

        jwt("auth-jwt") {
            verifier(JwtConfig.verifier)

            validate { credential ->
                val roles =
                    credential.payload
                        .getClaim("roles")
                        .asList(String::class.java)

                if (roles.contains("EXECUTOR")) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
