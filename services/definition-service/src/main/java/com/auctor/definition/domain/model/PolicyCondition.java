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
        if (requiresNumeric(operator) && !isNumeric(value)) {
            throw new IllegalArgumentException("value must be numeric for operator " + operator);
        }
    }

    private static boolean requiresNumeric(Operator operator) {
        return operator == Operator.GT
            || operator == Operator.LT
            || operator == Operator.GTE
            || operator == Operator.LTE;
    }

    private static boolean isNumeric(String value) {
        String trimmed = value.trim();
        try {
            Double.parseDouble(trimmed);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
