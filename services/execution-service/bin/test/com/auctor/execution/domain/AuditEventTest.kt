package com.auctor.execution.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuditEventTest {

    @Test
    fun `should create valid audit event`() {
        val event = AuditEvent(
            id = "audit-123",
            executionId = "exec-123",
            timestamp = Instant.now(),
            eventType = AuditEventType.EXECUTION_STARTED,
            fromState = null,
            toState = "pending",
            policyId = null,
            policyResult = null,
            explanation = "Execution started",
            actor = "user-001",
            correlationId = "corr-123"
        )

        assertNotNull(event)
        assertEquals("audit-123", event.id)
        assertEquals("exec-123", event.executionId)
        assertEquals(AuditEventType.EXECUTION_STARTED, event.eventType)
        assertEquals("pending", event.toState)
    }

    @Test
    fun `should create state transition event`() {
        val event = AuditEvent(
            id = "audit-456",
            executionId = "exec-123",
            timestamp = Instant.now(),
            eventType = AuditEventType.STATE_TRANSITION,
            fromState = "pending",
            toState = "approved",
            policyId = "policy-high-value",
            policyResult = true,
            explanation = "Policy allowed transition",
            actor = "system",
            correlationId = "corr-456"
        )

        assertEquals(AuditEventType.STATE_TRANSITION, event.eventType)
        assertEquals("pending", event.fromState)
        assertEquals("approved", event.toState)
        assertEquals(true, event.policyResult)
    }

    @Test
    fun `should throw on blank event id`() {
        assertThrows<IllegalArgumentException> {
            AuditEvent(
                id = "",
                executionId = "exec-123",
                timestamp = Instant.now(),
                eventType = AuditEventType.EXECUTION_STARTED,
                actor = "user-001",
                correlationId = "corr-123"
            )
        }
    }

    @Test
    fun `should throw on blank execution id`() {
        assertThrows<IllegalArgumentException> {
            AuditEvent(
                id = "audit-123",
                executionId = "",
                timestamp = Instant.now(),
                eventType = AuditEventType.EXECUTION_STARTED,
                actor = "user-001",
                correlationId = "corr-123"
            )
        }
    }

    @Test
    fun `should throw on blank actor`() {
        assertThrows<IllegalArgumentException> {
            AuditEvent(
                id = "audit-123",
                executionId = "exec-123",
                timestamp = Instant.now(),
                eventType = AuditEventType.EXECUTION_STARTED,
                actor = "",
                correlationId = "corr-123"
            )
        }
    }

    @Test
    fun `should throw on blank correlation id`() {
        assertThrows<IllegalArgumentException> {
            AuditEvent(
                id = "audit-123",
                executionId = "exec-123",
                timestamp = Instant.now(),
                eventType = AuditEventType.EXECUTION_STARTED,
                actor = "user-001",
                correlationId = ""
            )
        }
    }

    @Test
    fun `should be immutable data class`() {
        val event1 = AuditEvent(
            id = "audit-123",
            executionId = "exec-123",
            timestamp = Instant.parse("2024-01-01T00:00:00Z"),
            eventType = AuditEventType.EXECUTION_STARTED,
            actor = "user-001",
            correlationId = "corr-123"
        )

        val event2 = event1.copy(explanation = "Modified explanation")
        
        assertEquals("audit-123", event1.id)
        assertEquals("Modified explanation", event2.explanation)
        assertEquals(event1.id, event2.id)
    }
}
