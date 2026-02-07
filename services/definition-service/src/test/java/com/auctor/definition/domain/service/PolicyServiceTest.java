package com.auctor.definition.domain.service;

import com.auctor.definition.domain.exception.EntityNotFoundException;
import com.auctor.definition.domain.model.*;
import com.auctor.definition.domain.port.PolicyCommandPort;
import com.auctor.definition.domain.port.PolicyQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PolicyService.
 */
@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {
    
    @Mock
    private PolicyCommandPort commandPort;
    
    @Mock
    private PolicyQueryPort queryPort;
    
    private PolicyService policyService;
    
    @BeforeEach
    void setUp() {
        policyService = new PolicyService(commandPort, queryPort);
    }
    
    @Test
    void shouldCreatePolicy() {
        // Given
        PolicyDefinition expected = createPolicy(new PolicyStatus.Draft());
        when(commandPort.save(any())).thenReturn(expected);
        
        // When
        PolicyDefinition result = policyService.create(
            "Test Policy",
            List.of(new PolicyCondition("amount", Operator.LTE, "1000"))
        );
        
        // Then
        assertNotNull(result);
        verify(commandPort).save(any());
    }
    
    @Test
    void shouldPublishDraftPolicy() {
        // Given
        PolicyDefinition draft = createPolicy(new PolicyStatus.Draft());
        PolicyDefinition published = createPolicy(new PolicyStatus.Published());
        
        when(queryPort.findById(any())).thenReturn(Optional.of(draft));
        when(commandPort.save(any())).thenReturn(published);
        
        // When
        PolicyDefinition result = policyService.publish(draft.id());
        
        // Then
        assertNotNull(result);
        verify(commandPort).save(any());
    }
    
    @Test
    void shouldThrowWhenPublishingNonDraftPolicy() {
        // Given
        PolicyDefinition published = createPolicy(new PolicyStatus.Published());
        when(queryPort.findById(any())).thenReturn(Optional.of(published));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            policyService.publish(published.id())
        );
    }
    
    @Test
    void shouldGetPolicyById() {
        // Given
        PolicyDefinition policy = createPolicy(new PolicyStatus.Draft());
        when(queryPort.findById(any())).thenReturn(Optional.of(policy));
        
        // When
        PolicyDefinition result = policyService.getById(policy.id());
        
        // Then
        assertNotNull(result);
        assertEquals(policy.id(), result.id());
    }
    
    @Test
    void shouldThrowWhenPolicyNotFound() {
        // Given
        when(queryPort.findById(any())).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(EntityNotFoundException.class, () ->
            policyService.getById(new PolicyId("missing-id"))
        );
    }
    
    @Test
    void shouldGetPolicyByIdAndVersion() {
        // Given
        PolicyDefinition policy = createPolicy(new PolicyStatus.Draft());
        when(queryPort.findByIdAndVersion(any(), anyInt())).thenReturn(Optional.of(policy));
        
        // When
        PolicyDefinition result = policyService.getByIdAndVersion(policy.id(), 1);
        
        // Then
        assertNotNull(result);
        assertEquals(policy.id(), result.id());
    }
    
    @Test
    void shouldListAllPolicies() {
        // Given
        Page<PolicyDefinition> page = new PageImpl<>(List.of(createPolicy(new PolicyStatus.Draft())));
        when(queryPort.findAll(any())).thenReturn(page);
        
        // When
        Page<PolicyDefinition> result = policyService.listAll(PageRequest.of(0, 20));
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
    
    private PolicyDefinition createPolicy(PolicyStatus status) {
        return new PolicyDefinition(
            new PolicyId("test-id"),
            "Test Policy",
            1,
            status,
            List.of(new PolicyCondition("amount", Operator.LTE, "1000")),
            Instant.now()
        );
    }
}
