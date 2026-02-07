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
 * Integration tests for PolicyJpaRepository.
 */
class PolicyRepositoryIT extends IntegrationTestBase {
    
    @Autowired
    private PolicyJpaRepository repository;
    
    @Test
    void shouldSaveAndFindPolicy() {
        PolicyDefinition policy = createPolicy("test-policy-1");
        PolicyJpaEntity entity = DomainMapper.toJpaEntity(policy);
        
        PolicyJpaEntity saved = repository.save(entity);
        
        assertNotNull(saved);
        assertEquals(policy.id().value(), saved.getId());
    }
    
    @Test
    void shouldFindByIdAndVersion() {
        PolicyDefinition policy = createPolicy("test-policy-2");
        PolicyJpaEntity entity = DomainMapper.toJpaEntity(policy);
        repository.save(entity);
        
        Optional<PolicyJpaEntity> found = repository.findByIdAndVersion(
            policy.id().value(), policy.version()
        );
        
        assertTrue(found.isPresent());
        assertEquals(policy.id().value(), found.get().getId());
    }
    
    @Test
    void shouldFindAllWithPagination() {
        PolicyDefinition policy1 = createPolicy("test-policy-3");
        PolicyDefinition policy2 = createPolicy("test-policy-4");
        
        repository.save(DomainMapper.toJpaEntity(policy1));
        repository.save(DomainMapper.toJpaEntity(policy2));
        
        Page<PolicyJpaEntity> page = repository.findAllLatestVersions(PageRequest.of(0, 10));
        
        assertTrue(page.getTotalElements() >= 2);
    }
    
    private PolicyDefinition createPolicy(String id) {
        return new PolicyDefinition(
            new PolicyId(id),
            "Test Policy",
            1,
            new PolicyStatus.Draft(),
            List.of(new PolicyCondition("amount", Operator.LTE, "1000")),
            Instant.now()
        );
    }
}
