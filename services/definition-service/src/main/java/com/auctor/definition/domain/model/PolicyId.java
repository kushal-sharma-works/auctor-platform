package com.auctor.definition.domain.model;

/**
 * Value object representing a Policy identifier.
 * Validates that the id is not blank.
 */
public record PolicyId(String value) {
    public PolicyId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PolicyId value must not be blank");
        }
    }
}
