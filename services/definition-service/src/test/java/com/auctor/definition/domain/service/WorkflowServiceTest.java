package com.auctor.definition.domain.service;

import com.auctor.definition.domain.exception.EntityNotFoundException;
import com.auctor.definition.domain.model.*;
import com.auctor.definition.domain.port.WorkflowCommandPort;
import com.auctor.definition.domain.port.WorkflowQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WorkflowService.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {
    
    @Mock
    private WorkflowCommandPort commandPort;
    
    @Mock
    private WorkflowQueryPort queryPort;
    
    private WorkflowService workflowService;
    
    @BeforeEach
    void setUp() {
        workflowService = new WorkflowService(commandPort, queryPort);
    }
    
    @Test
    void shouldCreateWorkflow() {
        // Given
        WorkflowDefinition expected = createWorkflow(new WorkflowStatus.Draft());
        when(commandPort.save(any())).thenReturn(expected);
        
        // When
        WorkflowDefinition result = workflowService.create(
            "Test Workflow",
            List.of("DRAFT", "APPROVED"),
            "DRAFT",
            List.of(new Transition("DRAFT", "APPROVED", null, null))
        );
        
        // Then
        assertNotNull(result);
        verify(commandPort).save(any());
    }
    
    @Test
    void shouldPublishDraftWorkflow() {
        // Given
        WorkflowDefinition draft = createWorkflow(new WorkflowStatus.Draft());
        WorkflowDefinition published = createWorkflow(new WorkflowStatus.Published());
        
        when(queryPort.findById(any())).thenReturn(Optional.of(draft));
        when(commandPort.save(any())).thenReturn(published);
        
        // When
        WorkflowDefinition result = workflowService.publish(draft.id());
        
        // Then
        assertNotNull(result);
        verify(commandPort).save(any());
    }
    
    @Test
    void shouldThrowWhenPublishingNonDraftWorkflow() {
        // Given
        WorkflowDefinition published = createWorkflow(new WorkflowStatus.Published());
        when(queryPort.findById(any())).thenReturn(Optional.of(published));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            workflowService.publish(published.id())
        );
    }
    
    @Test
    void shouldThrowWhenPublishingWorkflowWithNoTransitions() {
        // Given
        WorkflowDefinition draft = new WorkflowDefinition(
            new WorkflowId("test-id"),
            "Test",
            1,
            new WorkflowStatus.Draft(),
            List.of("DRAFT"),
            "DRAFT",
            List.of(), // No transitions
            Instant.now(),
            Instant.now()
        );
        when(queryPort.findById(any())).thenReturn(Optional.of(draft));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            workflowService.publish(draft.id())
        );
    }
    
    @Test
    void shouldGetWorkflowById() {
        // Given
        WorkflowDefinition workflow = createWorkflow(new WorkflowStatus.Draft());
        when(queryPort.findById(any())).thenReturn(Optional.of(workflow));
        
        // When
        WorkflowDefinition result = workflowService.getById(workflow.id());
        
        // Then
        assertNotNull(result);
        assertEquals(workflow.id(), result.id());
    }
    
    @Test
    void shouldThrowWhenWorkflowNotFound() {
        // Given
        when(queryPort.findById(any())).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(EntityNotFoundException.class, () ->
            workflowService.getById(new WorkflowId("missing-id"))
        );
    }
    
    @Test
    void shouldGetWorkflowByIdAndVersion() {
        // Given
        WorkflowDefinition workflow = createWorkflow(new WorkflowStatus.Draft());
        when(queryPort.findByIdAndVersion(any(), anyInt())).thenReturn(Optional.of(workflow));
        
        // When
        WorkflowDefinition result = workflowService.getByIdAndVersion(workflow.id(), 1);
        
        // Then
        assertNotNull(result);
        assertEquals(workflow.id(), result.id());
    }
    
    @Test
    void shouldListAllWorkflows() {
        // Given
        Page<WorkflowDefinition> page = new PageImpl<>(List.of(createWorkflow(new WorkflowStatus.Draft())));
        when(queryPort.findAll(any())).thenReturn(page);
        
        // When
        Page<WorkflowDefinition> result = workflowService.listAll(PageRequest.of(0, 20));
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
    
    private WorkflowDefinition createWorkflow(WorkflowStatus status) {
        return new WorkflowDefinition(
            new WorkflowId("test-id"),
            "Test Workflow",
            1,
            status,
            List.of("DRAFT", "APPROVED"),
            "DRAFT",
            List.of(new Transition("DRAFT", "APPROVED", null, null)),
            Instant.now(),
            Instant.now()
        );
    }
}
