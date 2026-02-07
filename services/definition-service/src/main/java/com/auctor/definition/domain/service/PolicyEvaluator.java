package com.auctor.definition.domain.service;

import com.auctor.definition.domain.model.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for evaluating policy conditions against a context.
 * Uses pattern matching for operator evaluation.
 */
public class PolicyEvaluator {
    
    /**
     * Evaluate a policy definition against a context.
     * All conditions must pass (AND logic).
     */
    public EvaluationResult evaluate(PolicyDefinition policy, Map<String, Object> context) {
        StringBuilder explanation = new StringBuilder();
        
        for (PolicyCondition condition : policy.conditions()) {
            boolean conditionPassed = evaluateCondition(condition, context, explanation);
            
            if (!conditionPassed) {
                return new EvaluationResult(false, explanation.toString());
            }
        }
        
        explanation.append("All conditions passed");
        return new EvaluationResult(true, explanation.toString());
    }
    
    private boolean evaluateCondition(
        PolicyCondition condition,
        Map<String, Object> context,
        StringBuilder explanation
    ) {
        String field = condition.field();
        Object contextValue = context.get(field);
        
        if (contextValue == null) {
            explanation.append("Field '").append(field).append("' not found in context. ");
            return false;
        }
        
        String contextValueStr = contextValue.toString();
        String expectedValue = condition.value();
        
        boolean result = switch (condition.operator()) {
            case EQ -> {
                boolean matches = contextValueStr.equals(expectedValue);
                if (!matches) {
                    explanation.append("Field '").append(field).append("' value '")
                        .append(contextValueStr).append("' does not equal expected '")
                        .append(expectedValue).append("'. ");
                }
                yield matches;
            }
            case NEQ -> {
                boolean matches = !contextValueStr.equals(expectedValue);
                if (!matches) {
                    explanation.append("Field '").append(field).append("' value '")
                        .append(contextValueStr).append("' should not equal '")
                        .append(expectedValue).append("'. ");
                }
                yield matches;
            }
            case GT -> {
                boolean matches = compareNumeric(contextValueStr, expectedValue) > 0;
                if (!matches) {
                    explanation.append("Field '").append(field).append("' value '")
                        .append(contextValueStr).append("' is not greater than '")
                        .append(expectedValue).append("'. ");
                }
                yield matches;
            }
            case LT -> {
                boolean matches = compareNumeric(contextValueStr, expectedValue) < 0;
                if (!matches) {
                    explanation.append("Field '").append(field).append("' value '")
                        .append(contextValueStr).append("' is not less than '")
                        .append(expectedValue).append("'. ");
                }
                yield matches;
            }
            case GTE -> {
                boolean matches = compareNumeric(contextValueStr, expectedValue) >= 0;
                if (!matches) {
                    explanation.append("Field '").append(field).append("' value '")
                        .append(contextValueStr).append("' is not greater than or equal to '")
                        .append(expectedValue).append("'. ");
                }
                yield matches;
            }
            case LTE -> {
                boolean matches = compareNumeric(contextValueStr, expectedValue) <= 0;
                if (!matches) {
                    explanation.append("Field '").append(field).append("' value '")
                        .append(contextValueStr).append("' is not less than or equal to '")
                        .append(expectedValue).append("'. ");
                }
                yield matches;
            }
            case IN -> {
                Set<String> allowedValues = Arrays.stream(expectedValue.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
                boolean matches = allowedValues.contains(contextValueStr);
                if (!matches) {
                    explanation.append("Field '").append(field).append("' value '")
                        .append(contextValueStr).append("' is not in allowed set ")
                        .append(allowedValues).append(". ");
                }
                yield matches;
            }
            case NOT_IN -> {
                Set<String> disallowedValues = Arrays.stream(expectedValue.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
                boolean matches = !disallowedValues.contains(contextValueStr);
                if (!matches) {
                    explanation.append("Field '").append(field).append("' value '")
                        .append(contextValueStr).append("' is in disallowed set ")
                        .append(disallowedValues).append(". ");
                }
                yield matches;
            }
        };
        
        return result;
    }
    
    private int compareNumeric(String value1, String value2) {
        try {
            double num1 = Double.parseDouble(value1);
            double num2 = Double.parseDouble(value2);
            return Double.compare(num1, num2);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Cannot compare non-numeric values: " + value1 + ", " + value2
            );
        }
    }
}
