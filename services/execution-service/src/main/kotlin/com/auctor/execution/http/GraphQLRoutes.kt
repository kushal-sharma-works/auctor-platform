package com.auctor.execution.http

import com.auctor.execution.cache.CacheService
import com.auctor.execution.grpc.DefinitionGrpcClient
import com.auctor.execution.graphql.GraphQLProvider
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json

fun Route.installGraphQlRoutes(cacheService: CacheService?) {
    // If cache service is not available, skip GraphQL setup
    if (cacheService == null) {
        return
    }
    
    val gqlProvider = GraphQLProvider(cacheService)

    // Explicit OPTIONS for CORS preflight
    options("/graphql") {
        call.response.headers.append("Access-Control-Allow-Origin", "*")
        call.response.headers.append("Access-Control-Allow-Methods", "POST, OPTIONS")
        call.response.headers.append("Access-Control-Allow-Headers", "Content-Type, Authorization")
        call.respond(HttpStatusCode.OK)
    }
    
    route("/graphql") {
        authenticate("auth-jwt") {
            post {
                // Add CORS headers to response
                call.response.headers.append("Access-Control-Allow-Origin", "*")
                
                try {
                    // Read raw request body as string
                    val body = call.receiveText()
                    val json = Json.parseToJsonElement(body).jsonObject
                    
                    val query = json["query"]?.jsonPrimitive?.content
                    val variables = json["variables"]?.jsonObject?.let { 
                        it.toMap().mapValues { (_, v) -> v.jsonPrimitive.content }
                    }
                    
                    if (query.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "query is required", "data" to null))
                        return@post
                    }

                    // Extract Authorization header
                    val authHeader = call.request.header("Authorization")
                    val context: Map<String, Any>? = authHeader?.let { mapOf("authorization" to it) }

                    val result = gqlProvider.execute(query, variables, context)
                    call.respond(result)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "error" to (e.message ?: "Unknown error"),
                        "data" to null
                    ))
                }
            }
        }
    }

    // Health check (no auth required)
    route("/graphql-health") {
        get {
            call.respond(mapOf("status" to "ok"))
        }
    }
    
    // Debug endpoint to verify token format (no auth required)
    route("/graphql-debug/token-info") {
        get {
            val authHeader = call.request.header("Authorization")
            call.respond(mapOf(
                "message" to "To use GraphQL, send a valid JWT token in Authorization header as 'Bearer <token>'",
                "receivedAuthHeader" to authHeader,
                "format" to "Authorization: Bearer <JWT_TOKEN>"
            ))
        }
    }
}
