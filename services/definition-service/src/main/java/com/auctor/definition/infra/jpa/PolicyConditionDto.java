package com.auctor.definition.infra.jpa;

/**
 * DTO for storing policy conditions in JSONB.
 */
public record PolicyConditionDto(
    String field,
    String operator,
    String value
) {
}
