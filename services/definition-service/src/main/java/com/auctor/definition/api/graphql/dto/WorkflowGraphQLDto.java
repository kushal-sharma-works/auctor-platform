package com.auctor.definition.api.graphql.dto;

import com.auctor.definition.domain.model.Transition;
import com.auctor.definition.domain.model.WorkflowDefinition;

import java.util.List;

/**
 * GraphQL DTO for Workflow.
 * Maps domain model to GraphQL schema.
 */
public record WorkflowGraphQLDto(
    String id,
    String name,
    Integer version,
    String status,
    List<String> states,
    String initialState,
    List<TransitionGraphQLDto> transitions,
    String createdAt,
    String updatedAt
) {
    public static WorkflowGraphQLDto from(WorkflowDefinition workflow) {
        return new WorkflowGraphQLDto(
            workflow.id().value(),
            workflow.name(),
            workflow.version(),
            workflow.status().label(),
            workflow.states(),
            workflow.initialState(),
            workflow.transitions().stream()
                .map(TransitionGraphQLDto::from)
                .toList(),
            workflow.createdAt().toString(),
            workflow.updatedAt().toString()
        );
    }
}
