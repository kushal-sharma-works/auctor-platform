package com.auctor.execution.domain

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Immutable audit event representing a single auditable action.
 * Append-only - never updated or deleted.
 */
@Serializable
data class AuditEvent(
    val id: String,
    val executionId: String,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant,
    val eventType: AuditEventType,
    val fromState: String? = null,
    val toState: String? = null,
    val policyId: String? = null,
    val policyResult: Boolean? = null,
    val explanation: String? = null,
    val actor: String,
    val correlationId: String
) {
    init {
        require(id.isNotBlank()) { "Event id cannot be blank" }
        require(executionId.isNotBlank()) { "ExecutionId cannot be blank" }
        require(actor.isNotBlank()) { "Actor cannot be blank" }
        require(correlationId.isNotBlank()) { "CorrelationId cannot be blank" }
    }
}
