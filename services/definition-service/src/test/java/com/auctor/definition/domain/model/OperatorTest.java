package com.auctor.definition.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Operator enum.
 */
class OperatorTest {
    
    @Test
    void shouldHaveAllExpectedOperators() {
        // Then
        assertEquals(8, Operator.values().length);
        assertNotNull(Operator.EQ);
        assertNotNull(Operator.NEQ);
        assertNotNull(Operator.GT);
        assertNotNull(Operator.LT);
        assertNotNull(Operator.GTE);
        assertNotNull(Operator.LTE);
        assertNotNull(Operator.IN);
        assertNotNull(Operator.NOT_IN);
    }
    
    @Test
    void shouldParseFromString() {
        // When & Then
        assertEquals(Operator.EQ, Operator.valueOf("EQ"));
        assertEquals(Operator.NEQ, Operator.valueOf("NEQ"));
        assertEquals(Operator.GT, Operator.valueOf("GT"));
        assertEquals(Operator.LT, Operator.valueOf("LT"));
        assertEquals(Operator.GTE, Operator.valueOf("GTE"));
        assertEquals(Operator.LTE, Operator.valueOf("LTE"));
        assertEquals(Operator.IN, Operator.valueOf("IN"));
        assertEquals(Operator.NOT_IN, Operator.valueOf("NOT_IN"));
    }
    
    @Test
    void shouldThrowExceptionForInvalidOperator() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            Operator.valueOf("INVALID")
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            Operator.valueOf("EQUALS")
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            Operator.valueOf("GREATER_THAN")
        );
    }
    
    @Test
    void shouldBeOrderedConsistently() {
        // Given
        Operator[] operators = Operator.values();
        
        // Then - verify order matches declaration
        assertEquals(Operator.EQ, operators[0]);
        assertEquals(Operator.NEQ, operators[1]);
        assertEquals(Operator.GT, operators[2]);
        assertEquals(Operator.LT, operators[3]);
        assertEquals(Operator.GTE, operators[4]);
        assertEquals(Operator.LTE, operators[5]);
        assertEquals(Operator.IN, operators[6]);
        assertEquals(Operator.NOT_IN, operators[7]);
    }
    
    @Test
    void shouldHaveCorrectName() {
        // When & Then
        assertEquals("EQ", Operator.EQ.name());
        assertEquals("NOT_IN", Operator.NOT_IN.name());
    }
    
    @Test
    void shouldSupportSwitch() {
        // Given
        Operator operator = Operator.GT;
        
        // When
        String result = switch (operator) {
            case EQ -> "equals";
            case NEQ -> "not equals";
            case GT -> "greater than";
            case LT -> "less than";
            case GTE -> "greater or equal";
            case LTE -> "less or equal";
            case IN -> "in set";
            case NOT_IN -> "not in set";
        };
        
        // Then
        assertEquals("greater than", result);
    }
    
    @Test
    void shouldBeUsableInCollections() {
        // Given
        java.util.Set<Operator> comparisonOperators = java.util.Set.of(
            Operator.EQ, Operator.NEQ, Operator.GT, Operator.LT, Operator.GTE, Operator.LTE
        );
        
        java.util.Set<Operator> setOperators = java.util.Set.of(
            Operator.IN, Operator.NOT_IN
        );
        
        // Then
        assertEquals(6, comparisonOperators.size());
        assertEquals(2, setOperators.size());
        assertTrue(comparisonOperators.contains(Operator.EQ));
        assertTrue(setOperators.contains(Operator.IN));
    }
}
