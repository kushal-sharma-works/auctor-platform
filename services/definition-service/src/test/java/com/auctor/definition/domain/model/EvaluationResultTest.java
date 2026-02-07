package com.auctor.definition.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EvaluationResult value object.
 */
class EvaluationResultTest {
    
    @Test
    void shouldCreateAllowedResult() {
        // When
        EvaluationResult result = new EvaluationResult(true, "All conditions met");
        
        // Then
        assertTrue(result.allowed());
        assertEquals("All conditions met", result.explanation());
    }
    
    @Test
    void shouldCreateDeniedResult() {
        // When
        EvaluationResult result = new EvaluationResult(false, "Condition 'amount > 1000' failed");
        
        // Then
        assertFalse(result.allowed());
        assertEquals("Condition 'amount > 1000' failed", result.explanation());
    }
    
    @Test
    void shouldAllowEmptyExplanation() {
        // When
        EvaluationResult result = new EvaluationResult(true, "");
        
        // Then
        assertTrue(result.allowed());
        assertEquals("", result.explanation());
    }
    
    @Test
    void shouldAllowNullExplanation() {
        // When
        EvaluationResult result = new EvaluationResult(false, null);
        
        // Then
        assertFalse(result.allowed());
        assertNull(result.explanation());
    }
    
    @Test
    void shouldImplementRecordEquality() {
        // Given
        EvaluationResult result1 = new EvaluationResult(true, "explanation");
        EvaluationResult result2 = new EvaluationResult(true, "explanation");
        EvaluationResult result3 = new EvaluationResult(false, "explanation");
        EvaluationResult result4 = new EvaluationResult(true, "different");
        
        // Then
        assertEquals(result1, result2);
        assertNotEquals(result1, result3);
        assertNotEquals(result1, result4);
        assertEquals(result1.hashCode(), result2.hashCode());
    }
    
    @Test
    void shouldImplementToString() {
        // Given
        EvaluationResult result = new EvaluationResult(true, "test explanation");
        
        // When
        String toString = result.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("true"));
        assertTrue(toString.contains("test explanation"));
    }
    
    @Test
    void shouldHandleLongExplanation() {
        // Given
        String longExplanation = "This is a very long explanation that describes " +
                "in detail why the policy evaluation failed, including multiple " +
                "conditions that were checked and their individual results.";
        
        // When
        EvaluationResult result = new EvaluationResult(false, longExplanation);
        
        // Then
        assertEquals(longExplanation, result.explanation());
    }
    
    @Test
    void shouldHandleMultilineExplanation() {
        // Given
        String multilineExplanation = "Policy evaluation failed:\n" +
                "- Condition 1: PASSED\n" +
                "- Condition 2: FAILED (amount too high)\n" +
                "- Condition 3: PASSED";
        
        // When
        EvaluationResult result = new EvaluationResult(false, multilineExplanation);
        
        // Then
        assertEquals(multilineExplanation, result.explanation());
    }
}
