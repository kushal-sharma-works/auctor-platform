package com.auctor.definition.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PolicyDefinition domain model.
 */
class PolicyDefinitionTest {
    
    @Test
    void shouldCreateValidPolicyDefinition() {
        // Given
        PolicyId id = new PolicyId("test-id");
        List<PolicyCondition> conditions = List.of(
            new PolicyCondition("amount", Operator.LTE, "1000")
        );
        
        // When
        PolicyDefinition policy = new PolicyDefinition(
            id, "Test Policy", 1, new PolicyStatus.Draft(),
            conditions, Instant.now()
        );
        
        // Then
        assertNotNull(policy);
        assertEquals("test-id", policy.id().value());
        assertEquals("Test Policy", policy.name());
        assertEquals(1, policy.conditions().size());
    }
    
    @Test
    void shouldRejectBlankName() {
        assertThrows(IllegalArgumentException.class, () ->
            new PolicyDefinition(
                new PolicyId("id"), "", 1, new PolicyStatus.Draft(),
                List.of(), Instant.now()
            )
        );
    }
    
    @Test
    void shouldValidateConditions() {
        PolicyCondition condition = new PolicyCondition("field", Operator.EQ, "value");
        assertNotNull(condition);
        assertEquals("field", condition.field());
        assertEquals(Operator.EQ, condition.operator());
    }
    
    @Test
    void shouldRejectNullConditionField() {
        assertThrows(IllegalArgumentException.class, () ->
            new PolicyCondition(null, Operator.EQ, "value")
        );
    }
}
