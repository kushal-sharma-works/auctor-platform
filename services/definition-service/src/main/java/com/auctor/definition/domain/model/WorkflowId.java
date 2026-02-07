package com.auctor.definition.domain.model;

/**
 * Value object representing a Workflow identifier.
 * Validates that the id is not blank.
 */
public record WorkflowId(String value) {
    public WorkflowId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("WorkflowId value must not be blank");
        }
    }
}
