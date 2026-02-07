package com.auctor.definition.infra.jpa.converter;

import com.auctor.definition.infra.jpa.TransitionDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * JPA converter for storing List<TransitionDto> as JSONB.
 */
@Converter
public class TransitionListConverter implements AttributeConverter<List<TransitionDto>, String> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(List<TransitionDto> attribute) {
        if (attribute == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting transition list to JSON", e);
        }
    }
    
    @Override
    public List<TransitionDto> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<TransitionDto>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON to transition list", e);
        }
    }
}
