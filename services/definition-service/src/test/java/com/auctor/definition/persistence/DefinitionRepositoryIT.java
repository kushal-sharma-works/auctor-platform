package com.auctor.definition.persistence;

import com.auctor.definition.DefinitionServiceApplication;
import com.auctor.definition.infra.jpa.DefinitionJpaEntity;
import com.auctor.definition.infra.jpa.DefinitionJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DefinitionServiceApplication.class)
@Testcontainers
class DefinitionRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("definition")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private DefinitionJpaRepository definitionJpaRepository;

    @Test
    void shouldLoadDefinitionInsertedByFlyway() {
        DefinitionJpaEntity definition = definitionJpaRepository.findById("123").orElseThrow();

        assertThat(definition.getId()).isEqualTo("123");
        assertThat(definition.getName()).isEqualTo("sample-definition");
    }
}