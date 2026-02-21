package com.auctor.definition.api.graphql.dto;

import com.auctor.definition.domain.model.PolicyDefinition;

import java.util.List;

/**
 * GraphQL DTO for Policy.
 * Maps domain model to GraphQL schema.
 */
public record PolicyGraphQLDto(
    String id,
    String name,
    Integer version,
    String status,
    List<PolicyConditionGraphQLDto> conditions,
    String createdAt
) {
    public static PolicyGraphQLDto from(PolicyDefinition policy) {
        return new PolicyGraphQLDto(
            policy.id().value(),
            policy.name(),
            policy.version(),
            policy.status().label(),
            policy.conditions().stream()
                .map(PolicyConditionGraphQLDto::from)
                .toList(),
            policy.createdAt().toString()
        );
    }
}
