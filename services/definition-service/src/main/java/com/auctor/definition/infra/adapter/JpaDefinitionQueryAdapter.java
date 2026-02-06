package com.auctor.definition.infra.adapter;

import com.auctor.definition.domain.model.Definition;
import com.auctor.definition.domain.model.DefinitionId;
import com.auctor.definition.domain.port.DefinitionQueryPort;
import com.auctor.definition.infra.jpa.DefinitionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaDefinitionQueryAdapter implements DefinitionQueryPort {

    private final DefinitionJpaRepository repository;

    public JpaDefinitionQueryAdapter(DefinitionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Definition> findById(DefinitionId id) {
        return repository.findById(id.value())
                .map(e -> new Definition(
                        new DefinitionId(e.getId()),
                        e.getName(),
                        e.getDescription()
                ));
    }
}
