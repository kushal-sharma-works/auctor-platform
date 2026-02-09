package com.auctor.execution.http

import com.auctor.definition.grpc.v1.GetWorkflowRequest
import com.auctor.execution.grpc.DefinitionGrpcClientFactory
import com.auctor.execution.security.AuthContext
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.executionRoutes() {

    routing {
        get("/execute/{id}") {
            val authContext = AuthContext(userId = "anonymous", roles = emptyList())

            val definitionClient =
                DefinitionGrpcClientFactory.create(authContext)

            val request = GetWorkflowRequest.newBuilder()
                .setId(call.parameters["id"]!!)
                .build()

            val response = definitionClient.getWorkflow(request)

            call.respondText(
                "id=${response.id}, name=${response.name}, version=${response.version}"
            )
        }
    }
}
