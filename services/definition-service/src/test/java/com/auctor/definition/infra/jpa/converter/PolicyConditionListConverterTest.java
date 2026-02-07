package com.auctor.definition.infra.jpa.converter;

import com.auctor.definition.infra.jpa.PolicyConditionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PolicyConditionListConverter.
 */
class PolicyConditionListConverterTest {
    
    private PolicyConditionListConverter converter;
    
    @BeforeEach
    void setUp() {
        converter = new PolicyConditionListConverter();
    }
    
    @Test
    void shouldConvertPolicyConditionListToJson() {
        // Given
        List<PolicyConditionDto> conditions = List.of(
            new PolicyConditionDto("amount", "GT", "1000"),
            new PolicyConditionDto("status", "EQ", "ACTIVE")
        );
        
        // When
        String json = converter.convertToDatabaseColumn(conditions);
        
        // Then
        assertNotNull(json);
        assertTrue(json.contains("amount"));
        assertTrue(json.contains("GT"));
        assertTrue(json.contains("1000"));
        assertTrue(json.contains("status"));
    }
    
    @Test
    void shouldConvertJsonToPolicyConditionList() {
        // Given
        String json = "[{\"field\":\"amount\",\"operator\":\"GT\",\"value\":\"1000\"}," +
                     "{\"field\":\"status\",\"operator\":\"EQ\",\"value\":\"ACTIVE\"}]";
        
        // When
        List<PolicyConditionDto> conditions = converter.convertToEntityAttribute(json);
        
        // Then
        assertNotNull(conditions);
        assertEquals(2, conditions.size());
        
        PolicyConditionDto first = conditions.get(0);
        assertEquals("amount", first.field());
        assertEquals("GT", first.operator());
        assertEquals("1000", first.value());
        
        PolicyConditionDto second = conditions.get(1);
        assertEquals("status", second.field());
        assertEquals("EQ", second.operator());
        assertEquals("ACTIVE", second.value());
    }
    
    @Test
    void shouldHandleEmptyList() {
        // Given
        List<PolicyConditionDto> emptyList = List.of();
        
        // When
        String json = converter.convertToDatabaseColumn(emptyList);
        
        // Then
        assertEquals("[]", json);
    }
    
    @Test
    void shouldConvertEmptyJsonToEmptyList() {
        // When
        List<PolicyConditionDto> list = converter.convertToEntityAttribute("[]");
        
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
        List<PolicyConditionDto> list = converter.convertToEntityAttribute(null);
        
        // Then
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }
    
    @Test
    void shouldHandleEmptyString() {
        // When
        List<PolicyConditionDto> list = converter.convertToEntityAttribute("");
        
        // Then
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }
    
    @Test
    void shouldRoundTripConversion() {
        // Given
        List<PolicyConditionDto> original = List.of(
            new PolicyConditionDto("field1", "LTE", "100"),
            new PolicyConditionDto("field2", "IN", "A,B,C"),
            new PolicyConditionDto("field3", "NEQ", "value")
        );
        
        // When
        String json = converter.convertToDatabaseColumn(original);
        List<PolicyConditionDto> result = converter.convertToEntityAttribute(json);
        
        // Then
        assertEquals(original.size(), result.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.get(i).field(), result.get(i).field());
            assertEquals(original.get(i).operator(), result.get(i).operator());
            assertEquals(original.get(i).value(), result.get(i).value());
        }
    }
    
    @Test
    void shouldHandleSingleCondition() {
        // Given
        List<PolicyConditionDto> single = List.of(
            new PolicyConditionDto("test", "EQ", "value")
        );
        
        // When
        String json = converter.convertToDatabaseColumn(single);
        List<PolicyConditionDto> result = converter.convertToEntityAttribute(json);
        
        // Then
        assertEquals(1, result.size());
        assertEquals("test", result.get(0).field());
    }
    
    @Test
    void shouldHandleSpecialCharactersInValues() {
        // Given
        List<PolicyConditionDto> conditions = List.of(
            new PolicyConditionDto("field", "EQ", "value-with-special_chars.123")
        );
        
        // When
        String json = converter.convertToDatabaseColumn(conditions);
        List<PolicyConditionDto> result = converter.convertToEntityAttribute(json);
        
        // Then
        assertEquals("value-with-special_chars.123", result.get(0).value());
    }
    
    @Test
    void shouldHandleAllOperatorTypes() {
        // Given
        List<PolicyConditionDto> conditions = List.of(
            new PolicyConditionDto("f1", "EQ", "v1"),
            new PolicyConditionDto("f2", "NEQ", "v2"),
            new PolicyConditionDto("f3", "GT", "v3"),
            new PolicyConditionDto("f4", "LT", "v4"),
            new PolicyConditionDto("f5", "GTE", "v5"),
            new PolicyConditionDto("f6", "LTE", "v6"),
            new PolicyConditionDto("f7", "IN", "v7"),
            new PolicyConditionDto("f8", "NOT_IN", "v8")
        );
        
        // When
        String json = converter.convertToDatabaseColumn(conditions);
        List<PolicyConditionDto> result = converter.convertToEntityAttribute(json);
        
        // Then
        assertEquals(8, result.size());
        assertEquals("EQ", result.get(0).operator());
        assertEquals("NOT_IN", result.get(7).operator());
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
