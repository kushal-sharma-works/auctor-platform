package com.auctor.definition.api.graphql.dto;

import com.auctor.definition.domain.model.Operator;
import com.auctor.definition.domain.model.PolicyCondition;

/**
 * GraphQL DTO for PolicyCondition.
 */
public record PolicyConditionGraphQLDto(
    String field,
    String operator,
    String value
) {
    public static PolicyConditionGraphQLDto from(PolicyCondition condition) {
        return new PolicyConditionGraphQLDto(
            condition.field(),
            condition.operator().name(),
            condition.value()
        );
    }
    
    public PolicyCondition toDomain() {
        return new PolicyCondition(field, Operator.valueOf(operator), value);
    }
}
