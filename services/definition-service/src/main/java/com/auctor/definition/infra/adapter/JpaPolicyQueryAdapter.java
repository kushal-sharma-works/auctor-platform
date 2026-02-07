package com.auctor.definition.infra.adapter;

import com.auctor.definition.domain.model.PolicyDefinition;
import com.auctor.definition.domain.model.PolicyId;
import com.auctor.definition.domain.port.PolicyQueryPort;
import com.auctor.definition.infra.jpa.PolicyJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JPA adapter implementing policy query port.
 */
@Component
public class JpaPolicyQueryAdapter implements PolicyQueryPort {
    
    private final PolicyJpaRepository repository;
    
    public JpaPolicyQueryAdapter(PolicyJpaRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public Optional<PolicyDefinition> findById(PolicyId id) {
        return repository.findFirstByIdOrderByVersionDesc(id.value())
            .map(DomainMapper::toPolicyDomain);
    }
    
    @Override
    public Optional<PolicyDefinition> findByIdAndVersion(PolicyId id, int version) {
        return repository.findByIdAndVersion(id.value(), version)
            .map(DomainMapper::toPolicyDomain);
    }
    
    @Override
    public Page<PolicyDefinition> findAll(Pageable pageable) {
        return repository.findAllLatestVersions(pageable)
            .map(DomainMapper::toPolicyDomain);
    }
}
