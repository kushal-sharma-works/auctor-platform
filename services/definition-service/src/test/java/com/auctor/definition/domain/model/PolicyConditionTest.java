package com.auctor.definition.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enhanced unit tests for PolicyCondition value object.
 */
class PolicyConditionTest {
    
    @Test
    void shouldCreateValidPolicyCondition() {
        // When
        PolicyCondition condition = new PolicyCondition("amount", Operator.GT, "1000");
        
        // Then
        assertNotNull(condition);
        assertEquals("amount", condition.field());
        assertEquals(Operator.GT, condition.operator());
        assertEquals("1000", condition.value());
    }
    
    @Test
    void shouldRejectNullField() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new PolicyCondition(null, Operator.EQ, "value")
        );
        
        assertEquals("field must not be blank", exception.getMessage());
    }
    
    @Test
    void shouldRejectBlankField() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            new PolicyCondition("", Operator.EQ, "value")
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            new PolicyCondition("   ", Operator.EQ, "value")
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            new PolicyCondition("\t", Operator.EQ, "value")
        );
    }
    
    @Test
    void shouldRejectNullOperator() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new PolicyCondition("field", null, "value")
        );
        
        assertEquals("operator must not be null", exception.getMessage());
    }
    
    @Test
    void shouldRejectNullValue() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new PolicyCondition("field", Operator.EQ, null)
        );
        
        assertEquals("value must not be null", exception.getMessage());
    }
    
    @Test
    void shouldAllowEmptyStringValue() {
        // When
        PolicyCondition condition = new PolicyCondition("field", Operator.EQ, "");
        
        // Then
        assertEquals("", condition.value());
    }
    
    @Test
    void shouldHandleAllOperators() {
        for (Operator operator : Operator.values()) {
            PolicyCondition condition = new PolicyCondition("field", operator, "value");
            assertEquals(operator, condition.operator());
        }
    }
    
    @Test
    void shouldHandleNumericValues() {
        // When
        PolicyCondition condition = new PolicyCondition("age", Operator.GTE, "18");
        
        // Then
        assertEquals("age", condition.field());
        assertEquals(Operator.GTE, condition.operator());
        assertEquals("18", condition.value());
    }
    
    @Test
    void shouldHandleCommaSeparatedValues() {
        // When
        PolicyCondition condition = new PolicyCondition(
            "country",
            Operator.IN,
            "USA,CANADA,UK"
        );
        
        // Then
        assertEquals("USA,CANADA,UK", condition.value());
    }
    
    @Test
    void shouldHandleComplexFieldNames() {
        // When
        PolicyCondition condition1 = new PolicyCondition("user.profile.age", Operator.GT, "21");
        PolicyCondition condition2 = new PolicyCondition("status_code", Operator.EQ, "200");
        PolicyCondition condition3 = new PolicyCondition("field-with-dash", Operator.NEQ, "value");
        
        // Then
        assertEquals("user.profile.age", condition1.field());
        assertEquals("status_code", condition2.field());
        assertEquals("field-with-dash", condition3.field());
    }
    
    @Test
    void shouldHandleSpecialCharactersInValue() {
        // When
        PolicyCondition condition = new PolicyCondition(
            "description",
            Operator.EQ,
            "Value with special chars: @#$%"
        );
        
        // Then
        assertEquals("Value with special chars: @#$%", condition.value());
    }
    
    @Test
    void shouldImplementRecordEquality() {
        // Given
        PolicyCondition condition1 = new PolicyCondition("field", Operator.EQ, "value");
        PolicyCondition condition2 = new PolicyCondition("field", Operator.EQ, "value");
        PolicyCondition condition3 = new PolicyCondition("field", Operator.NEQ, "value");
        PolicyCondition condition4 = new PolicyCondition("other", Operator.EQ, "value");
        
        // Then
        assertEquals(condition1, condition2);
        assertNotEquals(condition1, condition3);
        assertNotEquals(condition1, condition4);
        assertEquals(condition1.hashCode(), condition2.hashCode());
    }
    
    @Test
    void shouldImplementToString() {
        // Given
        PolicyCondition condition = new PolicyCondition("amount", Operator.LTE, "5000");
        
        // When
        String toString = condition.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("amount"));
        assertTrue(toString.contains("LTE"));
        assertTrue(toString.contains("5000"));
    }
    
    @Test
    void shouldHandleBooleanValues() {
        // When
        PolicyCondition condition = new PolicyCondition("isActive", Operator.EQ, "true");
        
        // Then
        assertEquals("true", condition.value());
    }
    
    @Test
    void shouldHandleDecimalValues() {
        // When
        PolicyCondition condition = new PolicyCondition("price", Operator.LTE, "99.99");
        
        // Then
        assertEquals("99.99", condition.value());
    }
    
    @Test
    void shouldHandleNegativeNumbers() {
        // When
        PolicyCondition condition = new PolicyCondition("balance", Operator.LT, "-100");
        
        // Then
        assertEquals("-100", condition.value());
    }
}
