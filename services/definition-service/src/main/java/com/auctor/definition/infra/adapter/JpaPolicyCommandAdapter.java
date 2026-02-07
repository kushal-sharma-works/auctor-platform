package com.auctor.definition.infra.adapter;

import com.auctor.definition.domain.model.PolicyDefinition;
import com.auctor.definition.domain.model.PolicyId;
import com.auctor.definition.domain.port.PolicyCommandPort;
import com.auctor.definition.infra.jpa.PolicyJpaEntity;
import com.auctor.definition.infra.jpa.PolicyJpaRepository;
import org.springframework.stereotype.Component;

/**
 * JPA adapter implementing policy command port.
 */
@Component
public class JpaPolicyCommandAdapter implements PolicyCommandPort {
    
    private final PolicyJpaRepository repository;
    
    public JpaPolicyCommandAdapter(PolicyJpaRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public PolicyDefinition save(PolicyDefinition policy) {
        PolicyJpaEntity entity = DomainMapper.toJpaEntity(policy);
        PolicyJpaEntity saved = repository.save(entity);
        return DomainMapper.toPolicyDomain(saved);
    }
}
