package com.auctor.execution.http

import com.auctor.execution.cache.CacheService
import com.auctor.execution.domain.ExecutionEngine
import com.auctor.execution.domain.ExecutionRepository
import com.auctor.execution.domain.AuditRepository
import com.auctor.execution.grpc.DefinitionGrpcClient
import com.auctor.execution.graphql.GraphQLProvider
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun Route.installGraphQlRoutes(
    cacheService: CacheService?,
    executionEngine: ExecutionEngine,
    executionRepository: ExecutionRepository,
    auditRepository: AuditRepository
) {
    // Health check endpoint
    get("/health") {
        call.respondText("OK", ContentType.Text.Plain, HttpStatusCode.OK)
    }
    
    // Readiness check endpoint
    get("/ready") {
        call.respondText("READY", ContentType.Text.Plain, HttpStatusCode.OK)
    }
    
    // If cache service is not available, skip GraphQL setup
    if (cacheService == null) {
        return
    }
    
    val gqlProvider = GraphQLProvider(
        cacheService = cacheService,
        executionEngine = executionEngine,
        executionRepository = executionRepository,
        auditRepository = auditRepository
    )

    // Explicit OPTIONS for CORS preflight
    options("/graphql") {
        call.response.headers.append("Access-Control-Allow-Origin", "*")
        call.response.headers.append("Access-Control-Allow-Methods", "POST, OPTIONS")
        call.response.headers.append("Access-Control-Allow-Headers", "Content-Type, Authorization")
        call.respond(HttpStatusCode.OK)
    }
    
    route("/graphql") {
        post {
            // Add CORS headers to response
            call.response.headers.append("Access-Control-Allow-Origin", "*")
            
            try {
                // Read raw request body as string
                val body = call.receiveText()
                val json = Json.parseToJsonElement(body).jsonObject
                
                val query = json["query"]?.jsonPrimitive?.content
                val variables = json["variables"]?.let { element ->
                    if (element is JsonObject) {
                        element.toMap().mapValues { (_, v) -> jsonElementToAny(v) }
                    } else {
                        null
                    }
                }
                
                if (query.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "query is required", "data" to null))
                    return@post
                }

                // Authorization header is optional; auth is disabled in dev
                val authHeader = call.request.header("Authorization")
                val context: Map<String, Any>? = authHeader?.let { mapOf("authorization" to it) }

                val result = gqlProvider.execute(query, variables, context)
                call.respond(anyToJsonElement(result))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "data" to null
                ))
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
                "message" to "Auth is disabled; Authorization header is optional and ignored by the server",
                "receivedAuthHeader" to authHeader,
                "format" to "Authorization: Bearer <JWT_TOKEN> (optional)"
            ))
        }
    }
}

private fun jsonElementToAny(element: JsonElement): Any? {
    return when (element) {
        is JsonNull -> null
        is JsonObject -> element.toMap().mapValues { (_, v) -> jsonElementToAny(v) }
        is JsonArray -> element.map { jsonElementToAny(it) }
        is JsonPrimitive -> {
            if (element.isString) {
                element.content
            } else {
                val raw = element.content
                raw.toBooleanStrictOrNull()
                    ?: raw.toLongOrNull()
                    ?: raw.toDoubleOrNull()
                    ?: raw
            }
        }
        else -> element.toString()
    }
}

private fun anyToJsonElement(value: Any?): JsonElement {
    return when (value) {
        null -> JsonNull
        is JsonElement -> value
        is Map<*, *> -> {
            val entries = value.entries.associate { (k, v) ->
                (k?.toString() ?: "") to anyToJsonElement(v)
            }
            JsonObject(entries)
        }
        is Iterable<*> -> JsonArray(value.map { anyToJsonElement(it) })
        is Array<*> -> JsonArray(value.map { anyToJsonElement(it) })
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        else -> JsonPrimitive(value.toString())
    }
}
