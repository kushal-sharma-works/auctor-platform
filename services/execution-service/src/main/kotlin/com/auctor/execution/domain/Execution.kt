package com.auctor.execution.domain

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Core execution entity representing a workflow execution instance.
 * Immutable by design - use copy() for updates.
 */
@Serializable
data class Execution(
    val id: ExecutionId,
    val workflowId: String,
    val workflowVersion: Int,
    val currentState: String,
    val status: ExecutionStatus,
    val input: Map<String, String>,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant
) {
    init {
        require(workflowId.isNotBlank()) { "workflowId cannot be blank" }
        require(workflowVersion > 0) { "workflowVersion must be positive" }
        require(currentState.isNotBlank()) { "currentState cannot be blank" }
    }
}

// Custom serializer for Instant
object InstantSerializer : kotlinx.serialization.KSerializer<Instant> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("Instant", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Instant = Instant.parse(decoder.decodeString())
}
