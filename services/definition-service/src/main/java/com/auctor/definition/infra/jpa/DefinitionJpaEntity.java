package com.auctor.definition.infra.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "definitions")
public class DefinitionJpaEntity {

    @Id
    private String id;
    private String name;
    private String description;

    protected DefinitionJpaEntity() {}

    public DefinitionJpaEntity(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
