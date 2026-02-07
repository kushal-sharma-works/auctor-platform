package com.auctor.definition.api.rest.dto;

/**
 * Policy condition response DTO.
 */
public record PolicyConditionResponse(
    String field,
    String operator,
    String value
) {
}
