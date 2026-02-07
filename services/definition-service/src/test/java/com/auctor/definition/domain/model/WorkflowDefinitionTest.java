package com.auctor.definition.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WorkflowDefinition domain model.
 */
class WorkflowDefinitionTest {
    
    @Test
    void shouldCreateValidWorkflowDefinition() {
        // Given
        WorkflowId id = new WorkflowId("test-id");
        List<String> states = List.of("DRAFT", "APPROVED");
        List<Transition> transitions = List.of(
            new Transition("DRAFT", "APPROVED", null, null)
        );
        
        // When
        WorkflowDefinition workflow = new WorkflowDefinition(
            id, "Test Workflow", 1, new WorkflowStatus.Draft(),
            states, "DRAFT", transitions, Instant.now(), Instant.now()
        );
        
        // Then
        assertNotNull(workflow);
        assertEquals("test-id", workflow.id().value());
        assertEquals("Test Workflow", workflow.name());
        assertEquals(states, workflow.states());
    }
    
    @Test
    void shouldRejectBlankName() {
        assertThrows(IllegalArgumentException.class, () ->
            new WorkflowDefinition(
                new WorkflowId("id"), "", 1, new WorkflowStatus.Draft(),
                List.of("DRAFT"), "DRAFT", List.of(), Instant.now(), Instant.now()
            )
        );
    }
    
    @Test
    void shouldRejectEmptyStates() {
        assertThrows(IllegalArgumentException.class, () ->
            new WorkflowDefinition(
                new WorkflowId("id"), "Test", 1, new WorkflowStatus.Draft(),
                List.of(), "DRAFT", List.of(), Instant.now(), Instant.now()
            )
        );
    }
    
    @Test
    void shouldRejectInitialStateNotInStates() {
        assertThrows(IllegalArgumentException.class, () ->
            new WorkflowDefinition(
                new WorkflowId("id"), "Test", 1, new WorkflowStatus.Draft(),
                List.of("DRAFT"), "INVALID", List.of(), Instant.now(), Instant.now()
            )
        );
    }
    
    @Test
    void shouldRejectTransitionWithInvalidState() {
        List<String> states = List.of("DRAFT", "APPROVED");
        List<Transition> transitions = List.of(
            new Transition("DRAFT", "INVALID", null, null)
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            new WorkflowDefinition(
                new WorkflowId("id"), "Test", 1, new WorkflowStatus.Draft(),
                states, "DRAFT", transitions, Instant.now(), Instant.now()
            )
        );
    }
}
