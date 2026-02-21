package com.auctor.execution.http

import com.auctor.execution.cache.CacheService
import com.auctor.execution.domain.ExecutionEngine
import com.auctor.execution.domain.ExecutionRepository
import com.auctor.execution.domain.AuditRepository
import com.auctor.execution.graphql.GraphQLProvider
import com.auctor.execution.observability.HealthService
import io.ktor.http.HttpStatusCode
import com.auctor.execution.security.toAuthContext
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
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
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("GraphQLRoutes")

fun Route.installGraphQlRoutes(
    cacheService: CacheService?,
    executionEngine: ExecutionEngine,
    executionRepository: ExecutionRepository,
    auditRepository: AuditRepository,
    healthService: HealthService
) {
    // Health check endpoint
    get("/health") {
        call.respond(healthService.liveness())
    }
    
    // Readiness check endpoint
    get("/ready") {
        val readiness = healthService.readiness()
        val status = if (readiness["status"] == "UP") HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        call.respond(status, readiness)
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
    
    authenticate("auth-jwt") {
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

                    val authHeader = call.request.headers["Authorization"]
                    val principal = call.principal<JWTPrincipal>()
                    if (principal == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "missing auth context", "data" to null))
                        return@post
                    }
                    val authContext = principal.toAuthContext(authHeader)
                    val context = buildMap<String, Any> {
                        authContext.rawToken?.let { put("authorization", it) }
                        put("authContext", authContext)
                    }

                    val result = gqlProvider.execute(query, variables, context)
                    val errors = result["errors"] as? List<*>
                    if (!errors.isNullOrEmpty()) {
                        val status = statusForGraphQlErrors(errors)
                        call.respond(status, anyToJsonElement(result))
                    } else {
                        call.respond(anyToJsonElement(result))
                    }
                } catch (t: Throwable) {
                    if (t is kotlinx.coroutines.CancellationException) {
                        throw t
                    }
                    logger.error("Failed to process GraphQL request", t)
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "error" to (t.message ?: "GraphQL request failed"),
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
                "message" to "Auth is enabled; include Authorization: Bearer <JWT_TOKEN>",
                "receivedAuthHeader" to authHeader,
                "format" to "Authorization: Bearer <JWT_TOKEN>"
            ))
        }
    }
}

private fun statusForGraphQlErrors(errors: List<*>): HttpStatusCode {
    val messages = errors.mapNotNull { (it as? Map<*, *>)?.get("message") as? String }

    if (messages.any { it.contains("FORBIDDEN", ignoreCase = true) }) {
        return HttpStatusCode.Forbidden
    }
    if (messages.any { it.contains("not found", ignoreCase = true) }) {
        return HttpStatusCode.NotFound
    }
    if (messages.any {
            it.contains("already in terminal state", ignoreCase = true) ||
            it.contains("No valid transitions", ignoreCase = true) ||
            it.contains("No allowed transitions", ignoreCase = true)
        }
    ) {
        return HttpStatusCode.Conflict
    }

    return HttpStatusCode.BadRequest
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
