package com.auctor.definition.grpc.v1;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DefinitionGrpcServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("definition_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static ManagedChannel channel;
    private static DefinitionServiceGrpc.DefinitionServiceBlockingStub stub;

    @BeforeAll
    static void setup() {
        channel = ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        stub = DefinitionServiceGrpc.newBlockingStub(channel);
    }

    @AfterAll
    static void teardown() {
        channel.shutdown();
    }

    @Test
    void shouldReturnDefinition() {
        GetDefinitionRequest request =
                GetDefinitionRequest.newBuilder()
                        .setId("123")
                        .build();

        GetDefinitionResponse response = stub.getDefinition(request);

        assertThat(response.getId()).isEqualTo("123");
        assertThat(response.getName()).isEqualTo("sample-definition");
        assertThat(response.getDescription()).isEqualTo("stored in database");
    }
}
