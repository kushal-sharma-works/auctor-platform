package com.auctor.definition.domain.model;

import java.util.Objects;

public final class DefinitionId {

    private final String value;

    public DefinitionId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DefinitionId cannot be blank");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefinitionId that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
