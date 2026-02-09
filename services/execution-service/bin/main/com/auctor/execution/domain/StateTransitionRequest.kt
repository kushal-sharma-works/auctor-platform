package com.auctor.execution.domain

import kotlinx.serialization.Serializable

/**
 * Request to advance an execution to the next state.
 */
@Serializable
data class StateTransitionRequest(
    val executionId: ExecutionId,
    val actor: String,
    val correlationId: String
) {
    init {
        require(actor.isNotBlank()) { "Actor cannot be blank" }
        require(correlationId.isNotBlank()) { "CorrelationId cannot be blank" }
    }
}
