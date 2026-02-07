package com.auctor.definition.domain.service;

import com.auctor.definition.domain.model.Definition;
import com.auctor.definition.domain.model.DefinitionId;
import com.auctor.definition.domain.port.DefinitionQueryPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class DefinitionService {

    private final DefinitionQueryPort definitionQueryPort;
    private final Counter fetchCounter;

    public DefinitionService(DefinitionQueryPort definitionQueryPort, MeterRegistry registry) {
        this.fetchCounter =
            Counter.builder("definition.fetch.count")
                   .description("Number of definition fetches")
                   .register(registry);
        this.definitionQueryPort = definitionQueryPort;
    }

    public Definition getDefinition(DefinitionId id) {
        return definitionQueryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Definition not found: " + id.value()
                ));
    }
}
