package com.auctor.definition.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PolicyId value object.
 */
class PolicyIdTest {
    
    @Test
    void shouldCreatePolicyIdWithValidValue() {
        // When
        PolicyId policyId = new PolicyId("test-policy-123");
        
        // Then
        assertNotNull(policyId);
        assertEquals("test-policy-123", policyId.value());
    }
    
    @Test
    void shouldRejectNullValue() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new PolicyId(null)
        );
        
        assertEquals("PolicyId value must not be blank", exception.getMessage());
    }
    
    @Test
    void shouldRejectBlankValue() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> new PolicyId(""));
        assertThrows(IllegalArgumentException.class, () -> new PolicyId("   "));
        assertThrows(IllegalArgumentException.class, () -> new PolicyId("\t"));
    }
    
    @Test
    void shouldHandleUuidFormat() {
        // Given
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        
        // When
        PolicyId policyId = new PolicyId(uuid);
        
        // Then
        assertEquals(uuid, policyId.value());
    }
    
    @Test
    void shouldHandleCustomIdFormat() {
        // Given
        String customId = "policy-2024-001";
        
        // When
        PolicyId policyId = new PolicyId(customId);
        
        // Then
        assertEquals(customId, policyId.value());
    }
    
    @Test
    void shouldImplementRecordEquality() {
        // Given
        PolicyId id1 = new PolicyId("test-id");
        PolicyId id2 = new PolicyId("test-id");
        PolicyId id3 = new PolicyId("different-id");
        
        // Then
        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertEquals(id1.hashCode(), id2.hashCode());
    }
    
    @Test
    void shouldImplementToString() {
        // Given
        PolicyId policyId = new PolicyId("test-id");
        
        // When
        String toString = policyId.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("test-id"));
    }
    
    @Test
    void shouldHandleSpecialCharacters() {
        // Given
        String idWithSpecialChars = "policy-id_123.v2";
        
        // When
        PolicyId policyId = new PolicyId(idWithSpecialChars);
        
        // Then
        assertEquals(idWithSpecialChars, policyId.value());
    }
    
    @Test
    void shouldTrimNotApply() {
        // Given - spaces are part of validation, not trimmed
        String id = " id-with-spaces ";
        
        // When
        PolicyId policyId = new PolicyId(id);
        
        // Then
        assertEquals(id, policyId.value());
    }
}
