package com.auctor.definition.domain.port;

import com.auctor.definition.domain.model.Definition;
import com.auctor.definition.domain.model.DefinitionId;

import java.util.Optional;

public interface DefinitionQueryPort {
    Optional<Definition> findById(DefinitionId id);
}
