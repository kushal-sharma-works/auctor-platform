package com.auctor.execution.http

import com.auctor.execution.service.ExecutionService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.executionRoutes(
    executionService: ExecutionService
) {
    routing {
        get("/health") {
            call.respondText("OK")
        }

        get("/execute/{id}") {
            val id = call.parameters["id"]
                ?: error("Missing id")

            val result = executionService.execute(id)

            call.respondText(
                "id=${result.id}, name=${result.name}, desc=${result.description}"
            )
        }
    }
}
