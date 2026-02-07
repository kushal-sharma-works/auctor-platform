package com.auctor.definition.infra.jpa.converter;

import com.auctor.definition.infra.jpa.TransitionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransitionListConverter.
 */
class TransitionListConverterTest {
    
    private TransitionListConverter converter;
    
    @BeforeEach
    void setUp() {
        converter = new TransitionListConverter();
    }
    
    @Test
    void shouldConvertTransitionListToJson() {
        // Given
        List<TransitionDto> transitions = List.of(
            new TransitionDto("START", "END", "policy-1", "guard-1"),
            new TransitionDto("PENDING", "APPROVED", null, "amount > 100")
        );
        
        // When
        String json = converter.convertToDatabaseColumn(transitions);
        
        // Then
        assertNotNull(json);
        assertTrue(json.contains("START"));
        assertTrue(json.contains("END"));
        assertTrue(json.contains("policy-1"));
        assertTrue(json.contains("PENDING"));
    }
    
    @Test
    void shouldConvertJsonToTransitionList() {
        // Given
        String json = "[{\"fromState\":\"START\",\"toState\":\"END\"," +
                     "\"policyRef\":\"policy-1\",\"guardExpression\":\"guard-1\"}," +
                     "{\"fromState\":\"PENDING\",\"toState\":\"APPROVED\"," +
                     "\"policyRef\":null,\"guardExpression\":\"amount > 100\"}]";
        
        // When
        List<TransitionDto> transitions = converter.convertToEntityAttribute(json);
        
        // Then
        assertNotNull(transitions);
        assertEquals(2, transitions.size());
        
        TransitionDto first = transitions.get(0);
        assertEquals("START", first.fromState());
        assertEquals("END", first.toState());
        assertEquals("policy-1", first.policyRef());
        assertEquals("guard-1", first.guardExpression());
        
        TransitionDto second = transitions.get(1);
        assertEquals("PENDING", second.fromState());
        assertEquals("APPROVED", second.toState());
        assertNull(second.policyRef());
        assertEquals("amount > 100", second.guardExpression());
    }
    
    @Test
    void shouldHandleNullOptionalFields() {
        // Given
        List<TransitionDto> transitions = List.of(
            new TransitionDto("A", "B", null, null)
        );
        
        // When
        String json = converter.convertToDatabaseColumn(transitions);
        List<TransitionDto> result = converter.convertToEntityAttribute(json);
        
        // Then
        assertEquals(1, result.size());
        assertEquals("A", result.get(0).fromState());
        assertEquals("B", result.get(0).toState());
        assertNull(result.get(0).policyRef());
        assertNull(result.get(0).guardExpression());
    }
    
    @Test
    void shouldHandleEmptyList() {
        // Given
        List<TransitionDto> emptyList = List.of();
        
        // When
        String json = converter.convertToDatabaseColumn(emptyList);
        
        // Then
        assertEquals("[]", json);
    }
    
    @Test
    void shouldConvertEmptyJsonToEmptyList() {
        // When
        List<TransitionDto> list = converter.convertToEntityAttribute("[]");
        
        // Then
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }
    
    @Test
    void shouldHandleNullList() {
        // When
        String json = converter.convertToDatabaseColumn(null);
        
        // Then
        assertEquals("[]", json);
    }
    
    @Test
    void shouldHandleNullJson() {
        // When
        List<TransitionDto> list = converter.convertToEntityAttribute(null);
        
        // Then
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }
    
    @Test
    void shouldHandleEmptyString() {
        // When
        List<TransitionDto> list = converter.convertToEntityAttribute("");
        
        // Then
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }
    
    @Test
    void shouldRoundTripConversion() {
        // Given
        List<TransitionDto> original = List.of(
            new TransitionDto("DRAFT", "REVIEW", null, null),
            new TransitionDto("REVIEW", "APPROVED", "policy-check", "approvals >= 2"),
            new TransitionDto("REVIEW", "REJECTED", null, "rejected == true"),
            new TransitionDto("APPROVED", "COMPLETED", "shipping-policy", null)
        );
        
        // When
        String json = converter.convertToDatabaseColumn(original);
        List<TransitionDto> result = converter.convertToEntityAttribute(json);
        
        // Then
        assertEquals(original.size(), result.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.get(i).fromState(), result.get(i).fromState());
            assertEquals(original.get(i).toState(), result.get(i).toState());
            assertEquals(original.get(i).policyRef(), result.get(i).policyRef());
            assertEquals(original.get(i).guardExpression(), result.get(i).guardExpression());
        }
    }
    
    @Test
    void shouldHandleSingleTransition() {
        // Given
        List<TransitionDto> single = List.of(
            new TransitionDto("START", "FINISH", "policy", "guard")
        );
        
        // When
        String json = converter.convertToDatabaseColumn(single);
        List<TransitionDto> result = converter.convertToEntityAttribute(json);
        
        // Then
        assertEquals(1, result.size());
        assertEquals("START", result.get(0).fromState());
        assertEquals("FINISH", result.get(0).toState());
    }
    
    @Test
    void shouldHandleComplexGuardExpressions() {
        // Given
        List<TransitionDto> transitions = List.of(
            new TransitionDto("A", "B", null, "amount > 1000 && status == 'ACTIVE'"),
            new TransitionDto("B", "C", null, "(x >= 5 || y < 10) && z != 'INVALID'")
        );
        
        // When
        String json = converter.convertToDatabaseColumn(transitions);
        List<TransitionDto> result = converter.convertToEntityAttribute(json);
        
        // Then
        assertEquals(2, result.size());
        assertEquals("amount > 1000 && status == 'ACTIVE'", result.get(0).guardExpression());
        assertEquals("(x >= 5 || y < 10) && z != 'INVALID'", result.get(1).guardExpression());
    }
    
    @Test
    void shouldHandleSpecialCharactersInStateNames() {
        // Given
        List<TransitionDto> transitions = List.of(
            new TransitionDto("STATE_WITH_UNDERSCORE", "STATE-WITH-DASH", null, null),
            new TransitionDto("STATE.WITH.DOT", "STATE:WITH:COLON", null, null)
        );
        
        // When
        String json = converter.convertToDatabaseColumn(transitions);
        List<TransitionDto> result = converter.convertToEntityAttribute(json);
        
        // Then
        assertEquals(2, result.size());
        assertEquals("STATE_WITH_UNDERSCORE", result.get(0).fromState());
        assertEquals("STATE-WITH-DASH", result.get(0).toState());
    }
    
    @Test
    void shouldThrowExceptionForInvalidJson() {
        // Given
        String invalidJson = "{this is not valid json}";
        
        // When & Then
        assertThrows(RuntimeException.class, () ->
            converter.convertToEntityAttribute(invalidJson)
        );
    }
}
