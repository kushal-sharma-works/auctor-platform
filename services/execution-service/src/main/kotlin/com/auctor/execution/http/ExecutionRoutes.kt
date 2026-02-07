package com.auctor.execution.http

import com.auctor.execution.domain.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("ExecutionRoutes")

/**
 * Request to start a new execution.
 */
@Serializable
data class StartExecutionRequest(
    val workflowId: String,
    val workflowVersion: Int,
    val input: Map<String, String>
)

/**
 * Request to advance execution to next state.
 */
@Serializable
data class AdvanceExecutionRequest(
    val actor: String = "user"
)

/**
 * Error response.
 */
@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)

/**
 * Configure execution REST routes.
 */
fun Route.executionRoutes(executionEngine: ExecutionEngine) {
    
    // Health check
    get("/health") {
        call.respondText("OK", ContentType.Text.Plain, HttpStatusCode.OK)
    }
    
    // Readiness check
    get("/ready") {
        try {
            call.respondText("READY", ContentType.Text.Plain, HttpStatusCode.OK)
        } catch (e: Exception) {
            logger.error("Readiness check failed", e)
            call.respondText("NOT READY", ContentType.Text.Plain, HttpStatusCode.ServiceUnavailable)
        }
    }
    
    route("/api/v1/executions") {
        
        // GET /api/v1/executions - List executions (with pagination)
        get {
            try {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                
                val executions = executionEngine.listExecutions(limit, offset)
                call.respond(HttpStatusCode.OK, executions)
            } catch (e: Exception) {
                logger.error("Error listing executions", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("INTERNAL_ERROR", "Failed to list executions")
                )
            }
        }
        
        // GET /api/v1/executions/{id} - Get execution by ID
        get("/{id}") {
            try {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id parameter")
                val execution = executionEngine.getExecution(ExecutionId(id))
                call.respond(HttpStatusCode.OK, execution)
            } catch (e: IllegalArgumentException) {
                logger.error("Execution not found", e)
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("NOT_FOUND", e.message ?: "Execution not found")
                )
            } catch (e: Exception) {
                logger.error("Error fetching execution", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("INTERNAL_ERROR", "Failed to fetch execution")
                )
            }
        }
        
        // GET /api/v1/executions/{id}/audit - Get audit trail
        get("/{id}/audit") {
            try {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id parameter")
                val auditTrail = executionEngine.getAuditTrail(id)
                call.respond(HttpStatusCode.OK, auditTrail)
            } catch (e: Exception) {
                logger.error("Error fetching audit trail", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("INTERNAL_ERROR", "Failed to fetch audit trail")
                )
            }
        }
        
        // Authenticated routes
        authenticate("auth-jwt") {
            // POST /api/v1/executions - Start new execution (authenticated)
            post {
                try {
                    val request = call.receive<StartExecutionRequest>()
                    val authHeader = call.request.headers["Authorization"]
                    
                    val execution = executionEngine.startExecution(
                        workflowId = request.workflowId,
                        workflowVersion = request.workflowVersion,
                        input = request.input,
                        actor = "user", // In production, extract from JWT
                        authHeader = authHeader
                    )
                    
                    call.respond(HttpStatusCode.Created, execution)
                } catch (e: IllegalArgumentException) {
                    logger.error("Invalid request", e)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("BAD_REQUEST", e.message ?: "Invalid request")
                    )
                } catch (e: IllegalStateException) {
                    logger.error("Invalid state", e)
                    call.respond(
                        HttpStatusCode.Conflict,
                        ErrorResponse("CONFLICT", e.message ?: "Invalid state")
                    )
                } catch (e: Exception) {
                    logger.error("Internal error", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred")
                    )
                }
            }
            
            // POST /api/v1/executions/{id}/advance - Advance execution (authenticated)
            post("/{id}/advance") {
                try {
                    val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id parameter")
                    val request = call.receiveNullable<AdvanceExecutionRequest>()
                    val authHeader = call.request.headers["Authorization"]
                    
                    val stateTransitionRequest = StateTransitionRequest(
                        executionId = ExecutionId(id),
                        actor = request?.actor ?: "user",
                        correlationId = UUID.randomUUID().toString()
                    )
                    
                    val execution = executionEngine.advanceExecution(stateTransitionRequest, authHeader)
                    call.respond(HttpStatusCode.OK, execution)
                } catch (e: IllegalArgumentException) {
                    logger.error("Invalid request", e)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("BAD_REQUEST", e.message ?: "Invalid request")
                    )
                } catch (e: IllegalStateException) {
                    logger.error("Invalid state transition", e)
                    call.respond(
                        HttpStatusCode.Conflict,
                        ErrorResponse("CONFLICT", e.message ?: "Invalid state transition")
                    )
                } catch (e: Exception) {
                    logger.error("Error advancing execution", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("INTERNAL_ERROR", "Failed to advance execution")
                    )
                }
            }
        }
    }
}