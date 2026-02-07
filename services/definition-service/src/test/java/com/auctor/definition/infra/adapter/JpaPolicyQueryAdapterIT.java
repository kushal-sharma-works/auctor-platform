package com.auctor.definition.infra.adapter;

import com.auctor.definition.IntegrationTestBase;
import com.auctor.definition.domain.model.*;
import com.auctor.definition.infra.jpa.PolicyJpaEntity;
import com.auctor.definition.infra.jpa.PolicyJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JpaPolicyQueryAdapterIT extends IntegrationTestBase {
    
    @Autowired
    private JpaPolicyQueryAdapter adapter;
    
    @Autowired
    private PolicyJpaRepository repository;
    
    private PolicyId testPolicyId;
    
    @BeforeEach
    void setup() {
        testPolicyId = new PolicyId("test-policy-id");
    }
    
    @Test
    void shouldFindById() {
        // Given
        PolicyJpaEntity entity = createPolicyEntity(testPolicyId.value(), 1, "DRAFT");
        repository.save(entity);
        
        // When
        Optional<PolicyDefinition> result = adapter.findById(testPolicyId);
        
        // Then
        assertTrue(result.isPresent());
        PolicyDefinition policy = result.get();
        assertEquals(testPolicyId.value(), policy.id().value());
        assertEquals("Test Policy", policy.name());
        assertEquals(1, policy.version());
        assertInstanceOf(PolicyStatus.Draft.class, policy.status());
    }
    
    @Test
    void shouldReturnEmptyWhenPolicyNotFound() {
        // When
        Optional<PolicyDefinition> result = adapter.findById(new PolicyId("non-existent"));
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    void shouldFindLatestVersion() {
        // Given - create multiple versions
        repository.save(createPolicyEntity(testPolicyId.value(), 1, "DRAFT"));
        repository.save(createPolicyEntity(testPolicyId.value(), 2, "PUBLISHED"));
        repository.save(createPolicyEntity(testPolicyId.value(), 3, "DRAFT"));
        
        // When
        Optional<PolicyDefinition> result = adapter.findById(testPolicyId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(3, result.get().version());
    }
    
    @Test
    void shouldFindByIdAndVersion() {
        // Given
        repository.save(createPolicyEntity(testPolicyId.value(), 1, "PUBLISHED"));
        repository.save(createPolicyEntity(testPolicyId.value(), 2, "DRAFT"));
        
        // When
        Optional<PolicyDefinition> result = adapter.findByIdAndVersion(testPolicyId, 1);
        
        // Then
        assertTrue(result.isPresent());
        PolicyDefinition policy = result.get();
        assertEquals(1, policy.version());
        assertInstanceOf(PolicyStatus.Published.class, policy.status());
    }
    
    @Test
    void shouldReturnEmptyWhenVersionNotFound() {
        // Given
        repository.save(createPolicyEntity(testPolicyId.value(), 1, "DRAFT"));
        
        // When
        Optional<PolicyDefinition> result = adapter.findByIdAndVersion(testPolicyId, 99);
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    void shouldFindAllLatestVersions() {
        // Given - create multiple policies with versions
        repository.save(createPolicyEntity("policy-1", 1, "DRAFT"));
        repository.save(createPolicyEntity("policy-1", 2, "PUBLISHED"));
        repository.save(createPolicyEntity("policy-2", 1, "DRAFT"));
        repository.save(createPolicyEntity("policy-3", 1, "PUBLISHED"));
        
        // When
        Page<PolicyDefinition> result = adapter.findAll(PageRequest.of(0, 10));
        
        // Then
        assertEquals(3, result.getTotalElements());
        assertTrue(result.getContent().stream()
            .anyMatch(p -> p.id().value().equals("policy-1") && p.version() == 2));
    }
    
    @Test
    void shouldHandlePagination() {
        // Given
        for (int i = 1; i <= 15; i++) {
            repository.save(createPolicyEntity("policy-" + i, 1, "DRAFT"));
        }
        
        // When
        Page<PolicyDefinition> page1 = adapter.findAll(PageRequest.of(0, 5));
        Page<PolicyDefinition> page2 = adapter.findAll(PageRequest.of(1, 5));
        
        // Then
        assertEquals(5, page1.getSize());
        assertEquals(15, page1.getTotalElements());
        assertEquals(3, page1.getTotalPages());
        assertNotEquals(page1.getContent().get(0).id(), page2.getContent().get(0).id());
    }
    
    @Test
    void shouldMapConditionsCorrectly() {
        // Given
        PolicyJpaEntity entity = createPolicyEntity(testPolicyId.value(), 1, "DRAFT");
        repository.save(entity);
        
        // When
        Optional<PolicyDefinition> result = adapter.findById(testPolicyId);
        
        // Then
        assertTrue(result.isPresent());
        List<PolicyCondition> conditions = result.get().conditions();
        assertEquals(2, conditions.size());
        assertEquals("amount", conditions.get(0).field());
        assertEquals(Operator.LTE, conditions.get(0).operator());
        assertEquals("1000", conditions.get(0).value());
    }
    
    @Test
    void shouldPreserveTimestamps() {
        // Given
        Instant now = Instant.now();
        PolicyJpaEntity entity = createPolicyEntity(testPolicyId.value(), 1, "DRAFT");
        entity.setCreatedAt(now);
        repository.save(entity);
        
        // When
        Optional<PolicyDefinition> result = adapter.findById(testPolicyId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(now.truncatedTo(ChronoUnit.SECONDS), result.get().createdAt().truncatedTo(ChronoUnit.SECONDS));
    }
    
    // Helper methods
    
    private PolicyJpaEntity createPolicyEntity(String id, int version, String status) {
        PolicyJpaEntity entity = new PolicyJpaEntity();
        entity.setId(id);
        entity.setVersion(version);
        entity.setName("Test Policy");
        entity.setStatus(status);
        entity.setConditions(List.of(
            new com.auctor.definition.infra.jpa.PolicyConditionDto("amount", "LTE", "1000"),
            new com.auctor.definition.infra.jpa.PolicyConditionDto("status", "EQ", "ACTIVE")
        ));
        entity.setCreatedAt(Instant.now());
        return entity;
    }
}
