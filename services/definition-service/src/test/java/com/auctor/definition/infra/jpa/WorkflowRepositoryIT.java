package com.auctor.definition.infra.jpa;

import com.auctor.definition.IntegrationTestBase;
import com.auctor.definition.infra.adapter.DomainMapper;
import com.auctor.definition.domain.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WorkflowJpaRepository.
 */
class WorkflowRepositoryIT extends IntegrationTestBase {
    
    @Autowired
    private WorkflowJpaRepository repository;
    
    @Test
    void shouldSaveAndFindWorkflow() {
        WorkflowDefinition workflow = createWorkflow("test-workflow-1");
        WorkflowJpaEntity entity = DomainMapper.toJpaEntity(workflow);
        
        WorkflowJpaEntity saved = repository.save(entity);
        
        assertNotNull(saved);
        assertEquals(workflow.id().value(), saved.getId());
    }
    
    @Test
    void shouldFindByIdAndVersion() {
        WorkflowDefinition workflow = createWorkflow("test-workflow-2");
        WorkflowJpaEntity entity = DomainMapper.toJpaEntity(workflow);
        repository.save(entity);
        
        Optional<WorkflowJpaEntity> found = repository.findByIdAndVersion(
            workflow.id().value(), workflow.version()
        );
        
        assertTrue(found.isPresent());
        assertEquals(workflow.id().value(), found.get().getId());
    }
    
    @Test
    void shouldFindAllWithPagination() {
        WorkflowDefinition workflow1 = createWorkflow("test-workflow-3");
        WorkflowDefinition workflow2 = createWorkflow("test-workflow-4");
        
        repository.save(DomainMapper.toJpaEntity(workflow1));
        repository.save(DomainMapper.toJpaEntity(workflow2));
        
        Page<WorkflowJpaEntity> page = repository.findAllLatestVersions(PageRequest.of(0, 10));
        
        assertTrue(page.getTotalElements() >= 2);
    }
    
    private WorkflowDefinition createWorkflow(String id) {
        return new WorkflowDefinition(
            new WorkflowId(id),
            "Test Workflow",
            1,
            new WorkflowStatus.Draft(),
            List.of("DRAFT", "APPROVED"),
            "DRAFT",
            List.of(new Transition("DRAFT", "APPROVED", null, null)),
            Instant.now(),
            Instant.now()
        );
    }
}
