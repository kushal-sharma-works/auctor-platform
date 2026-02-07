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
        is Failed -> "FAILED:$reason"
        is Suspended -> "SUSPENDED"
    }
    
    companion object {
        fun fromStorageString(value: String): ExecutionStatus = when {
            value == "RUNNING" -> Running
            value == "COMPLETED" -> Completed
            value.startsWith("FAILED:") -> Failed(value.removePrefix("FAILED:"))
            value == "SUSPENDED" -> Suspended
            else -> throw IllegalArgumentException("Unknown ExecutionStatus: $value")
        }
    }
}
