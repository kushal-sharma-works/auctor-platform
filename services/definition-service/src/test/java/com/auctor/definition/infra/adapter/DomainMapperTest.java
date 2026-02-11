package com.auctor.definition.infra.adapter;

import com.auctor.definition.domain.model.*;
import com.auctor.definition.infra.jpa.PolicyJpaEntity;
import com.auctor.definition.infra.jpa.WorkflowJpaEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainMapperTest {

    @Test
    void shouldMapWorkflowDomainToJpaAndBack() {
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2024-01-02T00:00:00Z");
        WorkflowDefinition workflow = new WorkflowDefinition(
            new WorkflowId("wf-1"),
            "Workflow",
            3,
            new WorkflowStatus.Published(),
            List.of("START", "END"),
            "START",
            List.of(new Transition("START", "END", "policy-1", "x > 0")),
            createdAt,
            updatedAt
        );

        WorkflowJpaEntity entity = DomainMapper.toJpaEntity(workflow);

        assertEquals("wf-1", entity.getId());
        assertEquals(3, entity.getVersion());
        assertEquals("Workflow", entity.getName());
        assertEquals("PUBLISHED", entity.getStatus());
        assertEquals(List.of("START", "END"), entity.getStates());
        assertEquals("START", entity.getInitialState());
        assertEquals(1, entity.getTransitions().size());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(updatedAt, entity.getUpdatedAt());

        WorkflowDefinition mapped = DomainMapper.toWorkflowDomain(entity);

        assertEquals("wf-1", mapped.id().value());
        assertEquals(3, mapped.version());
        assertEquals("Workflow", mapped.name());
        assertEquals("PUBLISHED", mapped.status().label());
        assertEquals(List.of("START", "END"), mapped.states());
        assertEquals("START", mapped.initialState());
        assertEquals(1, mapped.transitions().size());
        assertEquals("START", mapped.transitions().get(0).fromState());
        assertEquals("END", mapped.transitions().get(0).toState());
        assertEquals("policy-1", mapped.transitions().get(0).policyRef());
        assertEquals("x > 0", mapped.transitions().get(0).guardExpression());
        assertEquals(createdAt, mapped.createdAt());
        assertEquals(updatedAt, mapped.updatedAt());
    }

    @Test
    void shouldMapPolicyDomainToJpaAndBack() {
        Instant createdAt = Instant.parse("2024-02-01T00:00:00Z");
        PolicyDefinition policy = new PolicyDefinition(
            new PolicyId("policy-1"),
            "Policy",
            2,
            new PolicyStatus.Draft(),
            List.of(new PolicyCondition("amount", Operator.GTE, "100")),
            createdAt
        );

        PolicyJpaEntity entity = DomainMapper.toJpaEntity(policy);

        assertEquals("policy-1", entity.getId());
        assertEquals(2, entity.getVersion());
        assertEquals("Policy", entity.getName());
        assertEquals("DRAFT", entity.getStatus());
        assertEquals(1, entity.getConditions().size());
        assertEquals(createdAt, entity.getCreatedAt());

        PolicyDefinition mapped = DomainMapper.toPolicyDomain(entity);

        assertEquals("policy-1", mapped.id().value());
        assertEquals(2, mapped.version());
        assertEquals("Policy", mapped.name());
        assertEquals("DRAFT", mapped.status().label());
        assertEquals(1, mapped.conditions().size());
        assertEquals("amount", mapped.conditions().get(0).field());
        assertEquals(Operator.GTE, mapped.conditions().get(0).operator());
        assertEquals("100", mapped.conditions().get(0).value());
        assertEquals(createdAt, mapped.createdAt());
    }
}
