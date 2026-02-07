package com.auctor.definition.api.rest.dto;

/**
 * Transition response DTO.
 */
public record TransitionResponse(
    String fromState,
    String toState,
    String policyRef,
    String guardExpression
) {
}
