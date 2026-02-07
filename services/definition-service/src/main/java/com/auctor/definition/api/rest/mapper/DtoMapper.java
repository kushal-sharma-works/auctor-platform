package com.auctor.definition.api.rest.mapper;

import com.auctor.definition.api.rest.dto.*;
import com.auctor.definition.domain.model.*;

import java.util.stream.Collectors;

/**
 * Mapper between REST DTOs and domain models.
 */
public class DtoMapper {
    
    // Workflow mappings
    
    public static WorkflowResponse toResponse(WorkflowDefinition workflow) {
        return new WorkflowResponse(
            workflow.id().value(),
            workflow.name(),
            workflow.version(),
            workflow.status().label(),
            workflow.states(),
            workflow.initialState(),
            workflow.transitions().stream()
                .map(t -> new TransitionResponse(t.fromState(), t.toState(), t.policyRef(), t.guardExpression()))
                .collect(Collectors.toList()),
            workflow.createdAt(),
            workflow.updatedAt()
        );
    }
    
    // Policy mappings
    
    public static PolicyResponse toResponse(PolicyDefinition policy) {
        return new PolicyResponse(
            policy.id().value(),
            policy.name(),
            policy.version(),
            policy.status().label(),
            policy.conditions().stream()
                .map(c -> new PolicyConditionResponse(c.field(), c.operator().name(), c.value()))
                .collect(Collectors.toList()),
            policy.createdAt()
        );
    }
    
    public static Transition toDomain(TransitionRequest request) {
        return new Transition(
            request.fromState(),
            request.toState(),
            request.policyRef(),
            request.guardExpression()
        );
    }
    
    public static PolicyCondition toDomain(PolicyConditionRequest request) {
        return new PolicyCondition(
            request.field(),
            Operator.valueOf(request.operator()),
            request.value()
        );
    }
}
