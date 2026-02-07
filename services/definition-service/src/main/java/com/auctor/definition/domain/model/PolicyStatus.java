package com.auctor.definition.domain.model;

/**
 * Sealed interface representing the status of a policy definition.
 * Only permits Draft and Published states.
 */
public sealed interface PolicyStatus permits PolicyStatus.Draft, PolicyStatus.Published {
    
    String label();
    
    record Draft() implements PolicyStatus {
        @Override
        public String label() {
            return "DRAFT";
        }
    }
    
    record Published() implements PolicyStatus {
        @Override
        public String label() {
            return "PUBLISHED";
        }
    }
    
    static PolicyStatus fromLabel(String label) {
        return switch (label) {
            case "DRAFT" -> new Draft();
            case "PUBLISHED" -> new Published();
            default -> throw new IllegalArgumentException("Unknown policy status: " + label);
        };
    }
}
