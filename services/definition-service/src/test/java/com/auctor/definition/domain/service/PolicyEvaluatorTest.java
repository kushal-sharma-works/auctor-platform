package com.auctor.definition.domain.service;

import com.auctor.definition.domain.model.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PolicyEvaluator.
 */
class PolicyEvaluatorTest {
    
    private PolicyEvaluator evaluator;
    
    @BeforeEach
    void setUp() {
        evaluator = new PolicyEvaluator(new SimpleMeterRegistry());
    }
    
    @Test
    void shouldEvaluateEqOperator() {
        PolicyDefinition policy = createPolicy(
            new PolicyCondition("status", Operator.EQ, "ACTIVE")
        );
        
        Map<String, Object> context = Map.of("status", "ACTIVE");
        EvaluationResult result = evaluator.evaluate(policy, context);
        
        assertTrue(result.allowed());
    }
    
    @Test
    void shouldEvaluateNeqOperator() {
        PolicyDefinition policy = createPolicy(
            new PolicyCondition("status", Operator.NEQ, "INACTIVE")
        );
        
        Map<String, Object> context = Map.of("status", "ACTIVE");
        EvaluationResult result = evaluator.evaluate(policy, context);
        
        assertTrue(result.allowed());
    }
    
    @Test
    void shouldEvaluateGtOperator() {
        PolicyDefinition policy = createPolicy(
            new PolicyCondition("amount", Operator.GT, "1000")
        );
        
        Map<String, Object> context = Map.of("amount", "2000");
        EvaluationResult result = evaluator.evaluate(policy, context);
        
        assertTrue(result.allowed());
    }
    
    @Test
    void shouldEvaluateLtOperator() {
        PolicyDefinition policy = createPolicy(
            new PolicyCondition("amount", Operator.LT, "5000")
        );
        
        Map<String, Object> context = Map.of("amount", "3000");
        EvaluationResult result = evaluator.evaluate(policy, context);
        
        assertTrue(result.allowed());
    }
    
    @Test
    void shouldEvaluateGteOperator() {
        PolicyDefinition policy = createPolicy(
            new PolicyCondition("amount", Operator.GTE, "1000")
        );
        
        Map<String, Object> context = Map.of("amount", "1000");
        EvaluationResult result = evaluator.evaluate(policy, context);
        
        assertTrue(result.allowed());
    }
    
    @Test
    void shouldEvaluateLteOperator() {
        PolicyDefinition policy = createPolicy(
            new PolicyCondition("amount", Operator.LTE, "5000")
        );
        
        Map<String, Object> context = Map.of("amount", "5000");
        EvaluationResult result = evaluator.evaluate(policy, context);
        
        assertTrue(result.allowed());
    }
    
    @Test
    void shouldEvaluateInOperator() {
        PolicyDefinition policy = createPolicy(
            new PolicyCondition("country", Operator.IN, "DE,FR,NL")
        );
        
        Map<String, Object> context = Map.of("country", "FR");
        EvaluationResult result = evaluator.evaluate(policy, context);
        
        assertTrue(result.allowed());
    }
    
    @Test
    void shouldEvaluateNotInOperator() {
        PolicyDefinition policy = createPolicy(
            new PolicyCondition("country", Operator.NOT_IN, "US,CA")
        );
        
        Map<String, Object> context = Map.of("country", "FR");
        EvaluationResult result = evaluator.evaluate(policy, context);
        
        assertTrue(result.allowed());
    }
    
    @Test
    void shouldFailWhenContextFieldMissing() {
        PolicyDefinition policy = createPolicy(
            new PolicyCondition("amount", Operator.LTE, "1000")
        );
        
        Map<String, Object> context = Map.of("other_field", "value");
        EvaluationResult result = evaluator.evaluate(policy, context);
        
        assertFalse(result.allowed());
        assertTrue(result.explanation().contains("not found in context"));
    }
    
    @Test
    void shouldApplyAndLogic() {
        PolicyDefinition policy = createPolicyWithConditions(
            new PolicyCondition("amount", Operator.LTE, "10000"),
            new PolicyCondition("country", Operator.IN, "DE,FR,NL")
        );
        
        // Both conditions pass
        Map<String, Object> context1 = Map.of("amount", "5000", "country", "DE");
        EvaluationResult result1 = evaluator.evaluate(policy, context1);
        assertTrue(result1.allowed());
        
        // First condition fails
        Map<String, Object> context2 = Map.of("amount", "15000", "country", "DE");
        EvaluationResult result2 = evaluator.evaluate(policy, context2);
        assertFalse(result2.allowed());
        
        // Second condition fails
        Map<String, Object> context3 = Map.of("amount", "5000", "country", "US");
        EvaluationResult result3 = evaluator.evaluate(policy, context3);
        assertFalse(result3.allowed());
    }
    
    private PolicyDefinition createPolicy(PolicyCondition condition) {
        return new PolicyDefinition(
            new PolicyId("test-policy"),
            "Test Policy",
            1,
            new PolicyStatus.Draft(),
            List.of(condition),
            Instant.now()
        );
    }
    
    private PolicyDefinition createPolicyWithConditions(PolicyCondition... conditions) {
        return new PolicyDefinition(
            new PolicyId("test-policy"),
            "Test Policy",
            1,
            new PolicyStatus.Draft(),
            List.of(conditions),
            Instant.now()
        );
    }
}
