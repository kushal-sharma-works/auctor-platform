package com.auctor.definition.api.graphql.input;

import java.util.List;

/**
 * GraphQL input for creating a policy.
 */
public record CreatePolicyInput(
    String name,
    List<PolicyConditionInput> conditions
) {
}
