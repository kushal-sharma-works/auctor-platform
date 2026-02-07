package com.auctor.definition.domain.model;

/**
 * Result of policy evaluation.
 */
public record EvaluationResult(
    boolean allowed,
    String explanation
) {
}
