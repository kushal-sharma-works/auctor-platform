package com.auctor.definition.config;

import com.auctor.definition.util.JwtTokenGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
class SecurityConfigIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void graphqlWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/graphql")
                .contentType("application/json")
                .content(workflowsQuery()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void graphqlViewerCanReadButCannotWrite() throws Exception {
        String viewerToken = JwtTokenGenerator.generate("viewer-user", List.of("VIEWER"));

        mockMvc.perform(post("/graphql")
                .header("Authorization", "Bearer " + viewerToken)
                .contentType("application/json")
                .content(workflowsQuery()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/graphql")
                .header("Authorization", "Bearer " + viewerToken)
                .contentType("application/json")
                .content(createPolicyMutation()))
            .andExpect(status().isForbidden());
    }

    @Test
    void graphqlAdminCanWrite() throws Exception {
        String adminToken = JwtTokenGenerator.generate("admin-user", List.of("ADMIN"));

        mockMvc.perform(post("/graphql")
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .content(createPolicyMutation()))
            .andExpect(status().isOk());
    }

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    private static String workflowsQuery() {
        return "{\"query\":\"query { workflows(page:0,size:1){ totalElements totalPages page size content { id name } } }\"}";
    }

    private static String createPolicyMutation() {
        return "{\"query\":\"mutation { createPolicy(input: { name: \\\"Policy\\\", conditions: [{ field: \\\"amount\\\", operator: \\\"GT\\\", value: \\\"100\\\" }] }) { id name } }\"}";
    }
}
