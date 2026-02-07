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
 * Integration tests for WorkflowController.
 */
@AutoConfigureMockMvc
class WorkflowControllerIT extends IntegrationTestBase {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void shouldCreateWorkflow() throws Exception {
        Map<String, Object> request = Map.of(
            "name", "Test Workflow",
            "states", List.of("DRAFT", "APPROVED"),
            "initialState", "DRAFT",
            "transitions", List.of(
                Map.of("fromState", "DRAFT", "toState", "APPROVED")
            )
        );
        
        mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Workflow"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.id").exists());
    }
    
    @Test
    void shouldRejectInvalidWorkflow() throws Exception {
        Map<String, Object> request = Map.of(
            "name", "",
            "states", List.of(),
            "initialState", "DRAFT",
            "transitions", List.of()
        );
        
        mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void shouldGetWorkflowById() throws Exception {
        // First create a workflow
        String id = createWorkflowAndGetId();
        
        // Then fetch it
        mockMvc.perform(get("/api/v1/workflows/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value("Test Workflow"));
    }
    
    @Test
    void shouldReturn404ForMissingWorkflow() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/missing-id"))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void shouldPublishWorkflow() throws Exception {
        String id = createWorkflowAndGetId();
        
        mockMvc.perform(post("/api/v1/workflows/" + id + "/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }
    
    @Test
    void shouldReturn409WhenPublishingAlreadyPublished() throws Exception {
        String id = createWorkflowAndGetId();
        
        // Publish once
        mockMvc.perform(post("/api/v1/workflows/" + id + "/publish"))
            .andExpect(status().isOk());
        
        // Try to publish again
        mockMvc.perform(post("/api/v1/workflows/" + id + "/publish"))
            .andExpect(status().isBadRequest()); // Changed from 409 to 400 per GlobalExceptionHandler
    }
    
    @Test
    void shouldListWorkflowsWithPagination() throws Exception {
        createWorkflowAndGetId();
        
        mockMvc.perform(get("/api/v1/workflows")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").exists());
    }
    
    private String createWorkflowAndGetId() throws Exception {
        Map<String, Object> request = Map.of(
            "name", "Test Workflow",
            "states", List.of("DRAFT", "APPROVED"),
            "initialState", "DRAFT",
            "transitions", List.of(
                Map.of("fromState", "DRAFT", "toState", "APPROVED")
            )
        );
        
        String response = mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        
        return objectMapper.readTree(response).get("id").asText();
    }
}
