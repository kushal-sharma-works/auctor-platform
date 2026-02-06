package com.auctor.execution.http

import com.auctor.definition.grpc.v1.GetDefinitionRequest
import com.auctor.execution.grpc.DefinitionGrpcClientFactory
import com.auctor.execution.security.toAuthContext
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.executionRoutes() {

    routing {
        authenticate("auth-jwt") {

            get("/execute/{id}") {
                val principal = call.principal<JWTPrincipal>()
                    ?: error("JWT principal missing")

                val authContext = principal.toAuthContext()

                val definitionClient =
                    DefinitionGrpcClientFactory.create(authContext)

                val request = GetDefinitionRequest.newBuilder()
                    .setId(call.parameters["id"]!!)
                    .build()

                val response = definitionClient.getDefinition(request)

                call.respondText(
                    "id=${response.id}, name=${response.name}, desc=${response.description}"
                )
            }
        }
    }
}
