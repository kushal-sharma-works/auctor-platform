package com.auctor.definition.infra.jpa.converter;

import com.auctor.definition.infra.jpa.PolicyConditionDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * JPA converter for storing List<PolicyConditionDto> as JSONB.
 */
@Converter
public class PolicyConditionListConverter implements AttributeConverter<List<PolicyConditionDto>, String> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(List<PolicyConditionDto> attribute) {
        if (attribute == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting policy condition list to JSON", e);
        }
    }
    
    @Override
    public List<PolicyConditionDto> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<PolicyConditionDto>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON to policy condition list", e);
        }
    }
}
