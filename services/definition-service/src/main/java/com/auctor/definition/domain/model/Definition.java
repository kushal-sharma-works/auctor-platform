package com.auctor.definition.domain.model;

import java.util.Objects;

public final class Definition {

    private final DefinitionId id;
    private final String name;
    private final String description;

    public Definition(DefinitionId id, String name, String description) {
        this.id = Objects.requireNonNull(id, "id must not be null");

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = name;

        this.description = description == null ? "" : description;
    }

    public DefinitionId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }
}
