package com.auctor.execution

import com.auctor.execution.cache.CacheService
import com.auctor.execution.grpc.DefinitionGrpcClient
import com.auctor.execution.http.installGraphQlRoutes
import com.auctor.execution.http.executionRoutes
import com.auctor.execution.observability.initTracing
import com.auctor.execution.security.configureAuth
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.response.*

fun main() {
    embeddedServer(Netty, port = 8082) {
        module()
    }.start(wait = true)
}

fun Application.module(
    grpcClient: DefinitionGrpcClient? = null,
    cacheService: CacheService? = null
) {
    // Install content negotiation for JSON
    install(ContentNegotiation) {
        json()
    }

    // create grpc client (shared) - use provided one for testing or create default
    val actualGrpcClient = grpcClient ?: DefinitionGrpcClient(targetHost = "localhost", targetPort = 9090, callDeadlineMs = 2000)

    // create cache service (shared) - use provided one for testing or create default
    val actualCacheService = cacheService ?: CacheService(actualGrpcClient, redisUrl = environment.config.propertyOrNull("ktor.redis.url")?.getString() ?: "redis://localhost:6379")

    // Initialize OpenTelemetry tracing
    val openTelemetry = initTracing()

    // Note: OpenTelemetry Ktor plugin not available for Ktor 3.0 yet
    // Manual instrumentation can be added if needed

    // Configure authentication
    configureAuth()

    // Configure execution routes
    executionRoutes()

    // Now install routes:
    routing {
        // health and simple endpoints you already have
        get("/health") {
            call.respondText("OK")
        }

        // GraphQL endpoints
        installGraphQlRoutes(actualCacheService)
    }

    // shutdown hook to clean resources - only if we created them
    environment.monitor.subscribe(ApplicationStopped) {
        if (cacheService == null) actualCacheService.close()
        if (grpcClient == null) actualGrpcClient.close()
    }
}
