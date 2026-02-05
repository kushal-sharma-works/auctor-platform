package com.auctor.execution

import com.auctor.execution.grpc.DefinitionGrpcClient
import com.auctor.execution.routes.executeRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8082) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val definitionClient = DefinitionGrpcClient()
    executeRoutes(definitionClient)
}
