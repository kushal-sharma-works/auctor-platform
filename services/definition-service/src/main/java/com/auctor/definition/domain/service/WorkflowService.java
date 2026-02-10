package com.auctor.definition.domain.service;

import com.auctor.definition.domain.exception.EntityNotFoundException;
import com.auctor.definition.domain.model.*;
import com.auctor.definition.domain.port.WorkflowCommandPort;
import com.auctor.definition.domain.port.WorkflowQueryPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain service for workflow operations.
 * Contains business logic for workflow lifecycle.
 */
public class WorkflowService {
    
    private final WorkflowCommandPort commandPort;
    private final WorkflowQueryPort queryPort;
    private final Counter workflowCreatedCounter;
    private final Counter workflowPublishedCounter;
    
    public WorkflowService(
        WorkflowCommandPort commandPort,
        WorkflowQueryPort queryPort,
        MeterRegistry meterRegistry
    ) {
        this.commandPort = commandPort;
        this.queryPort = queryPort;
        this.workflowCreatedCounter = meterRegistry.counter("workflow.created.total");
        this.workflowPublishedCounter = meterRegistry.counter("workflow.published.total");
    }
    
    /**
     * Create a new workflow definition.
     */
    public WorkflowDefinition create(
        String name,
        List<String> states,
        String initialState,
        List<Transition> transitions
    ) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        
        WorkflowDefinition workflow = new WorkflowDefinition(
            new WorkflowId(id),
            name,
            1,
            new WorkflowStatus.Draft(),
            states,
            initialState,
            transitions,
            now,
            now
        );
        
        WorkflowDefinition saved = commandPort.save(workflow);
        workflowCreatedCounter.increment();
        return saved;
    }
    
    /**
     * Publish a workflow definition.
     * Validates that the workflow is in DRAFT status and has at least one transition.
     */
    public WorkflowDefinition publish(WorkflowId id) {
        WorkflowDefinition workflow = queryPort.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Workflow", id.value()));
        
        // Validate workflow is in DRAFT status
        if (!(workflow.status() instanceof WorkflowStatus.Draft)) {
            throw new IllegalArgumentException("Workflow must be in DRAFT status to publish");
        }
        
        // Validate workflow has at least one transition
        if (workflow.transitions().isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one transition to publish");
        }
        
        // Update status to PUBLISHED and save as a new version
        WorkflowDefinition publishedWorkflow = workflow
            .withStatus(new WorkflowStatus.Published())
            .withVersion(workflow.version() + 1);
        WorkflowDefinition saved = commandPort.save(publishedWorkflow);
        workflowPublishedCounter.increment();
        return saved;
    }
    
    /**
     * Get workflow by ID (latest version).
     */
    public WorkflowDefinition getById(WorkflowId id) {
        return queryPort.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Workflow", id.value()));
    }
    
    /**
     * Get workflow by ID and specific version.
     */
    public WorkflowDefinition getByIdAndVersion(WorkflowId id, int version) {
        return queryPort.findByIdAndVersion(id, version)
            .orElseThrow(() -> new EntityNotFoundException(
                "Workflow not found with id: " + id.value() + " and version: " + version
            ));
    }
    
    /**
     * List all workflows with pagination.
     */
    public Page<WorkflowDefinition> listAll(Pageable pageable) {
        return queryPort.findAll(pageable);
    }
}
