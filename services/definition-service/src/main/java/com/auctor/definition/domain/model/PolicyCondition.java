package com.auctor.definition.domain.model;

/**
 * Represents a single condition in a policy definition.
 */
public record PolicyCondition(
    String field,
    Operator operator,
    String value
) {
    public PolicyCondition {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
        if (operator == null) {
            throw new IllegalArgumentException("operator must not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
    }
}
