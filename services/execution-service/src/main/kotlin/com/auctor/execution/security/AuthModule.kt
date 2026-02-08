package com.auctor.execution.security

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuth() {

    val config = environment.config
    val isDev = developmentMode
    
    install(Authentication) {
        
        jwt("auth-jwt") {
            val verifier = JwtConfig.buildVerifier(config, isDev)
            verifier(verifier)
            
            validate { credential ->
                val roles = credential.payload
                    .getClaim("roles")
                    .asList(String::class.java)
                    ?: emptyList()
                
                if (roles.contains("EXECUTOR")) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}