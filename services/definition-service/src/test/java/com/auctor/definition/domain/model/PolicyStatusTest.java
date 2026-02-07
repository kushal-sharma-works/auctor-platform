package com.auctor.definition.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enhanced unit tests for PolicyStatus sealed hierarchy.
 */
class PolicyStatusTest {
    
    @Test
    void shouldCreateDraftStatus() {
        // When
        PolicyStatus status = new PolicyStatus.Draft();
        
       // Then
        assertNotNull(status);
        assertEquals("DRAFT", status.label());
    }
    
    @Test
    void shouldCreatePublishedStatus() {
        // When
        PolicyStatus status = new PolicyStatus.Published();
        
        // Then
        assertNotNull(status);
        assertEquals("PUBLISHED", status.label());
    }
    
    @Test
    void shouldImplementRecordEqualityForDraft() {
        // Given
        PolicyStatus draft1 = new PolicyStatus.Draft();
        PolicyStatus draft2 = new PolicyStatus.Draft();
        
        // Then
        assertEquals(draft1, draft2);
        assertEquals(draft1.hashCode(), draft2.hashCode());
    }
    
    @Test
    void shouldImplementRecordEqualityForPublished() {
        // Given
        PolicyStatus published1 = new PolicyStatus.Published();
        PolicyStatus published2 = new PolicyStatus.Published();
        
        // Then
        assertEquals(published1, published2);
        assertEquals(published1.hashCode(), published2.hashCode());
    }
    
    @Test
    void shouldNotBeEqualAcrossStatuses() {
        // Given
        PolicyStatus draft = new PolicyStatus.Draft();
        PolicyStatus published = new PolicyStatus.Published();
        
        // Then
        assertNotEquals(draft, published);
    }
    
    @Test
    void shouldSupportPatternMatching() {
        // Given
        PolicyStatus status = new PolicyStatus.Draft();
        
        // When
        String result = switch (status) {
            case PolicyStatus.Draft d -> "draft";
            case PolicyStatus.Published p -> "published";
        };
        
        // Then
        assertEquals("draft", result);
    }
    
    @Test
    void shouldSupportInstanceOfChecks() {
        // Given
        PolicyStatus status = new PolicyStatus.Published();
        
        // Then
        assertTrue(status instanceof PolicyStatus.Published);
        assertFalse(status instanceof PolicyStatus.Draft);
    }
    
    @Test
    void shouldHaveCorrectLabels() {
        // Given
        PolicyStatus draft = new PolicyStatus.Draft();
        PolicyStatus published = new PolicyStatus.Published();
        
        // Then
        assertEquals("DRAFT", draft.label());
        assertEquals("PUBLISHED", published.label());
    }
    
    @Test
    void shouldBeSealedHierarchy() {
        // Given - PolicyStatus is sealed, can only be Draft or Published
        PolicyStatus status = new PolicyStatus.Draft();
        
        // Then - should compile with exhaustive switch (no default needed)
        String label = switch (status) {
            case PolicyStatus.Draft d -> d.label();
            case PolicyStatus.Published p -> p.label();
        };
        
        assertNotNull(label);
    }
    
    @Test
    void shouldCreateStatusFromLabel() {
        // When
        PolicyStatus draft = PolicyStatus.fromLabel("DRAFT");
        PolicyStatus published = PolicyStatus.fromLabel("PUBLISHED");
        
        // Then
        assertTrue(draft instanceof PolicyStatus.Draft);
        assertTrue(published instanceof PolicyStatus.Published);
    }
    
    @Test
    void shouldThrowExceptionForInvalidLabel() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PolicyStatus.fromLabel("INVALID")
        );
        
        assertTrue(exception.getMessage().contains("Unknown policy status"));
    }
}
