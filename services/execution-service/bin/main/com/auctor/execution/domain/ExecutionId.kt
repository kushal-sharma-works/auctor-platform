package com.auctor.execution.domain

import kotlinx.serialization.Serializable

/**
 * Inline value class wrapping execution ID.
 * Provides type safety and zero runtime overhead.
 */
@Serializable
@JvmInline
value class ExecutionId(val value: String) {
    init {
        require(value.isNotBlank()) { "ExecutionId cannot be blank" }
    }
    
    override fun toString(): String = value
}
