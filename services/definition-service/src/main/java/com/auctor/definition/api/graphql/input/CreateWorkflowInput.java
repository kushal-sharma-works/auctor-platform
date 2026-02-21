package com.auctor.definition.api.graphql.input;

import java.util.List;

/**
 * GraphQL input for creating a workflow.
 */
public record CreateWorkflowInput(
    String name,
    List<String> states,
    String initialState,
    List<TransitionInput> transitions
) {
}
