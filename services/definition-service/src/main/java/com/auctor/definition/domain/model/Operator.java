package com.auctor.definition.domain.model;

/**
 * Enum representing comparison operators for policy conditions.
 */
public enum Operator {
    EQ,      // Equals
    NEQ,     // Not equals
    GT,      // Greater than
    LT,      // Less than
    GTE,     // Greater than or equal to
    LTE,     // Less than or equal to
    IN,      // In set (comma-separated)
    NOT_IN   // Not in set (comma-separated)
}
