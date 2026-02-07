package com.auctor.definition.domain.model;

/**
 * Represents a state transition in a workflow.
 * Validates that fromState and toState are different.
 */
public record Transition(
    String fromState,
    String toState,
    String policyRef,
    String guardExpression
) {
    public Transition {
        if (fromState == null || fromState.isBlank()) {
            throw new IllegalArgumentException("fromState must not be blank");
        }
        if (toState == null || toState.isBlank()) {
            throw new IllegalArgumentException("toState must not be blank");
        }
        if (fromState.equals(toState)) {
            throw new IllegalArgumentException("fromState must not equal toState");
        }
    }
}
