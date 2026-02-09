package com.auctor.definition.api.graphql.input;

import com.auctor.definition.domain.model.Operator;
import com.auctor.definition.domain.model.PolicyCondition;

/**
 * GraphQL input for policy condition.
 */
public record PolicyConditionInput(
    String field,
    String operator,
    String value
) {
    public PolicyCondition toDomain() {
        return new PolicyCondition(field, Operator.valueOf(operator), value);
    }
}
