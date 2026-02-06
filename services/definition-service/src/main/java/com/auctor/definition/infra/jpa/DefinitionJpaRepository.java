package com.auctor.definition.infra.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DefinitionJpaRepository
        extends JpaRepository<DefinitionJpaEntity, String> {
}
