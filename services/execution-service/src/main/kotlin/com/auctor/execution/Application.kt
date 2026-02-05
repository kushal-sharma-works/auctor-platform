package com.auctor.execution

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*

fun main() {
    embeddedServer(Netty, port = 8082) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    routing {
        get("/health") {
            call.respondText("OK")
        }

        get("/execute/{id}") {
            val id = call.parameters["id"]!!
            call.respondText("placeholder") // gRPC will be wired next
        }
    }
}
