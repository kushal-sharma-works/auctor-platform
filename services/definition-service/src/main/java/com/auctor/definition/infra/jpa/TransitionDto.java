package com.auctor.definition.infra.jpa;

/**
 * DTO for storing transitions in JSONB.
 */
public record TransitionDto(
    String fromState,
    String toState,
    String policyRef,
    String guardExpression
) {
}
