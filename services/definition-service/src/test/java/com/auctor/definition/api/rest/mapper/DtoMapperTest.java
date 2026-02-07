package com.auctor.definition.api.rest.mapper;

import com.auctor.definition.api.rest.dto.*;
import com.auctor.definition.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DtoMapper.
 */
class DtoMapperTest {
    
    @Test
    void shouldMapWorkflowDefinitionToResponse() {
        // Given
        WorkflowId id = new WorkflowId("test-id");
        List<String> states = List.of("START", "END");
        List<Transition> transitions = List.of(
            new Transition("START", "END", "policy-ref", "guard-expr")
        );
        Instant now = Instant.now();
        
        WorkflowDefinition workflow = new WorkflowDefinition(
            id, "Test Workflow", 1, new WorkflowStatus.Draft(),
            states, "START", transitions, now, now
        );
        
        // When
        WorkflowResponse response = DtoMapper.toResponse(workflow);
        
        // Then
        assertNotNull(response);
        assertEquals("test-id", response.id());
        assertEquals("Test Workflow", response.name());
        assertEquals(1, response.version());
        assertEquals("DRAFT", response.status());
        assertEquals(2, response.states().size());
        assertEquals("START", response.initialState());
        assertEquals(1, response.transitions().size());
        
        TransitionResponse transition = response.transitions().get(0);
        assertEquals("START", transition.fromState());
        assertEquals("END", transition.toState());
        assertEquals("policy-ref", transition.policyRef());
        assertEquals("guard-expr", transition.guardExpression());
    }
    
    @Test
    void shouldMapPolicyDefinitionToResponse() {
        // Given
        PolicyId id = new PolicyId("policy-id");
        List<PolicyCondition> conditions = List.of(
            new PolicyCondition("amount", Operator.GT, "1000"),
            new PolicyCondition("status", Operator.EQ, "ACTIVE")
        );
        Instant now = Instant.now();
        
        PolicyDefinition policy = new PolicyDefinition(
            id, "Test Policy", 2, new PolicyStatus.Published(),
            conditions, now
        );
        
        // When
        PolicyResponse response = DtoMapper.toResponse(policy);
        
        // Then
        assertNotNull(response);
        assertEquals("policy-id", response.id());
        assertEquals("Test Policy", response.name());
        assertEquals(2, response.version());
        assertEquals("PUBLISHED", response.status());
        assertEquals(2, response.conditions().size());
        
        PolicyConditionResponse condition1 = response.conditions().get(0);
        assertEquals("amount", condition1.field());
        assertEquals("GT", condition1.operator());
        assertEquals("1000", condition1.value());
    }
    
    @Test
    void shouldMapTransitionRequestToDomain() {
        // Given
        TransitionRequest request = new TransitionRequest(
            "STATE_A", "STATE_B", "policy-123", "x > 5"
        );
        
        // When
        Transition transition = DtoMapper.toDomain(request);
        
        // Then
        assertNotNull(transition);
        assertEquals("STATE_A", transition.fromState());
        assertEquals("STATE_B", transition.toState());
        assertEquals("policy-123", transition.policyRef());
        assertEquals("x > 5", transition.guardExpression());
    }
    
    @Test
    void shouldMapTransitionRequestWithNullOptionalFields() {
        // Given
        TransitionRequest request = new TransitionRequest(
            "STATE_A", "STATE_B", null, null
        );
        
        // When
        Transition transition = DtoMapper.toDomain(request);
        
        // Then
        assertNotNull(transition);
        assertEquals("STATE_A", transition.fromState());
        assertEquals("STATE_B", transition.toState());
        assertNull(transition.policyRef());
        assertNull(transition.guardExpression());
    }
    
    @Test
    void shouldMapPolicyConditionRequestToDomain() {
        // Given
        PolicyConditionRequest request = new PolicyConditionRequest(
            "field", "LTE", "100"
        );
        
        // When
        PolicyCondition condition = DtoMapper.toDomain(request);
        
        // Then
        assertNotNull(condition);
        assertEquals("field", condition.field());
        assertEquals(Operator.LTE, condition.operator());
        assertEquals("100", condition.value());
    }
    
    @Test
    void shouldHandleAllOperatorTypes() {
        for (Operator operator : Operator.values()) {
            PolicyConditionRequest request = new PolicyConditionRequest(
                "test", operator.name(), "value"
            );
            
            PolicyCondition condition = DtoMapper.toDomain(request);
            
            assertEquals(operator, condition.operator());
        }
    }
    
    @Test
    void shouldThrowExceptionForInvalidOperator() {
        PolicyConditionRequest request = new PolicyConditionRequest(
            "field", "INVALID_OPERATOR", "value"
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            DtoMapper.toDomain(request)
        );
    }
    
    @Test
    void shouldHandleEmptyCollections() {
        // Given
        WorkflowDefinition workflow = new WorkflowDefinition(
            new WorkflowId("id"), "Empty Workflow", 1, new WorkflowStatus.Draft(),
            List.of("START"), "START", List.of(), Instant.now(), Instant.now()
        );
        
        // When
        WorkflowResponse response = DtoMapper.toResponse(workflow);
        
        // Then
        assertNotNull(response);
        assertTrue(response.transitions().isEmpty());
    }
    
    @Test
    void shouldPreserveTimestamps() {
        // Given
        Instant createdAt = Instant.parse("2024-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2024-01-02T15:30:00Z");
        
        WorkflowDefinition workflow = new WorkflowDefinition(
            new WorkflowId("id"), "Test", 1, new WorkflowStatus.Draft(),
            List.of("START"), "START", List.of(), createdAt, updatedAt
        );
        
        // When
        WorkflowResponse response = DtoMapper.toResponse(workflow);
        
        // Then
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }
}
