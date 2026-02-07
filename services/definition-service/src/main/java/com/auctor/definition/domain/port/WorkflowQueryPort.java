package com.auctor.definition.domain.port;

import com.auctor.definition.domain.model.WorkflowDefinition;
import com.auctor.definition.domain.model.WorkflowId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Port for workflow query operations (reads).
 */
public interface WorkflowQueryPort {
    
    /**
     * Find a workflow by its ID (returns latest version).
     */
    Optional<WorkflowDefinition> findById(WorkflowId id);
    
    /**
     * Find a workflow by ID and specific version.
     */
    Optional<WorkflowDefinition> findByIdAndVersion(WorkflowId id, int version);
    
    /**
     * Find all workflows with pagination.
     */
    Page<WorkflowDefinition> findAll(Pageable pageable);
    
    /**
     * Find the latest published version of a workflow.
     */
    Optional<WorkflowDefinition> findLatestPublished(WorkflowId id);
}
