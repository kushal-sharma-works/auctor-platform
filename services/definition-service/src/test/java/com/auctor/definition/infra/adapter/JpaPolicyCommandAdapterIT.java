package com.auctor.definition.infra.adapter;

import com.auctor.definition.IntegrationTestBase;
import com.auctor.definition.domain.model.*;
import com.auctor.definition.infra.jpa.PolicyJpaEntity;
import com.auctor.definition.infra.jpa.PolicyJpaRepository;
import com.auctor.definition.infra.jpa.PolicyDefinitionId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JpaPolicyCommandAdapterIT extends IntegrationTestBase {
    
    @Autowired
    private JpaPolicyCommandAdapter adapter;
    
    @Autowired
    private PolicyJpaRepository repository;
    
    @Test
    void shouldSaveNewPolicy() {
        // Given
        PolicyDefinition policy = new PolicyDefinition(
            new PolicyId("new-policy"),
            "New Policy",
            1,
            new PolicyStatus.Draft(),
            List.of(new PolicyCondition("field", Operator.EQ, "value")),
            Instant.now()
        );
        
        // When
        PolicyDefinition saved = adapter.save(policy);
        
        // Then
        assertNotNull(saved);
        assertEquals(policy.id(), saved.id());
        assertEquals(policy.name(), saved.name());
        
        // Verify saved in repository
        Optional<PolicyJpaEntity> entity = repository.findById(new PolicyDefinitionId("new-policy", 1));
        assertTrue(entity.isPresent());
        assertEquals("New Policy", entity.get().getName());
    }
    
    @Test
    void shouldUpdateExistingPolicy() {
        // Given - save initial version
        PolicyDefinition initial = new PolicyDefinition(
            new PolicyId("update-policy"),
            "Initial Name",
            1,
            new PolicyStatus.Draft(),
            List.of(new PolicyCondition("field", Operator.EQ, "value")),
            Instant.now()
        );
        adapter.save(initial);
        
        // When - save updated version
        PolicyDefinition updated = new PolicyDefinition(
            initial.id(),
            "Updated Name",
            2,
            new PolicyStatus.Published(),
            initial.conditions(),
            initial.createdAt()
        );
        PolicyDefinition saved = adapter.save(updated);
        
        // Then
        assertEquals("Updated Name", saved.name());
        assertEquals(2, saved.version());
        assertInstanceOf(PolicyStatus.Published.class, saved.status());
    }
    
    @Test
    void shouldPreserveAllFields() {
        // Given
        Instant createdAt = Instant.now().minus(1, ChronoUnit.DAYS);
        List<PolicyCondition> conditions = List.of(
            new PolicyCondition("amount", Operator.GTE, "100"),
            new PolicyCondition("status", Operator.IN, "ACTIVE,PENDING")
        );
        PolicyDefinition policy = new PolicyDefinition(
            new PolicyId("complete-policy"),
            "Complete Policy",
            1,
            new PolicyStatus.Draft(),
            conditions,
            createdAt
        );
        
        // When
        PolicyDefinition saved = adapter.save(policy);
        
        // Then
        assertEquals(2, saved.conditions().size());
        assertEquals("amount", saved.conditions().get(0).field());
        assertEquals(Operator.GTE, saved.conditions().get(0).operator());
        assertEquals(createdAt.truncatedTo(ChronoUnit.SECONDS), saved.createdAt().truncatedTo(ChronoUnit.SECONDS));
    }
    
    @Test
    void shouldHandleComplexConditions() {
        // Given
        List<PolicyCondition> conditions = List.of(
            new PolicyCondition("user.role", Operator.EQ, "ADMIN"),
            new PolicyCondition("account.balance", Operator.GT, "1000.50"),
            new PolicyCondition("tags", Operator.IN, "VIP,PREMIUM"),
            new PolicyCondition("status", Operator.NEQ, "SUSPENDED")
        );
        PolicyDefinition policy = new PolicyDefinition(
            new PolicyId("complex-policy"),
            "Complex Policy",
            1,
            new PolicyStatus.Draft(),
            conditions,
            Instant.now()
        );
        
        // When
        PolicyDefinition saved = adapter.save(policy);
        
        // Then
        assertEquals(4, saved.conditions().size());
        assertEquals("user.role", saved.conditions().get(0).field());
        assertEquals(Operator.EQ, saved.conditions().get(0).operator());
    }
    
    @Test
    void shouldSaveMultipleVersions() {
        // Given
        PolicyId id = new PolicyId("versioned-policy");
        
        // When - save multiple versions
        adapter.save(new PolicyDefinition(id, "V1", 1, new PolicyStatus.Draft(),
            List.of(new PolicyCondition("f", Operator.EQ, "v")), Instant.now()));
        adapter.save(new PolicyDefinition(id, "V2", 2, new PolicyStatus.Published(),
            List.of(new PolicyCondition("f", Operator.EQ, "v")), Instant.now()));
        adapter.save(new PolicyDefinition(id, "V3", 3, new PolicyStatus.Draft(),
            List.of(new PolicyCondition("f", Operator.EQ, "v")), Instant.now()));
        
        // Then - all versions should exist
        Optional<PolicyJpaEntity> v1 = repository.findByIdAndVersion(id.value(), 1);
        Optional<PolicyJpaEntity> v2 = repository.findByIdAndVersion(id.value(), 2);
        Optional<PolicyJpaEntity> v3 = repository.findByIdAndVersion(id.value(), 3);
        
        assertTrue(v1.isPresent());
        assertTrue(v2.isPresent());
        assertTrue(v3.isPresent());
        assertEquals("V1", v1.get().getName());
        assertEquals("V2", v2.get().getName());
        assertEquals("V3", v3.get().getName());
    }
    
    @Test
    void shouldRoundTripSuccessfully() {
        // Given
        PolicyDefinition original = new PolicyDefinition(
            new PolicyId("roundtrip-policy"),
            "Round Trip Test",
            1,
            new PolicyStatus.Published(),
            List.of(
                new PolicyCondition("field1", Operator.LTE, "100"),
                new PolicyCondition("field2", Operator.NEQ, "BLOCKED")
            ),
            Instant.now()
        );
        
        // When - save and retrieve
        adapter.save(original);
        Optional<PolicyJpaEntity> entity = repository.findById(new PolicyDefinitionId("roundtrip-policy", 1));
        PolicyDefinition retrieved = DomainMapper.toPolicyDomain(entity.get());
        
        // Then - should match
        assertEquals(original.id(), retrieved.id());
        assertEquals(original.name(), retrieved.name());
        assertEquals(original.version(), retrieved.version());
        assertEquals(original.conditions().size(), retrieved.conditions().size());
        assertEquals(original.status().label(), retrieved.status().label());
    }
}
