package com.auctor.execution.domain

import kotlinx.serialization.Serializable

/**
 * Sealed class hierarchy representing execution status.
 * Provides exhaustive when expressions and type-safe pattern matching.
 */
@Serializable
sealed class ExecutionStatus {
    @Serializable
    data object Running : ExecutionStatus()
    
    @Serializable
    data object Completed : ExecutionStatus()
    
    @Serializable
    data class Failed(val reason: String) : ExecutionStatus()
    
    @Serializable
    data object Suspended : ExecutionStatus()
    
    fun toStorageString(): String = when (this) {
        is Running -> "RUNNING"
        is Completed -> "COMPLETED"
        is Failed -> "FAILED:${encodeReason(reason)}"
        is Suspended -> "SUSPENDED"
    }
    
    companion object {
        fun fromStorageString(value: String): ExecutionStatus = when {
            value == "RUNNING" -> Running
            value == "COMPLETED" -> Completed
            value.startsWith("FAILED:") -> {
                val encoded = value.removePrefix("FAILED:")
                Failed(decodeReason(encoded))
            }
            value == "SUSPENDED" -> Suspended
            else -> throw IllegalArgumentException("Unknown ExecutionStatus: $value")
        }

        private fun encodeReason(reason: String): String {
            return java.util.Base64.getEncoder().encodeToString(reason.toByteArray(Charsets.UTF_8))
        }

        private fun decodeReason(encoded: String): String {
            return try {
                String(java.util.Base64.getDecoder().decode(encoded), Charsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                encoded
            }
        }
    }
}
