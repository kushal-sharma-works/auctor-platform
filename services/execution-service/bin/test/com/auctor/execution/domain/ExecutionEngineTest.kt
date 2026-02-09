package com.auctor.execution.domain

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ExecutionEngine domain logic.
 *
 * Note: These tests validate domain model behavior and business logic only.
 */
class ExecutionEngineTest {

    @Test
    fun `Execution domain model should be immutable`() {
        val execution = Execution(
            id = ExecutionId("test-001"),
            workflowId = "wf-001",
            workflowVersion = 1,
            currentState = "pending",
            status = ExecutionStatus.Running,
            input = mapOf("key" to "value"),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Verify execution can be copied with modifications
        val updated = execution.copy(currentState = "approved", status = ExecutionStatus.Completed)
        
        assertEquals("pending", execution.currentState)
        assertEquals("approved", updated.currentState)
        assertTrue(execution.status is ExecutionStatus.Running)
        assertTrue(updated.status is ExecutionStatus.Completed)
    }

    @Test
    fun `ExecutionStatus should handle all states`() {
        val running = ExecutionStatus.Running
        val completed = ExecutionStatus.Completed
        val suspended = ExecutionStatus.Suspended
        val failed = ExecutionStatus.Failed("Error message")

        assertTrue(running is ExecutionStatus.Running)
        assertTrue(completed is ExecutionStatus.Completed)
        assertTrue(suspended is ExecutionStatus.Suspended)
        assertTrue(failed is ExecutionStatus.Failed)
        assertEquals("Error message", (failed as ExecutionStatus.Failed).reason)
    }

    @Test
    fun `AuditEvent should capture state transitions`() {
        val event = AuditEvent(
            id = "audit-001",
            executionId = "exec-001",
            timestamp = Instant.now(),
            eventType = AuditEventType.STATE_TRANSITION,
            fromState = "pending",
            toState = "approved",
            policyId = "policy-001",
            policyResult = true,
            explanation = "Transition allowed",
            actor = "user-001",
            correlationId = "corr-001"
        )

        assertEquals(AuditEventType.STATE_TRANSITION, event.eventType)
        assertEquals("pending", event.fromState)
        assertEquals("approved", event.toState)
        assertEquals(true, event.policyResult)
    }

    @Test
    fun `StateTransitionRequest should validate`() {
        val request = StateTransitionRequest(
            executionId = ExecutionId("exec-001"),
            actor = "user-001",
            correlationId = "corr-001"
        )

        assertEquals("exec-001", request.executionId.value)
        assertEquals("user-001", request.actor)
    }

    @Test
    fun `PolicyEvaluationResult should contain decision`() {
        val resultAllowed = PolicyEvaluationResult(
            policyId = "policy-001",
            allowed = true,
            explanation = "Request approved"
        )

        val resultDenied = PolicyEvaluationResult(
            policyId = "policy-002",
            allowed = false,
            explanation = "Request denied"
        )

        assertTrue(resultAllowed.allowed)
        assertTrue(!resultDenied.allowed)
    }
}
