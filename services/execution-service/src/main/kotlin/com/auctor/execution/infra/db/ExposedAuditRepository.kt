package com.auctor.execution.infra.db

import com.auctor.execution.domain.AuditEvent
import com.auctor.execution.domain.AuditEventType
import com.auctor.execution.domain.AuditRepository
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Exposed implementation of AuditRepository.
 * Append-only - no update or delete operations.
 */
class ExposedAuditRepository : AuditRepository {
    
    override suspend fun append(event: AuditEvent): Unit = dbQuery {
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
    
    override suspend fun findByExecutionId(executionId: String): List<AuditEvent> = dbQuery {
        AuditEvents.selectAll()
            .where { AuditEvents.executionId eq executionId }
            .orderBy(AuditEvents.timestamp to SortOrder.ASC)
            .map { rowToAuditEvent(it) }
    }
    
    private fun rowToAuditEvent(row: ResultRow): AuditEvent {
        return AuditEvent(
            id = row[AuditEvents.id],
            executionId = row[AuditEvents.executionId],
            timestamp = row[AuditEvents.timestamp],
            eventType = AuditEventType.valueOf(row[AuditEvents.eventType]),
            fromState = row[AuditEvents.fromState],
            toState = row[AuditEvents.toState],
            policyId = row[AuditEvents.policyId],
            policyResult = row[AuditEvents.policyResult],
            explanation = row[AuditEvents.explanation],
            actor = row[AuditEvents.actor],
            correlationId = row[AuditEvents.correlationId]
        )
    }
    
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
