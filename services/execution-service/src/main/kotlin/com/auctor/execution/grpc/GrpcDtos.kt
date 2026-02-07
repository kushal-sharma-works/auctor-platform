package com.auctor.execution.grpc

import kotlinx.serialization.Serializable

/**
 * DTO for Workflow from definition-service.
 */
@Serializable
data class WorkflowDto(
    val id: String,
    val name: String,
    val version: Int,
    val status: String,
    val states: List<String>,
    val initialState: String,
    val transitions: List<TransitionDto>
)

/**
 * DTO for Transition
 */
@Serializable
data class TransitionDto(
    val fromState: String,
    val toState: String,
    val policyRef: String?
)

/**
 * DTO for Policy from definition-service.
 */
@Serializable
data class PolicyDto(
    val id: String,
    val name: String,
    val version: Int,
    val status: String,
    val conditions: List<PolicyConditionDto>
)

/**
 * DTO for Policy Condition
 */
@Serializable
data class PolicyConditionDto(
    val field: String,
    val operator: String,
    val value: String
)

/**
 * DTO for Policy Evaluation Result
 */
@Serializable
data class PolicyEvaluationResultDto(
    val allowed: Boolean,
    val explanation: String
)
