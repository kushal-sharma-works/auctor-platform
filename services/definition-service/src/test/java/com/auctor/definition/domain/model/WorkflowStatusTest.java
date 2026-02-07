package com.auctor.definition.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WorkflowStatus sealed interface.
 */
class WorkflowStatusTest {
    
    @Test
    void shouldReturnCorrectLabelForDraft() {
        WorkflowStatus status = new WorkflowStatus.Draft();
        assertEquals("DRAFT", status.label());
    }
    
    @Test
    void shouldReturnCorrectLabelForPublished() {
        WorkflowStatus status = new WorkflowStatus.Published();
        assertEquals("PUBLISHED", status.label());
    }
    
    @Test
    void shouldCreateStatusFromLabel() {
        WorkflowStatus draft = WorkflowStatus.fromLabel("DRAFT");
        assertTrue(draft instanceof WorkflowStatus.Draft);
        
        WorkflowStatus published = WorkflowStatus.fromLabel("PUBLISHED");
        assertTrue(published instanceof WorkflowStatus.Published);
    }
    
    @Test
    void shouldThrowExceptionForInvalidLabel() {
        assertThrows(IllegalArgumentException.class, () ->
            WorkflowStatus.fromLabel("INVALID")
        );
    }
}
