package com.auctor.definition.api.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Policy condition request DTO.
 */
public record PolicyConditionRequest(
    @NotBlank(message = "field must not be blank")
    String field,
    
    @NotBlank(message = "operator must not be blank")
    String operator,
    
    @NotNull(message = "value must not be null")
    String value
) {
}
