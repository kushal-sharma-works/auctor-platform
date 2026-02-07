package com.auctor.definition.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Core domain object representing a policy definition.
 * Immutable domain model with validation rules.
 */
public class PolicyDefinition {
    private final PolicyId id;
    private final String name;
    private final int version;
    private final PolicyStatus status;
    private final List<PolicyCondition> conditions;
    private final Instant createdAt;

    public PolicyDefinition(
        PolicyId id,
        String name,
        int version,
        PolicyStatus status,
        List<PolicyCondition> conditions,
        Instant createdAt
    ) {
        // Validation
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = name;
        this.version = version;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.conditions = conditions == null ? List.of() : List.copyOf(conditions);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public PolicyId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int version() {
        return version;
    }

    public PolicyStatus status() {
        return status;
    }

    public List<PolicyCondition> conditions() {
        return conditions;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public PolicyDefinition withStatus(PolicyStatus newStatus) {
        return new PolicyDefinition(id, name, version, newStatus, conditions, createdAt);
    }

    public PolicyDefinition withVersion(int newVersion) {
        return new PolicyDefinition(id, name, newVersion, status, conditions, createdAt);
    }
}
