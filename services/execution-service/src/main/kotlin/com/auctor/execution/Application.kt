package com.auctor.execution

import com.auctor.execution.http.executionRoutes
import com.auctor.execution.grpc.DefinitionGrpcClient
import com.auctor.execution.security.configureAuth
import com.auctor.execution.service.ExecutionService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8082) {
        module()
    }.start(wait = true)
}

fun Application.module(
    definitionClient: DefinitionGrpcClient = DefinitionGrpcClient()
) {
    configureAuth()

    val executionService = ExecutionService(definitionClient)
    executionRoutes(executionService)
}
