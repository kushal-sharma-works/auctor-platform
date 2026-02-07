package com.auctor.definition.infra.jpa.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StringListConverter.
 */
class StringListConverterTest {
    
    private StringListConverter converter;
    
    @BeforeEach
    void setUp() {
        converter = new StringListConverter();
    }
    
    @Test
    void shouldConvertListToJson() {
        // Given
        List<String> list = List.of("state1", "state2", "state3");
        
        // When
        String json = converter.convertToDatabaseColumn(list);
        
        // Then
        assertNotNull(json);
        assertTrue(json.contains("state1"));
        assertTrue(json.contains("state2"));
        assertTrue(json.contains("state3"));
    }
    
    @Test
    void shouldConvertJsonToList() {
        // Given
        String json = "[\"state1\",\"state2\",\"state3\"]";
        
        // When
        List<String> list = converter.convertToEntityAttribute(json);
        
        // Then
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("state1", list.get(0));
        assertEquals("state2", list.get(1));
        assertEquals("state3", list.get(2));
    }
    
    @Test
    void shouldHandleEmptyList() {
        // Given
        List<String> emptyList = List.of();
        
        // When
        String json = converter.convertToDatabaseColumn(emptyList);
        
        // Then
        assertEquals("[]", json);
    }
    
    @Test
    void shouldConvertEmptyJsonToEmptyList() {
        // When
        List<String> list = converter.convertToEntityAttribute("[]");
        
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
        List<String> list = converter.convertToEntityAttribute(null);
        
        // Then
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }
    
    @Test
    void shouldHandleEmptyString() {
        // When
        List<String> list = converter.convertToEntityAttribute("");
        
        // Then
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }
    
    @Test
    void shouldHandleSpecialCharacters() {
        // Given
        List<String> list = List.of("state-with-dash", "state_with_underscore", "state.with.dot");
        
        // When
        String json = converter.convertToDatabaseColumn(list);
        List<String> result = converter.convertToEntityAttribute(json);
        
        // Then
        assertEquals(list, result);
    }
    
    @Test
    void shouldRoundTripConversion() {
        // Given
        List<String> original = List.of("DRAFT", "REVIEW", "APPROVED", "REJECTED");
        
        // When
        String json = converter.convertToDatabaseColumn(original);
        List<String> result = converter.convertToEntityAttribute(json);
        
        // Then
        assertEquals(original, result);
    }
    
    @Test
    void shouldHandleSingleElementList() {
        // Given
        List<String> singleElement = List.of("SINGLE");
        
        // When
        String json = converter.convertToDatabaseColumn(singleElement);
        List<String> result = converter.convertToEntityAttribute(json);
        
        // Then
        assertEquals(1, result.size());
        assertEquals("SINGLE", result.get(0));
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
