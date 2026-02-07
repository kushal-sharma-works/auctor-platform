package com.auctor.definition.infra.adapter;

import com.auctor.definition.domain.model.*;
import com.auctor.definition.infra.jpa.PolicyConditionDto;
import com.auctor.definition.infra.jpa.PolicyJpaEntity;
import com.auctor.definition.infra.jpa.TransitionDto;
import com.auctor.definition.infra.jpa.WorkflowJpaEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper between domain models and JPA entities.
 */
public class DomainMapper {
    
    // Workflow mappings
    
    public static WorkflowJpaEntity toJpaEntity(WorkflowDefinition domain) {
        WorkflowJpaEntity entity = new WorkflowJpaEntity();
        entity.setId(domain.id().value());
        entity.setVersion(domain.version());
        entity.setName(domain.name());
        entity.setStatus(domain.status().label());
        entity.setStates(domain.states());
        entity.setInitialState(domain.initialState());
        entity.setTransitions(toTransitionDtos(domain.transitions()));
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }
    
    public static WorkflowDefinition toWorkflowDomain(WorkflowJpaEntity entity) {
        return new WorkflowDefinition(
            new WorkflowId(entity.getId()),
            entity.getName(),
            entity.getVersion(),
            WorkflowStatus.fromLabel(entity.getStatus()),
            entity.getStates(),
            entity.getInitialState(),
            toTransitions(entity.getTransitions()),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    private static List<TransitionDto> toTransitionDtos(List<Transition> transitions) {
        return transitions.stream()
            .map(t -> new TransitionDto(t.fromState(), t.toState(), t.policyRef(), t.guardExpression()))
            .collect(Collectors.toList());
    }
    
    private static List<Transition> toTransitions(List<TransitionDto> dtos) {
        return dtos.stream()
            .map(dto -> new Transition(dto.fromState(), dto.toState(), dto.policyRef(), dto.guardExpression()))
            .collect(Collectors.toList());
    }
    
    // Policy mappings
    
    public static PolicyJpaEntity toJpaEntity(PolicyDefinition domain) {
        PolicyJpaEntity entity = new PolicyJpaEntity();
        entity.setId(domain.id().value());
        entity.setVersion(domain.version());
        entity.setName(domain.name());
        entity.setStatus(domain.status().label());
        entity.setConditions(toPolicyConditionDtos(domain.conditions()));
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }
    
    public static PolicyDefinition toPolicyDomain(PolicyJpaEntity entity) {
        return new PolicyDefinition(
            new PolicyId(entity.getId()),
            entity.getName(),
            entity.getVersion(),
            PolicyStatus.fromLabel(entity.getStatus()),
            toPolicyConditions(entity.getConditions()),
            entity.getCreatedAt()
        );
    }
    
    private static List<PolicyConditionDto> toPolicyConditionDtos(List<PolicyCondition> conditions) {
        return conditions.stream()
            .map(c -> new PolicyConditionDto(c.field(), c.operator().name(), c.value()))
            .collect(Collectors.toList());
    }
    
    private static List<PolicyCondition> toPolicyConditions(List<PolicyConditionDto> dtos) {
        return dtos.stream()
            .map(dto -> new PolicyCondition(dto.field(), Operator.valueOf(dto.operator()), dto.value()))
            .collect(Collectors.toList());
    }
}
