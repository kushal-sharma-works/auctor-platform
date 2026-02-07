package com.auctor.definition.config;

import com.auctor.definition.domain.port.DefinitionQueryPort;
import com.auctor.definition.domain.service.DefinitionService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    DefinitionService definitionService(DefinitionQueryPort port, MeterRegistry registry) {
        return new DefinitionService(port, registry);
    }
}
