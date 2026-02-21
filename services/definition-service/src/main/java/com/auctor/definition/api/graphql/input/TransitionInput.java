package com.auctor.definition.api.graphql.input;

import com.auctor.definition.domain.model.Transition;

/**
 * GraphQL input for transition.
 */
public record TransitionInput(
    String fromState,
    String toState,
    String policyRef
) {
    public Transition toDomain() {
        return new Transition(fromState, toState, policyRef, null);
    }
}
