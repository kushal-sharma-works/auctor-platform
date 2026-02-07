package com.auctor.definition.infra.adapter;

import com.auctor.definition.IntegrationTestBase;
import com.auctor.definition.domain.model.*;
import com.auctor.definition.infra.jpa.WorkflowJpaEntity;
import com.auctor.definition.infra.jpa.WorkflowJpaRepository;
import com.auctor.definition.infra.jpa.WorkflowDefinitionId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JpaWorkflowCommandAdapterIT extends IntegrationTestBase {
    
    @Autowired
    private JpaWorkflowCommandAdapter adapter;
    
    @Autowired
    private WorkflowJpaRepository repository;
    
    @Test
    void shouldSaveNewWorkflow() {
        // Given
        WorkflowDefinition workflow = new WorkflowDefinition(
            new WorkflowId("new-workflow"),
            "New Workflow",
            1,
            new WorkflowStatus.Draft(),
            List.of("START", "END"),
            "START",
            List.of(new Transition("START", "END", null, null)),
            Instant.now(),
            Instant.now()
        );
        
        // When
        WorkflowDefinition saved = adapter.save(workflow);
        
        // Then
        assertNotNull(saved);
        assertEquals(workflow.id(), saved.id());
        assertEquals(workflow.name(), saved.name());
        
        // Verify saved in repository
        Optional<WorkflowJpaEntity> entity = repository.findById(new WorkflowDefinitionId("new-workflow", 1));
        assertTrue(entity.isPresent());
        assertEquals("New Workflow", entity.get().getName());
    }
    
    @Test
    void shouldUpdateExistingWorkflow() {
        // Given - save initial version
        WorkflowDefinition initial = new WorkflowDefinition(
            new WorkflowId("update-workflow"),
            "Initial Name",
            1,
            new WorkflowStatus.Draft(),
            List.of("START", "END"),
            "START",
            List.of(new Transition("START", "END", null, null)),
            Instant.now(),
            Instant.now()
        );
        adapter.save(initial);
        
        // When - save updated version
        WorkflowDefinition updated = new WorkflowDefinition(
            initial.id(),
            "Updated Name",
            2,
            new WorkflowStatus.Published(),
            initial.states(),
            initial.initialState(),
            initial.transitions(),
            initial.createdAt(),
            Instant.now()
        );
        WorkflowDefinition saved = adapter.save(updated);
        
        // Then
        assertEquals("Updated Name", saved.name());
        assertEquals(2, saved.version());
        assertInstanceOf(WorkflowStatus.Published.class, saved.status());
    }
    
    @Test
    void shouldPreserveAllFields() {
        // Given
        Instant created = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant updated = Instant.now();
        List<String> states = List.of("DRAFT", "REVIEW", "APPROVED", "REJECTED");
        List<Transition> transitions = List.of(
            new Transition("DRAFT", "REVIEW", null, null),
            new Transition("REVIEW", "APPROVED", "approval-policy", null),
            new Transition("REVIEW", "REJECTED", null, "status == 'INVALID'")
        );
        
        WorkflowDefinition workflow = new WorkflowDefinition(
            new WorkflowId("complete-workflow"),
            "Complete Workflow",
            1,
            new WorkflowStatus.Draft(),
            states,
            "DRAFT",
            transitions,
            created,
            updated
        );
        
        // When
        WorkflowDefinition saved = adapter.save(workflow);
        
        // Then
        assertEquals(4, saved.states().size());
        assertEquals(3, saved.transitions().size());
        assertEquals("DRAFT", saved.initialState());
        assertEquals(created.truncatedTo(ChronoUnit.SECONDS), saved.createdAt().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(updated.truncatedTo(ChronoUnit.SECONDS), saved.updatedAt().truncatedTo(ChronoUnit.SECONDS));
    }
    
    @Test
    void shouldHandleComplexTransitions() {
        // Given
        List<Transition> transitions = List.of(
            new Transition("START", "MIDDLE", "policy-1", null),
            new Transition("MIDDLE", "END", null, "amount > 1000"),
            new Transition("MIDDLE", "REJECTED", "policy-2", "status == 'BLOCKED'"),
            new Transition("START", "END", null, null) // Direct path
        );
        
        WorkflowDefinition workflow = new WorkflowDefinition(
            new WorkflowId("complex-workflow"),
            "Complex Workflow",
            1,
            new WorkflowStatus.Draft(),
            List.of("START", "MIDDLE", "END", "REJECTED"),
            "START",
            transitions,
            Instant.now(),
            Instant.now()
        );
        
        // When
        WorkflowDefinition saved = adapter.save(workflow);
        
        // Then
        assertEquals(4, saved.transitions().size());
        assertTrue(saved.transitions().stream()
            .anyMatch(t -> "policy-1".equals(t.policyRef())));
        assertTrue(saved.transitions().stream()
            .anyMatch(t -> "amount > 1000".equals(t.guardExpression())));
    }
    
    @Test
    void shouldSaveMultipleVersions() {
        // Given
        WorkflowId id = new WorkflowId("versioned-workflow");
        List<String> states = List.of("A", "B");
        List<Transition> transitions = List.of(new Transition("A", "B", null, null));
        
        // When - save multiple versions
        adapter.save(new WorkflowDefinition(id, "V1", 1, new WorkflowStatus.Draft(),
            states, "A", transitions, Instant.now(), Instant.now()));
        adapter.save(new WorkflowDefinition(id, "V2", 2, new WorkflowStatus.Published(),
            states, "A", transitions, Instant.now(), Instant.now()));
        adapter.save(new WorkflowDefinition(id, "V3", 3, new WorkflowStatus.Draft(),
            states, "A", transitions, Instant.now(), Instant.now()));
        
        // Then - all versions should exist
        Optional<WorkflowJpaEntity> v1 = repository.findByIdAndVersion(id.value(), 1);
        Optional<WorkflowJpaEntity> v2 = repository.findByIdAndVersion(id.value(), 2);
        Optional<WorkflowJpaEntity> v3 = repository.findByIdAndVersion(id.value(), 3);
        
        assertTrue(v1.isPresent());
        assertTrue(v2.isPresent());
        assertTrue(v3.isPresent());
        assertEquals("V1", v1.get().getName());
        assertEquals("V2", v2.get().getName());
        assertEquals("V3", v3.get().getName());
    }
    
    @Test
    void shouldRoundTripSuccessfully() {
        // Given
        WorkflowDefinition original = new WorkflowDefinition(
            new WorkflowId("roundtrip-workflow"),
            "Round Trip Test",
            1,
            new WorkflowStatus.Published(),
            List.of("STATE1", "STATE2", "STATE3"),
            "STATE1",
            List.of(
                new Transition("STATE1", "STATE2", "policy-ref", null),
                new Transition("STATE2", "STATE3", null, "guard-expr")
            ),
            Instant.now(),
            Instant.now()
        );
        
        // When - save and retrieve
        adapter.save(original);
        Optional<WorkflowJpaEntity> entity = repository.findById(new WorkflowDefinitionId("roundtrip-workflow", 1));
        WorkflowDefinition retrieved = DomainMapper.toWorkflowDomain(entity.get());
        
        // Then - should match
        assertEquals(original.id(), retrieved.id());
        assertEquals(original.name(), retrieved.name());
        assertEquals(original.version(), retrieved.version());
        assertEquals(original.states().size(), retrieved.states().size());
        assertEquals(original.transitions().size(), retrieved.transitions().size());
        assertEquals(original.status().label(), retrieved.status().label());
    }
    
    @Test
    void shouldHandleNullPolicyRefAndGuardExpression() {
        // Given
        WorkflowDefinition workflow = new WorkflowDefinition(
            new WorkflowId("null-fields-workflow"),
            "Null Fields Test",
            1,
            new WorkflowStatus.Draft(),
            List.of("A", "B"),
            "A",
            List.of(new Transition("A", "B", null, null)),
            Instant.now(),
            Instant.now()
        );
        
        // When
        WorkflowDefinition saved = adapter.save(workflow);
        
        // Then
        assertNotNull(saved.transitions());
        assertEquals(1, saved.transitions().size());
        assertNull(saved.transitions().get(0).policyRef());
        assertNull(saved.transitions().get(0).guardExpression());
    }
}
