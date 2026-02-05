package com.auctor.execution.routes

import com.auctor.execution.grpc.DefinitionGrpcClient
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.executeRoutes(definitionClient: DefinitionGrpcClient) {
    routing {
        get("/execute/{id}") {
            val id = call.parameters["id"]!!
            val result = definitionClient.getDefinition(id)
            call.respondText(result)
        }
    }
}
