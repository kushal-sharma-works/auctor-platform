package com.auctor.definition.api.rest;

import com.auctor.definition.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PolicyController.
 */
@AutoConfigureMockMvc
class PolicyControllerIT extends IntegrationTestBase {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void shouldCreatePolicy() throws Exception {
        Map<String, Object> request = Map.of(
            "name", "Test Policy",
            "conditions", List.of(
                Map.of("field", "amount", "operator", "LTE", "value", "1000")
            )
        );
        
        mockMvc.perform(post("/api/v1/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Policy"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.id").exists());
    }
    
    @Test
    void shouldRejectInvalidPolicy() throws Exception {
        Map<String, Object> request = Map.of(
            "name", "",
            "conditions", List.of()
        );
        
        mockMvc.perform(post("/api/v1/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void shouldGetPolicyById() throws Exception {
        String id = createPolicyAndGetId();
        
        mockMvc.perform(get("/api/v1/policies/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value("Test Policy"));
    }
    
    @Test
    void shouldReturn404ForMissingPolicy() throws Exception {
        mockMvc.perform(get("/api/v1/policies/missing-id"))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void shouldPublishPolicy() throws Exception {
        String id = createPolicyAndGetId();
        
        mockMvc.perform(post("/api/v1/policies/" + id + "/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }
    
    @Test
    void shouldListPoliciesWithPagination() throws Exception {
        createPolicyAndGetId();
        
        mockMvc.perform(get("/api/v1/policies")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").exists());
    }
    
    private String createPolicyAndGetId() throws Exception {
        Map<String, Object> request = Map.of(
            "name", "Test Policy",
            "conditions", List.of(
                Map.of("field", "amount", "operator", "LTE", "value", "1000")
            )
        );
        
        String response = mockMvc.perform(post("/api/v1/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        
        return objectMapper.readTree(response).get("id").asText();
    }
}
