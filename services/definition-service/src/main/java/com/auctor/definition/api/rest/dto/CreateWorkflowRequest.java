package com.auctor.definition.api.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request to create a workflow.
 */
public record CreateWorkflowRequest(
    @NotBlank(message = "name must not be blank")
    String name,
    
    @NotEmpty(message = "states must not be empty")
    List<String> states,
    
    @NotBlank(message = "initialState must not be blank")
    String initialState,
    
    @NotNull(message = "transitions must not be null")
    List<TransitionRequest> transitions
) {
}
