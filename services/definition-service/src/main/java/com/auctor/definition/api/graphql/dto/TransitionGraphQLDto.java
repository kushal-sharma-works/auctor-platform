package com.auctor.definition.api.graphql.dto;

import com.auctor.definition.domain.model.Transition;

/**
 * GraphQL DTO for Transition.
 */
public record TransitionGraphQLDto(
    String fromState,
    String toState,
    String policyRef
) {
    public static TransitionGraphQLDto from(Transition transition) {
        return new TransitionGraphQLDto(
            transition.fromState(),
            transition.toState(),
            transition.policyRef()
        );
    }
    
    public Transition toDomain() {
        return new Transition(fromState, toState, policyRef, null);
    }
}
