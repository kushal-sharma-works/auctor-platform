package com.auctor.definition.observability;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test-h2")
class CorrelationIdFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldPropagateCorrelationIdHeader() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .header("X-Correlation-ID", "corr-123"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Correlation-ID", "corr-123"));
    }

    @Test
    void shouldGenerateCorrelationIdHeaderWhenMissing() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Correlation-ID"));
    }
}
