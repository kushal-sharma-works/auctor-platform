package com.auctor.definition.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Core domain object representing a workflow definition.
 * Immutable domain model with validation rules.
 */
public class WorkflowDefinition {
    private final WorkflowId id;
    private final String name;
    private final int version;
    private final WorkflowStatus status;
    private final List<String> states;
    private final String initialState;
    private final List<Transition> transitions;
    private final Instant createdAt;
    private final Instant updatedAt;

    public WorkflowDefinition(
        WorkflowId id,
        String name,
        int version,
        WorkflowStatus status,
        List<String> states,
        String initialState,
        List<Transition> transitions,
        Instant createdAt,
        Instant updatedAt
    ) {
        // Validation
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (states == null || states.isEmpty()) {
            throw new IllegalArgumentException("states must not be empty");
        }
        if (initialState == null || !states.contains(initialState)) {
            throw new IllegalArgumentException("initialState must be in states list");
        }
        if (transitions != null) {
            for (Transition transition : transitions) {
                if (!states.contains(transition.fromState())) {
                    throw new IllegalArgumentException(
                        "transition fromState '" + transition.fromState() + "' not in states list"
                    );
                }
                if (!states.contains(transition.toState())) {
                    throw new IllegalArgumentException(
                        "transition toState '" + transition.toState() + "' not in states list"
                    );
                }
            }
        }

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = name;
        this.version = version;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.states = List.copyOf(states);
        this.initialState = initialState;
        this.transitions = transitions == null ? List.of() : List.copyOf(transitions);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public WorkflowId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int version() {
        return version;
    }

    public WorkflowStatus status() {
        return status;
    }

    public List<String> states() {
        return states;
    }

    public String initialState() {
        return initialState;
    }

    public List<Transition> transitions() {
        return transitions;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public WorkflowDefinition withStatus(WorkflowStatus newStatus) {
        return new WorkflowDefinition(
            id, name, version, newStatus, states, initialState, transitions, createdAt, Instant.now()
        );
    }

    public WorkflowDefinition withVersion(int newVersion) {
        return new WorkflowDefinition(
            id, name, newVersion, status, states, initialState, transitions, createdAt, Instant.now()
        );
    }
}
