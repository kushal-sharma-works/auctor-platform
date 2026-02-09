package com.auctor.execution.infra.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Exposed Table definition for audit events.
 * Append-only table - no updates or deletes.
 */
object AuditEvents : Table("audit_events") {
    val id = varchar("id", 64)
    val executionId = varchar("execution_id", 64)
    val timestamp = timestamp("timestamp")
    val eventType = varchar("event_type", 50)
    val fromState = varchar("from_state", 100).nullable()
    val toState = varchar("to_state", 100).nullable()
    val policyId = varchar("policy_id", 100).nullable()
    val policyResult = bool("policy_result").nullable()
    val explanation = text("explanation").nullable()
    val actor = varchar("actor", 100)
    val correlationId = varchar("correlation_id", 36)

    override val primaryKey = PrimaryKey(id)
}
