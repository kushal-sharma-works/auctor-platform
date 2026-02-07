package com.auctor.definition.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WorkflowId value object.
 */
class WorkflowIdTest {
    
    @Test
    void shouldCreateWorkflowIdWithValidValue() {
        // When
        WorkflowId workflowId = new WorkflowId("test-workflow-456");
        
        // Then
        assertNotNull(workflowId);
        assertEquals("test-workflow-456", workflowId.value());
    }
    
    @Test
    void shouldRejectNullValue() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new WorkflowId(null)
        );
        
        assertEquals("WorkflowId value must not be blank", exception.getMessage());
    }
    
    @Test
    void shouldRejectBlankValue() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> new WorkflowId(""));
        assertThrows(IllegalArgumentException.class, () -> new WorkflowId("   "));
        assertThrows(IllegalArgumentException.class, () -> new WorkflowId("\t"));
        assertThrows(IllegalArgumentException.class, () -> new WorkflowId("\n"));
    }
    
    @Test
    void shouldHandleUuidFormat() {
        // Given
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        
        // When
        WorkflowId workflowId = new WorkflowId(uuid);
        
        // Then
        assertEquals(uuid, workflowId.value());
    }
    
    @Test
    void shouldHandleCustomIdFormat() {
        // Given
        String customId = "workflow-approval-v2";
        
        // When
        WorkflowId workflowId = new WorkflowId(customId);
        
        // Then
        assertEquals(customId, workflowId.value());
    }
    
    @Test
    void shouldImplementRecordEquality() {
        // Given
        WorkflowId id1 = new WorkflowId("test-id");
        WorkflowId id2 = new WorkflowId("test-id");
        WorkflowId id3 = new WorkflowId("different-id");
        
        // Then
        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertEquals(id1.hashCode(), id2.hashCode());
    }
    
    @Test
    void shouldImplementToString() {
        // Given
        WorkflowId workflowId = new WorkflowId("workflow-001");
        
        // When
        String toString = workflowId.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("workflow-001"));
    }
    
    @Test
    void shouldHandleSpecialCharacters() {
        // Given
        String idWithSpecialChars = "workflow_id-123.v2";
        
        // When
        WorkflowId workflowId = new WorkflowId(idWithSpecialChars);
        
        // Then
        assertEquals(idWithSpecialChars, workflowId.value());
    }
    
    @Test
    void shouldHandleLongIds() {
        // Given
        String longId = "workflow-" + "a".repeat(100);
        
        // When
        WorkflowId workflowId = new WorkflowId(longId);
        
        // Then
        assertEquals(longId, workflowId.value());
    }
    
    @Test
    void shouldNotBeEqualToPolicyId() {
        // Given
        WorkflowId workflowId = new WorkflowId("same-id");
        PolicyId policyId = new PolicyId("same-id");
        
        // Then - different types should not be equal
        assertNotEquals(workflowId, policyId);
    }
}
