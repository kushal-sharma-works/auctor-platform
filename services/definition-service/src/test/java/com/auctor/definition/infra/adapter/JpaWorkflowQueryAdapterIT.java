package com.auctor.definition.infra.adapter;

import com.auctor.definition.IntegrationTestBase;
import com.auctor.definition.domain.model.*;
import com.auctor.definition.infra.jpa.TransitionDto;
import com.auctor.definition.infra.jpa.WorkflowJpaEntity;
import com.auctor.definition.infra.jpa.WorkflowJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JpaWorkflowQueryAdapterIT extends IntegrationTestBase {
    
    @Autowired
    private JpaWorkflowQueryAdapter adapter;
    
    @Autowired
    private WorkflowJpaRepository repository;
    
    private WorkflowId testWorkflowId;
    
    @BeforeEach
    void setup() {
        testWorkflowId = new WorkflowId("test-workflow-id");
    }
    
    @Test
    void shouldFindById() {
        // Given
        WorkflowJpaEntity entity = createWorkflowEntity(testWorkflowId.value(), 1, "DRAFT");
        repository.save(entity);
        
        // When
        Optional<WorkflowDefinition> result = adapter.findById(testWorkflowId);
        
        // Then
        assertTrue(result.isPresent());
        WorkflowDefinition workflow = result.get();
        assertEquals(testWorkflowId.value(), workflow.id().value());
        assertEquals("Test Workflow", workflow.name());
        assertEquals(1, workflow.version());
        assertInstanceOf(WorkflowStatus.Draft.class, workflow.status());
    }
    
    @Test
    void shouldReturnEmptyWhenWorkflowNotFound() {
        // When
        Optional<WorkflowDefinition> result = adapter.findById(new WorkflowId("non-existent"));
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    void shouldFindLatestVersion() {
        // Given - create multiple versions
        repository.save(createWorkflowEntity(testWorkflowId.value(), 1, "DRAFT"));
        repository.save(createWorkflowEntity(testWorkflowId.value(), 2, "PUBLISHED"));
        repository.save(createWorkflowEntity(testWorkflowId.value(), 3, "DRAFT"));
        
        // When
        Optional<WorkflowDefinition> result = adapter.findById(testWorkflowId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(3, result.get().version());
    }
    
    @Test
    void shouldFindByIdAndVersion() {
        // Given
        repository.save(createWorkflowEntity(testWorkflowId.value(), 1, "PUBLISHED"));
        repository.save(createWorkflowEntity(testWorkflowId.value(), 2, "DRAFT"));
        
        // When
        Optional<WorkflowDefinition> result = adapter.findByIdAndVersion(testWorkflowId, 1);
        
        // Then
        assertTrue(result.isPresent());
        WorkflowDefinition workflow = result.get();
        assertEquals(1, workflow.version());
        assertInstanceOf(WorkflowStatus.Published.class, workflow.status());
    }
    
    @Test
    void shouldReturnEmptyWhenVersionNotFound() {
        // Given
        repository.save(createWorkflowEntity(testWorkflowId.value(), 1, "DRAFT"));
        
        // When
        Optional<WorkflowDefinition> result = adapter.findByIdAndVersion(testWorkflowId, 99);
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    void shouldFindAllLatestVersions() {
        // Given - create multiple workflows with versions
        repository.save(createWorkflowEntity("workflow-1", 1, "DRAFT"));
        repository.save(createWorkflowEntity("workflow-1", 2, "PUBLISHED"));
        repository.save(createWorkflowEntity("workflow-2", 1, "DRAFT"));
        repository.save(createWorkflowEntity("workflow-3", 1, "PUBLISHED"));
        
        // When
        Page<WorkflowDefinition> result = adapter.findAll(PageRequest.of(0, 10));
        
        // Then
        assertEquals(3, result.getTotalElements());
        assertTrue(result.getContent().stream()
            .anyMatch(w -> w.id().value().equals("workflow-1") && w.version() == 2));
    }
    
    @Test
    void shouldHandlePagination() {
        // Given
        for (int i = 1; i <= 12; i++) {
            repository.save(createWorkflowEntity("workflow-" + i, 1, "DRAFT"));
        }
        
        // When
        Page<WorkflowDefinition> page1 = adapter.findAll(PageRequest.of(0, 5));
        Page<WorkflowDefinition> page2 = adapter.findAll(PageRequest.of(1, 5));
        
        // Then
        assertEquals(5, page1.getSize());
        assertEquals(12, page1.getTotalElements());
        assertEquals(3, page1.getTotalPages());
        assertNotEquals(page1.getContent().get(0).id(), page2.getContent().get(0).id());
    }
    
    @Test
    void shouldFindLatestPublished() {
        // Given
        repository.save(createWorkflowEntity(testWorkflowId.value(), 1, "PUBLISHED"));
        repository.save(createWorkflowEntity(testWorkflowId.value(), 2, "DRAFT"));
        repository.save(createWorkflowEntity(testWorkflowId.value(), 3, "PUBLISHED"));
        repository.save(createWorkflowEntity(testWorkflowId.value(), 4, "DRAFT"));
        
        // When
        Optional<WorkflowDefinition> result = adapter.findLatestPublished(testWorkflowId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(3, result.get().version());
        assertInstanceOf(WorkflowStatus.Published.class, result.get().status());
    }
    
    @Test
    void shouldReturnEmptyWhenNoPublishedVersion() {
        // Given
        repository.save(createWorkflowEntity(testWorkflowId.value(), 1, "DRAFT"));
        repository.save(createWorkflowEntity(testWorkflowId.value(), 2, "DRAFT"));
        
        // When
        Optional<WorkflowDefinition> result = adapter.findLatestPublished(testWorkflowId);
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    void shouldMapStatesCorrectly() {
        // Given
        WorkflowJpaEntity entity = createWorkflowEntity(testWorkflowId.value(), 1, "DRAFT");
        repository.save(entity);
        
        // When
        Optional<WorkflowDefinition> result = adapter.findById(testWorkflowId);
        
        // Then
        assertTrue(result.isPresent());
        List<String> states = result.get().states();
        assertEquals(3, states.size());
        assertTrue(states.contains("START"));
        assertTrue(states.contains("PROCESSING"));
        assertTrue(states.contains("END"));
    }
    
    @Test
    void shouldMapTransitionsCorrectly() {
        // Given
        WorkflowJpaEntity entity = createWorkflowEntity(testWorkflowId.value(), 1, "DRAFT");
        repository.save(entity);
        
        // When
        Optional<WorkflowDefinition> result = adapter.findById(testWorkflowId);
        
        // Then
        assertTrue(result.isPresent());
        List<Transition> transitions = result.get().transitions();
        assertEquals(2, transitions.size());
        assertEquals("START", transitions.get(0).fromState());
        assertEquals("PROCESSING", transitions.get(0).toState());
    }
    
    @Test
    void shouldPreserveTimestamps() {
        // Given
        Instant created = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant updated = Instant.now();
        WorkflowJpaEntity entity = createWorkflowEntity(testWorkflowId.value(), 1, "DRAFT");
        entity.setCreatedAt(created);
        entity.setUpdatedAt(updated);
        repository.save(entity);
        
        // When
        Optional<WorkflowDefinition> result = adapter.findById(testWorkflowId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(created.truncatedTo(ChronoUnit.SECONDS), result.get().createdAt().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(updated.truncatedTo(ChronoUnit.SECONDS), result.get().updatedAt().truncatedTo(ChronoUnit.SECONDS));
    }
    
    // Helper methods
    
    private WorkflowJpaEntity createWorkflowEntity(String id, int version, String status) {
        WorkflowJpaEntity entity = new WorkflowJpaEntity();
        entity.setId(id);
        entity.setVersion(version);
        entity.setName("Test Workflow");
        entity.setStatus(status);
        entity.setStates(List.of("START", "PROCESSING", "END"));
        entity.setInitialState("START");
        entity.setTransitions(List.of(
            new TransitionDto("START", "PROCESSING", null, null),
            new TransitionDto("PROCESSING", "END", "policy-ref", "guard-expr")
        ));
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }
}
