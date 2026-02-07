package com.auctor.execution.domain

import com.auctor.execution.grpc.DefinitionGrpcClient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * Core domain service for workflow execution with state machine logic.
 * 
 * Responsibilities:
 * - Start new executions
 * - Advance executions through state transitions
 * - Evaluate policies via gRPC
 * - Maintain immutable audit trail
 * - Use structured concurrency for parallel operations
 */
class ExecutionEngine(
    private val executionRepository: ExecutionRepository,
    private val auditRepository: AuditRepository,
    private val grpcClient: DefinitionGrpcClient
) {
    private val logger = LoggerFactory.getLogger(ExecutionEngine::class.java)
    
    /**
     * Start a new workflow execution.
     * Creates execution in initial state and records EXECUTION_STARTED audit event.
     */
    suspend fun startExecution(
        workflowId: String,
        workflowVersion: Int,
        input: Map<String, String>,
        actor: String,
        authHeader: String? = null
    ): Execution = coroutineScope {
        logger.info("Starting execution for workflow $workflowId v$workflowVersion")
        
        // Fetch workflow definition via gRPC with timeout
        val workflow = withTimeout(5000) {
            grpcClient.getWorkflow(workflowId, workflowVersion, authHeader)
        } ?: throw IllegalArgumentException("Workflow $workflowId v$workflowVersion not found")
        
        // Validate workflow is published
        if (workflow.status != "PUBLISHED") {
            throw IllegalStateException("Workflow ${workflow.id} v${workflow.version} is not PUBLISHED (status: ${workflow.status})")
        }
        
        // Create execution
        val executionId = ExecutionId("exec-${UUID.randomUUID()}")
        val now = Instant.now()
        
        val execution = Execution(
            id = executionId,
            workflowId = workflow.id,
            workflowVersion = workflow.version,
            currentState = workflow.initialState,
            status = ExecutionStatus.Running,
            input = input,
            createdAt = now,
            updatedAt = now
        )
        
        // Save execution
        val savedExecution = executionRepository.save(execution)
        
        // Record audit event
        val auditEvent = AuditEvent(
            id = "audit-${UUID.randomUUID()}",
            executionId = savedExecution.id.value,
            timestamp = now,
            eventType = AuditEventType.EXECUTION_STARTED,
            fromState = null,
            toState = workflow.initialState,
            policyId = null,
            policyResult = null,
            explanation = "Execution started for workflow ${workflow.name} v${workflow.version}",
            actor = actor,
            correlationId = UUID.randomUUID().toString()
        )
        auditRepository.append(auditEvent)
        
        logger.info("Execution ${savedExecution.id} started in state ${workflow.initialState}")
        savedExecution
    }
    
    /**
     * Advance execution to next state.
     * - Loads execution and workflow
     * - Finds valid transitions from current state
     * - Evaluates policy if transition has policyRef
     * - Updates state if allowed, or stays in current state if denied
     * - Records audit events (STATE_TRANSITION, POLICY_EVALUATED)
     * - Marks execution COMPLETED if new state has no outgoing transitions
     */
    suspend fun advanceExecution(request: StateTransitionRequest, authHeader: String? = null): Execution = coroutineScope {
        logger.info("Advancing execution ${request.executionId}")
        
        // Load execution
        val execution = executionRepository.findById(request.executionId)
            ?: throw IllegalArgumentException("Execution ${request.executionId} not found")
        
        // Check if execution is already terminal
        if (execution.status is ExecutionStatus.Completed || execution.status is ExecutionStatus.Failed) {
            throw IllegalStateException("Execution ${execution.id} is already in terminal state: ${execution.status}")
        }
        
        // Load workflow via gRPC with timeout
        val workflow = withTimeout(5000) {
            grpcClient.getWorkflow(execution.workflowId, execution.workflowVersion, authHeader)
        } ?: throw IllegalStateException("Workflow ${execution.workflowId} v${execution.workflowVersion} not found")
        
        // Find valid transitions from current state
        val validTransitions = workflow.transitions.filter { it.fromState == execution.currentState }
        if (validTransitions.isEmpty()) {
            throw IllegalStateException("No valid transitions from state ${execution.currentState}")
        }
        
        // For simplicity, take first valid transition (in production, this could be user-selected)
        val transition = validTransitions.first()
        val newState = transition.toState
        
        // Evaluate policy if transition has policyRef
        val policyResult = if (!transition.policyRef.isNullOrBlank()) {
            val result = withTimeout(5000) {
                grpcClient.evaluatePolicy(
                    policyId = transition.policyRef,
                    version = execution.workflowVersion,
                    context = execution.input,
                    authHeader = authHeader
                )
            }
            
            // Record policy evaluation audit event
            val policyAuditEvent = AuditEvent(
                id = "audit-${UUID.randomUUID()}",
                executionId = execution.id.value,
                timestamp = Instant.now(),
                eventType = AuditEventType.POLICY_EVALUATED,
                fromState = execution.currentState,
                toState = newState,
                policyId = transition.policyRef,
                policyResult = result.allowed,
                explanation = result.explanation,
                actor = request.actor,
                correlationId = request.correlationId
            )
            auditRepository.append(policyAuditEvent)
            
            result
        } else {
            null
        }
        
        // If policy denies, stay in current state
        if (policyResult != null && !policyResult.allowed) {
            logger.warn("Policy ${transition.policyRef} denied transition from ${execution.currentState} to $newState")
            return@coroutineScope execution
        }
        
        // Update execution state
        val now = Instant.now()
        val updatedExecution = execution.copy(
            currentState = newState,
            updatedAt = now
        )
        
        // Record state transition audit event
        val transitionAuditEvent = AuditEvent(
            id = "audit-${UUID.randomUUID()}",
            executionId = execution.id.value,
            timestamp = now,
            eventType = AuditEventType.STATE_TRANSITION,
            fromState = execution.currentState,
            toState = newState,
            policyId = transition.policyRef,
            policyResult = policyResult?.allowed,
            explanation = "Transitioned from ${execution.currentState} to $newState",
            actor = request.actor,
            correlationId = request.correlationId
        )
        auditRepository.append(transitionAuditEvent)
        
        // Check if new state has no outgoing transitions (terminal state)
        val outgoingTransitions = workflow.transitions.filter { it.fromState == newState }
        val finalExecution = if (outgoingTransitions.isEmpty()) {
            logger.info("Execution ${execution.id} reached terminal state $newState")
            val completedExecution = updatedExecution.copy(status = ExecutionStatus.Completed)
            
            // Record completion audit event
            val completionAuditEvent = AuditEvent(
                id = "audit-${UUID.randomUUID()}",
                executionId = execution.id.value,
                timestamp = now,
                eventType = AuditEventType.EXECUTION_COMPLETED,
                fromState = newState,
                toState = null,
                policyId = null,
                policyResult = null,
                explanation = "Execution completed in state $newState",
                actor = request.actor,
                correlationId = request.correlationId
            )
            auditRepository.append(completionAuditEvent)
            
            completedExecution
        } else {
            updatedExecution
        }
        
        // Save and return
        executionRepository.update(finalExecution)
    }
    
    /**
     * Get execution by ID.
     */
    suspend fun getExecution(id: ExecutionId): Execution {
        return executionRepository.findById(id)
            ?: throw IllegalArgumentException("Execution $id not found")
    }
    
    /**
     * Get audit trail for an execution.
     */
    suspend fun getAuditTrail(executionId: String): List<AuditEvent> {
        return auditRepository.findByExecutionId(executionId)
    }
    
    /**
     * List executions with pagination.
     */
    suspend fun listExecutions(limit: Int = 20, offset: Int = 0): List<Execution> {
        return executionRepository.findAll(limit, offset)
    }
}
