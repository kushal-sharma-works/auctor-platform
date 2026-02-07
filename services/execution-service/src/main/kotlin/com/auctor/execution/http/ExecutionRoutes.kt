package com.auctor.execution.http

import com.auctor.execution.domain.*
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
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

@Serializable
data class ExecutionStatusResponse(
    val state: String,
    val reason: String? = null
)

@Serializable
data class ExecutionResponse(
    val id: String,
    val workflowId: String,
    val workflowVersion: Int,
    val currentState: String,
    val status: ExecutionStatusResponse,
    val input: Map<String, String>,
    val createdAt: String,
    val updatedAt: String
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
        call.respondText("READY", ContentType.Text.Plain, HttpStatusCode.OK)
    }
    
    route("/api/v1/executions") {
        
        // GET /api/v1/executions - List executions (with pagination)
        get {
            try {
                val limitParam = call.request.queryParameters["limit"]
                val offsetParam = call.request.queryParameters["offset"]
                val limit = limitParam?.toIntOrNull()
                val offset = offsetParam?.toIntOrNull()

                if (limitParam != null && limit == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Invalid limit"))
                    return@get
                }
                if (offsetParam != null && offset == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Invalid offset"))
                    return@get
                }

                val safeLimit = limit ?: 20
                val safeOffset = offset ?: 0

                if (safeLimit !in 1..100 || safeOffset < 0) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "limit must be 1..100 and offset >= 0"))
                    return@get
                }

                val executions = executionEngine.listExecutions(safeLimit, safeOffset)
                call.respond(HttpStatusCode.OK, executions.map { it.toResponse() })
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
                call.respond(HttpStatusCode.OK, execution.toResponse())
            } catch (e: ExecutionNotFoundException) {
                logger.info("Execution not found: ${e.message}")
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
                    val actor = actorFromPrincipal(call)
                    
                    val execution = executionEngine.startExecution(
                        workflowId = request.workflowId,
                        workflowVersion = request.workflowVersion,
                        input = request.input,
                        actor = actor,
                        authHeader = authHeader
                    )
                    
                    call.respond(HttpStatusCode.Created, execution.toResponse())
                } catch (e: IllegalArgumentException) {
                    logger.info("Invalid request: ${e.message}")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("BAD_REQUEST", e.message ?: "Invalid request")
                    )
                } catch (e: IllegalStateException) {
                    logger.info("Invalid state: ${e.message}")
                    call.respond(
                        HttpStatusCode.Conflict,
                        ErrorResponse("CONFLICT", e.message ?: "Invalid state")
                    )
                } catch (e: StatusRuntimeException) {
                    handleGrpcError(call, e)
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
                    val authHeader = call.request.headers["Authorization"]
                    val actor = actorFromPrincipal(call)
                    
                    val stateTransitionRequest = StateTransitionRequest(
                        executionId = ExecutionId(id),
                        actor = actor,
                        correlationId = UUID.randomUUID().toString()
                    )
                    
                    val execution = executionEngine.advanceExecution(stateTransitionRequest, authHeader)
                    call.respond(HttpStatusCode.OK, execution.toResponse())
                } catch (e: IllegalArgumentException) {
                    logger.info("Invalid request: ${e.message}")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("BAD_REQUEST", e.message ?: "Invalid request")
                    )
                } catch (e: ExecutionNotFoundException) {
                    logger.info("Execution not found: ${e.message}")
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("NOT_FOUND", e.message ?: "Execution not found")
                    )
                } catch (e: IllegalStateException) {
                    logger.info("Invalid state transition: ${e.message}")
                    call.respond(
                        HttpStatusCode.Conflict,
                        ErrorResponse("CONFLICT", e.message ?: "Invalid state transition")
                    )
                } catch (e: StatusRuntimeException) {
                    handleGrpcError(call, e)
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

private fun Execution.toResponse(): ExecutionResponse {
    return ExecutionResponse(
        id = id.value,
        workflowId = workflowId,
        workflowVersion = workflowVersion,
        currentState = currentState,
        status = status.toResponse(),
        input = input,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString()
    )
}

private fun ExecutionStatus.toResponse(): ExecutionStatusResponse {
    return when (this) {
        is ExecutionStatus.Running -> ExecutionStatusResponse("RUNNING")
        is ExecutionStatus.Completed -> ExecutionStatusResponse("COMPLETED")
        is ExecutionStatus.Suspended -> ExecutionStatusResponse("SUSPENDED")
        is ExecutionStatus.Failed -> ExecutionStatusResponse("FAILED", reason)
    }
}

private fun actorFromPrincipal(call: ApplicationCall): String {
    val principal = call.principal<JWTPrincipal>()
    val subject = principal?.payload?.subject ?: principal?.payload?.getClaim("sub")?.asString()
    return subject ?: "unknown"
}

private suspend fun handleGrpcError(call: ApplicationCall, e: StatusRuntimeException) {
    when (e.status.code) {
        Status.Code.NOT_FOUND -> call.respond(
            HttpStatusCode.NotFound,
            ErrorResponse("NOT_FOUND", e.status.description ?: "Not found")
        )
        Status.Code.INVALID_ARGUMENT -> call.respond(
            HttpStatusCode.BadRequest,
            ErrorResponse("BAD_REQUEST", e.status.description ?: "Invalid request")
        )
        else -> call.respond(
            HttpStatusCode.BadGateway,
            ErrorResponse("UPSTREAM_ERROR", e.status.description ?: "Upstream service error")
        )
    }
}