package com.auctor.execution.infra.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Exposed Table definition for executions.
 * Maps to the executions table in PostgreSQL.
 */
object Executions : Table("executions") {
    val id = varchar("id", 64)
    val workflowId = varchar("workflow_id", 36)
    val workflowVersion = integer("workflow_version")
    val currentState = varchar("current_state", 100)
    val status = varchar("status", 255)
    val input = text("input") // Store as JSON string
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
