package com.auctor.definition.domain.exception;

/**
 * Exception thrown when an entity is not found.
 */
public class EntityNotFoundException extends RuntimeException {
    
    public EntityNotFoundException(String message) {
        super(message);
    }
    
    public EntityNotFoundException(String entityType, String id) {
        super(entityType + " not found with id: " + id);
    }
}
