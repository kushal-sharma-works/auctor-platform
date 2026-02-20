package com.auctor.execution.infra.db

import com.auctor.execution.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Exposed implementation of ExecutionRepository.
 * Uses Exposed SQL DSL for database operations with suspended transactions.
 */
class ExposedExecutionRepository : ExecutionRepository {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun save(execution: Execution): Execution = dbQuery {
        Executions.insert {
            it[id] = execution.id.value
            it[workflowId] = execution.workflowId
            it[workflowVersion] = execution.workflowVersion
            it[currentState] = execution.currentState
            it[status] = execution.status.toStorageString()
            it[input] = json.encodeToString(execution.input)
            it[createdAt] = execution.createdAt
            it[updatedAt] = execution.updatedAt
        }
        execution
    }

    override suspend fun saveWithAudit(execution: Execution, auditEvents: List<AuditEvent>): Execution = dbQuery {
        Executions.insert {
            it[id] = execution.id.value
            it[workflowId] = execution.workflowId
            it[workflowVersion] = execution.workflowVersion
            it[currentState] = execution.currentState
            it[status] = execution.status.toStorageString()
            it[input] = json.encodeToString(execution.input)
            it[createdAt] = execution.createdAt
            it[updatedAt] = execution.updatedAt
        }
        auditEvents.forEach { insertAuditEvent(it) }
        execution
    }
    
    override suspend fun findById(id: ExecutionId): Execution? = dbQuery {
        Executions.selectAll()
            .where { Executions.id eq id.value }
            .map { rowToExecution(it) }
            .singleOrNull()
    }
    
    override suspend fun findAll(limit: Int, offset: Int): List<Execution> = dbQuery {
        Executions.selectAll()
            .orderBy(Executions.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { rowToExecution(it) }
    }
    
    override suspend fun update(execution: Execution): Execution = dbQuery {
        val updated = Executions.update({ Executions.id eq execution.id.value }) {
            it[workflowId] = execution.workflowId
            it[workflowVersion] = execution.workflowVersion
            it[currentState] = execution.currentState
            it[status] = execution.status.toStorageString()
            it[input] = json.encodeToString(execution.input)
            it[updatedAt] = execution.updatedAt
        }
        require(updated == 1) { "Execution with id ${execution.id} not found" }
        execution
    }

    override suspend fun updateWithAudit(execution: Execution, auditEvents: List<AuditEvent>): Execution = dbQuery {
        val updated = Executions.update({ Executions.id eq execution.id.value }) {
            it[workflowId] = execution.workflowId
            it[workflowVersion] = execution.workflowVersion
            it[currentState] = execution.currentState
            it[status] = execution.status.toStorageString()
            it[input] = json.encodeToString(execution.input)
            it[updatedAt] = execution.updatedAt
        }
        require(updated == 1) { "Execution with id ${execution.id} not found" }
        auditEvents.forEach { insertAuditEvent(it) }
        execution
    }
    
    private fun rowToExecution(row: ResultRow): Execution {
        val inputMap = json.decodeFromString<Map<String, String>>(row[Executions.input])
        return Execution(
            id = ExecutionId(row[Executions.id]),
            workflowId = row[Executions.workflowId],
            workflowVersion = row[Executions.workflowVersion],
            currentState = row[Executions.currentState],
            status = ExecutionStatus.fromStorageString(row[Executions.status]),
            input = inputMap,
            createdAt = row[Executions.createdAt],
            updatedAt = row[Executions.updatedAt]
        )
    }

    private fun insertAuditEvent(event: AuditEvent) {
        AuditEvents.insert {
            it[id] = event.id
            it[executionId] = event.executionId
            it[timestamp] = event.timestamp
            it[eventType] = event.eventType.name
            it[fromState] = event.fromState
            it[toState] = event.toState
            it[policyId] = event.policyId
            it[policyResult] = event.policyResult
            it[explanation] = event.explanation
            it[actor] = event.actor
            it[correlationId] = event.correlationId
        }
    }
    
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
