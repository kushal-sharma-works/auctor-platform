package com.auctor.execution.domain

import kotlinx.serialization.Serializable

/**
 * Result of a policy evaluation.
 */
@Serializable
data class PolicyEvaluationResult(
    val policyId: String,
    val allowed: Boolean,
    val explanation: String
)
