package com.auctor.definition.domain.model;

/**
 * Sealed interface representing the status of a workflow definition.
 * Only permits Draft and Published states.
 */
public sealed interface WorkflowStatus permits WorkflowStatus.Draft, WorkflowStatus.Published {
    
    String label();
    
    record Draft() implements WorkflowStatus {
        @Override
        public String label() {
            return "DRAFT";
        }
    }
    
    record Published() implements WorkflowStatus {
        @Override
        public String label() {
            return "PUBLISHED";
        }
    }
    
    static WorkflowStatus fromLabel(String label) {
        return switch (label) {
            case "DRAFT" -> new Draft();
            case "PUBLISHED" -> new Published();
            default -> throw new IllegalArgumentException("Unknown workflow status: " + label);
        };
    }
}
