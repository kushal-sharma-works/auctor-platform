package com.auctor.definition.api.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Transition request DTO.
 */
public record TransitionRequest(
    @NotBlank(message = "fromState must not be blank")
    String fromState,
    
    @NotBlank(message = "toState must not be blank")
    String toState,
    
    String policyRef,
    String guardExpression
) {
}
