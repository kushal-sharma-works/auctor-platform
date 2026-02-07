package com.auctor.definition.infra.adapter;

import com.auctor.definition.domain.model.WorkflowDefinition;
import com.auctor.definition.domain.model.WorkflowId;
import com.auctor.definition.domain.model.WorkflowStatus;
import com.auctor.definition.domain.port.WorkflowCommandPort;
import com.auctor.definition.infra.jpa.WorkflowJpaEntity;
import com.auctor.definition.infra.jpa.WorkflowJpaRepository;
import org.springframework.stereotype.Component;

/**
 * JPA adapter implementing workflow command port.
 */
@Component
public class JpaWorkflowCommandAdapter implements WorkflowCommandPort {
    
    private final WorkflowJpaRepository repository;
    
    public JpaWorkflowCommandAdapter(WorkflowJpaRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public WorkflowDefinition save(WorkflowDefinition workflow) {
        WorkflowJpaEntity entity = DomainMapper.toJpaEntity(workflow);
        WorkflowJpaEntity saved = repository.save(entity);
        return DomainMapper.toWorkflowDomain(saved);
    }
}
