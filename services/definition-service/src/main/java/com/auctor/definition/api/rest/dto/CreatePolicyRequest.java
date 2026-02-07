package com.auctor.definition.api.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request to create a policy.
 */
public record CreatePolicyRequest(
    @NotBlank(message = "name must not be blank")
    String name,
    
    @NotNull(message = "conditions must not be null")
    List<PolicyConditionRequest> conditions
) {
}
