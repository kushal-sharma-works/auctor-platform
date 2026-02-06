package com.auctor.definition.domain.service;

import com.auctor.definition.domain.model.Definition;
import com.auctor.definition.domain.model.DefinitionId;
import com.auctor.definition.domain.port.DefinitionQueryPort;

public class DefinitionService {

    private final DefinitionQueryPort definitionQueryPort;

    public DefinitionService(DefinitionQueryPort definitionQueryPort) {
        this.definitionQueryPort = definitionQueryPort;
    }

    public Definition getDefinition(DefinitionId id) {
        return definitionQueryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Definition not found: " + id.value()
                ));
    }
}
