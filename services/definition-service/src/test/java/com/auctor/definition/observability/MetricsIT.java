package com.auctor.definition.observability;

import com.auctor.definition.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MetricsIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExposeCustomMetricsInPrometheus() throws Exception {
        Map<String, Object> workflowRequest = Map.of(
            "name", "Metrics Workflow",
            "states", List.of("DRAFT", "APPROVED"),
            "initialState", "DRAFT",
            "transitions", List.of(
                Map.of("fromState", "DRAFT", "toState", "APPROVED")
            )
        );

        String workflowResponse = mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(workflowRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String workflowId = objectMapper.readTree(workflowResponse).get("id").asText();

        mockMvc.perform(post("/api/v1/workflows/" + workflowId + "/publish"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("workflow_created_total")))
            .andExpect(content().string(containsString("workflow_published_total")));
    }
}
