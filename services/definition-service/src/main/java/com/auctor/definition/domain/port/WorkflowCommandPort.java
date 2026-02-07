package com.auctor.definition.domain.port;

import com.auctor.definition.domain.model.WorkflowDefinition;
import com.auctor.definition.domain.model.WorkflowId;

/**
 * Port for workflow command operations (writes).
 */
public interface WorkflowCommandPort {
    
    /**
     * Save a workflow definition.
     */
    WorkflowDefinition save(WorkflowDefinition workflow);
    
    /**
     * Publish a workflow definition.
     */
    WorkflowDefinition publish(WorkflowId id);
}
