package com.auctor.definition.infra.adapter;

import com.auctor.definition.domain.model.WorkflowDefinition;
import com.auctor.definition.domain.model.WorkflowId;
import com.auctor.definition.domain.port.WorkflowQueryPort;
import com.auctor.definition.infra.jpa.WorkflowJpaEntity;
import com.auctor.definition.infra.jpa.WorkflowJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JPA adapter implementing workflow query port.
 */
@Component
public class JpaWorkflowQueryAdapter implements WorkflowQueryPort {
    
    private final WorkflowJpaRepository repository;
    
    public JpaWorkflowQueryAdapter(WorkflowJpaRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public Optional<WorkflowDefinition> findById(WorkflowId id) {
        return repository.findFirstByIdOrderByVersionDesc(id.value())
            .map(DomainMapper::toWorkflowDomain);
    }
    
    @Override
    public Optional<WorkflowDefinition> findByIdAndVersion(WorkflowId id, int version) {
        return repository.findByIdAndVersion(id.value(), version)
            .map(DomainMapper::toWorkflowDomain);
    }
    
    @Override
    public Page<WorkflowDefinition> findAll(Pageable pageable) {
        return repository.findAllLatestVersions(pageable)
            .map(DomainMapper::toWorkflowDomain);
    }
    
    @Override
    public Optional<WorkflowDefinition> findLatestPublished(WorkflowId id) {
        return repository.findFirstByIdAndStatusOrderByVersionDesc(id.value(), "PUBLISHED")
            .map(DomainMapper::toWorkflowDomain);
    }
}
