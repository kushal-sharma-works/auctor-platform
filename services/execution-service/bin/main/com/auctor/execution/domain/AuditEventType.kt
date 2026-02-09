package com.auctor.execution.domain

/**
 * Enumeration of audit event types.
 * Used to categorize audit trail events.
 */
enum class AuditEventType {
    EXECUTION_STARTED,
    STATE_TRANSITION,
    POLICY_EVALUATED,
    EXECUTION_COMPLETED,
    EXECUTION_FAILED
}
