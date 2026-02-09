package com.auctor.execution.domain

/**
 * Repository interface for AuditEvent persistence.
 * Append-only - no update or delete operations.
 */
interface AuditRepository {
    /**
     * Append a new audit event.
     * This is an append-only operation - events are never updated or deleted.
     */
    suspend fun append(event: AuditEvent)
    
    /**
     * Find all audit events for a specific execution, ordered by timestamp.
     * @param executionId Execution ID to query
     * @return List of audit events ordered by timestamp ascending
     */
    suspend fun findByExecutionId(executionId: String): List<AuditEvent>
}
