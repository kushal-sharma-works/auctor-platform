package com.auctor.execution.infra.db

import com.auctor.execution.domain.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration test for ExposedExecutionRepository and ExposedAuditRepository.
 * Uses H2 in-memory database to test actual database interactions.
 */
class ExposedRepositoryIT {

    private lateinit var executionRepository: ExposedExecutionRepository
    private lateinit var auditRepository: ExposedAuditRepository
    private lateinit var database: Database

    @BeforeEach
    fun setup() {
        // Use H2 in-memory database with PostgreSQL compatibility mode
        database = Database.connect(
            "jdbc:h2:mem:test_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        // Create tables
        transaction(database) {
            SchemaUtils.create(Executions, AuditEvents)
        }

        executionRepository = ExposedExecutionRepository()
        auditRepository = ExposedAuditRepository()
    }

    @Test
    fun `should save and retrieve execution by id`() {
        runBlocking {
            // Arrange
            val execution = Execution(
                id = ExecutionId("exec-123"),
                workflowId = "wf-456",
                workflowVersion = 1,
                currentState = "STARTED",
                status = ExecutionStatus.Running,
                input = mapOf("key1" to "value1", "key2" to "value2"),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            // Act
            val saved = executionRepository.save(execution)
            val retrieved = executionRepository.findById(ExecutionId("exec-123"))

            // Assert
            assertNotNull(retrieved)
            assertEquals(execution.id, retrieved.id)
            assertEquals(execution.workflowId, retrieved.workflowId)
            assertEquals(execution.workflowVersion, retrieved.workflowVersion)
            assertEquals(execution.currentState, retrieved.currentState)
            assertEquals(execution.input, retrieved.input)
            assertTrue(retrieved.status is ExecutionStatus.Running)
        }
    }

    @Test
    fun `should return null when execution not found`() {
        runBlocking {
            // Act
            val result = executionRepository.findById(ExecutionId("non-existent"))

            // Assert
            assertNull(result)
        }
    }

    @Test
    fun `should update existing execution`() {
        runBlocking {
            // Arrange
            val execution = Execution(
                id = ExecutionId("exec-789"),
                workflowId = "wf-111",
                workflowVersion = 1,
                currentState = "STARTED",
                status = ExecutionStatus.Running,
                input = mapOf("key" to "initial"),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            executionRepository.save(execution)

            // Act
            val updated = execution.copy(
                currentState = "STEP1",
                status = ExecutionStatus.Completed,
                input = mapOf("key" to "updated"),
                updatedAt = Instant.now()
            )
            executionRepository.update(updated)

            val retrieved = executionRepository.findById(ExecutionId("exec-789"))

            // Assert
            assertNotNull(retrieved)
            assertEquals("STEP1", retrieved.currentState)
            assertTrue(retrieved.status is ExecutionStatus.Completed)
            assertEquals(mapOf("key" to "updated"), retrieved.input)
        }
    }

    @Test
    fun `should save execution with audit events atomically`() {
        runBlocking {
            // Arrange
            val execution = Execution(
                id = ExecutionId("exec-with-audit"),
                workflowId = "wf-222",
                workflowVersion = 1,
                currentState = "STARTED",
                status = ExecutionStatus.Running,
                input = mapOf(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            val auditEvent = AuditEvent(
                id = "audit-1",
                executionId = "exec-with-audit",
                timestamp = Instant.now(),
                eventType = AuditEventType.EXECUTION_STARTED,
                fromState = null,
                toState = "STARTED",
                policyId = null,
                policyResult = null,
                explanation = "Execution started",
                actor = "system",
                correlationId = "corr-123"
            )

            // Act
            executionRepository.saveWithAudit(execution, listOf(auditEvent))

            val retrievedExecution = executionRepository.findById(ExecutionId("exec-with-audit"))
            val retrievedAudit = auditRepository.findByExecutionId("exec-with-audit")

            // Assert
            assertNotNull(retrievedExecution)
            assertEquals(1, retrievedAudit.size)
            assertEquals("audit-1", retrievedAudit[0].id)
            assertEquals(AuditEventType.EXECUTION_STARTED, retrievedAudit[0].eventType)
        }
    }

    @Test
    fun `should update execution with audit events atomically`() {
        runBlocking {
            // Arrange
            val execution = Execution(
                id = ExecutionId("exec-update-audit"),
                workflowId = "wf-333",
                workflowVersion = 1,
                currentState = "STARTED",
                status = ExecutionStatus.Running,
                input = mapOf(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            executionRepository.save(execution)

            // Act - Update with audit
            val updated = execution.copy(
                currentState = "STEP1",
                updatedAt = Instant.now()
            )
            val auditEvent = AuditEvent(
                id = "audit-2",
                executionId = "exec-update-audit",
                timestamp = Instant.now(),
                eventType = AuditEventType.STATE_TRANSITION,
                fromState = "STARTED",
                toState = "STEP1",
                policyId = null,
                policyResult = null,
                explanation = "Transitioned to STEP1",
                actor = "user@example.com",
                correlationId = "corr-456"
            )
            executionRepository.updateWithAudit(updated, listOf(auditEvent))

            val retrievedAudit = auditRepository.findByExecutionId("exec-update-audit")

            // Assert
            assertEquals(1, retrievedAudit.size)
            assertEquals(AuditEventType.STATE_TRANSITION, retrievedAudit[0].eventType)
            assertEquals("STARTED", retrievedAudit[0].fromState)
            assertEquals("STEP1", retrievedAudit[0].toState)
        }
    }

    @Test
    fun `should find all executions with pagination`() {
        runBlocking {
            // Arrange - Create 5 executions
            val now = Instant.now()
            for (i in 1..5) {
                val execution = Execution(
                    id = ExecutionId("exec-$i"),
                    workflowId = "wf-444",
                    workflowVersion = 1,
                    currentState = "STARTED",
                    status = ExecutionStatus.Running,
                    input = mapOf(),
                    createdAt = now.plusSeconds(i.toLong()),
                    updatedAt = now.plusSeconds(i.toLong())
                )
                executionRepository.save(execution)
            }

            // Act - Get first 3 executions
            val page1 = executionRepository.findAll(limit = 3, offset = 0)
            val page2 = executionRepository.findAll(limit = 3, offset = 3)

            // Assert
            assertEquals(3, page1.size)
            assertEquals(2, page2.size)
            
            // Should be ordered by createdAt DESC (newest first)
            assertEquals("exec-5", page1[0].id.value)
            assertEquals("exec-4", page1[1].id.value)
            assertEquals("exec-3", page1[2].id.value)
        }
    }

    @Test
    fun `should append and retrieve audit events by execution id`() {
        runBlocking {
            // Arrange
            val event1 = AuditEvent(
                id = "audit-10",
                executionId = "exec-audit-test",
                timestamp = Instant.now(),
                eventType = AuditEventType.EXECUTION_STARTED,
                fromState = null,
                toState = "STARTED",
                policyId = null,
                policyResult = null,
                explanation = "Started",
                actor = "system",
                correlationId = "corr-789"
            )

            val event2 = AuditEvent(
                id = "audit-11",
                executionId = "exec-audit-test",
                timestamp = Instant.now().plusSeconds(1),
                eventType = AuditEventType.POLICY_EVALUATED,
                fromState = null,
                toState = null,
                policyId = "policy-1",
                policyResult = true,
                explanation = "Policy passed",
                actor = "system",
                correlationId = "corr-789"
            )

            // Act
            auditRepository.append(event1)
            auditRepository.append(event2)

            val retrieved = auditRepository.findByExecutionId("exec-audit-test")

            // Assert
            assertEquals(2, retrieved.size)
            
            // Should be ordered by timestamp ASC
            assertEquals("audit-10", retrieved[0].id)
            assertEquals("audit-11", retrieved[1].id)
            assertEquals(AuditEventType.EXECUTION_STARTED, retrieved[0].eventType)
            assertEquals(AuditEventType.POLICY_EVALUATED, retrieved[1].eventType)
        }
    }

    @Test
    fun `should handle different execution status types correctly`() {
        runBlocking {
            // Arrange & Act - Save executions with different statuses
            val runningExec = Execution(
                id = ExecutionId("exec-running"),
                workflowId = "wf-status",
                workflowVersion = 1,
                currentState = "STATE1",
                status = ExecutionStatus.Running,
                input = mapOf(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            executionRepository.save(runningExec)

            val completedExec = Execution(
                id = ExecutionId("exec-completed"),
                workflowId = "wf-status",
                workflowVersion = 1,
                currentState = "END",
                status = ExecutionStatus.Completed,
                input = mapOf(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            executionRepository.save(completedExec)

            val failedExec = Execution(
                id = ExecutionId("exec-failed"),
                workflowId = "wf-status",
                workflowVersion = 1,
                currentState = "ERROR",
                status = ExecutionStatus.Failed("Network timeout"),
                input = mapOf(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            executionRepository.save(failedExec)

            val suspendedExec = Execution(
                id = ExecutionId("exec-suspended"),
                workflowId = "wf-status",
                workflowVersion = 1,
                currentState = "PAUSED",
                status = ExecutionStatus.Suspended,
                input = mapOf(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            executionRepository.save(suspendedExec)

            // Assert - Retrieve and verify each status
            val retrieved1 = executionRepository.findById(ExecutionId("exec-running"))
            assertTrue(retrieved1?.status is ExecutionStatus.Running)

            val retrieved2 = executionRepository.findById(ExecutionId("exec-completed"))
            assertTrue(retrieved2?.status is ExecutionStatus.Completed)

            val retrieved3 = executionRepository.findById(ExecutionId("exec-failed"))
            assertTrue(retrieved3?.status is ExecutionStatus.Failed)
            assertEquals("Network timeout", (retrieved3?.status as ExecutionStatus.Failed).reason)

            val retrieved4 = executionRepository.findById(ExecutionId("exec-suspended"))
            assertTrue(retrieved4?.status is ExecutionStatus.Suspended)
        }
    }

    @Test
    fun `should return empty list when no audit events found`() {
        runBlocking {
            // Act
            val result = auditRepository.findByExecutionId("non-existent-execution")

            // Assert
            assertTrue(result.isEmpty())
        }
    }
}
