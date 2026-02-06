package com.auctor.definition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
public class DefinitionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DefinitionServiceApplication.class, args);
    }
}