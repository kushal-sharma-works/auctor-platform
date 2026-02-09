package com.auctor.definition.api.graphql;

import com.auctor.definition.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.graphql.test.tester.GraphQlTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Policy GraphQL API.
 * Tests all queries and mutations with various scenarios.
 */
@AutoConfigureGraphQlTester
class PolicyGraphQLControllerIT extends IntegrationTestBase {
    
    @Autowired
    private GraphQlTester graphQlTester;
    
    @Test
    void shouldCreatePolicy() {
        String mutation = """
            mutation {
                createPolicy(input: {
                    name: "High Value Order Policy",
                    conditions: [
                        {
                            field: "order.amount",
                            operator: "GREATER_THAN",
                            value: "1000"
                        },
                        {
                            field: "order.currency",
                            operator: "EQUALS",
                            value: "USD"
                        }
                    ]
                }) {
                    id
                    name
                    version
                    status
                    conditions {
                        field
                        operator
                        value
                    }
                    createdAt
                }
            }
            """;
        
        graphQlTester.document(mutation)
            .execute()
            .path("createPolicy")
            .entity(Object.class)
            .satisfies(policy -> {
                assertThat(policy).isNotNull();
            })
            .path("createPolicy.id").entity(String.class).satisfies(id -> {
                assertThat(id).isNotNull();
            })
            .path("createPolicy.name").entity(String.class).isEqualTo("High Value Order Policy")
            .path("createPolicy.version").entity(Integer.class).isEqualTo(1)
            .path("createPolicy.status").entity(String.class).isEqualTo("DRAFT")
            .path("createPolicy.conditions").entityList(Object.class).hasSize(2);
    }
    
    @Test
    void shouldGetPolicyById() {
        // First create a policy
        String createMutation = """
            mutation {
                createPolicy(input: {
                    name: "Simple Policy",
                    conditions: [
                        {
                            field: "status",
                            operator: "EQUALS",
                            value: "ACTIVE"
                        }
                    ]
                }) {
                    id
                }
            }
            """;
        
        String policyId = graphQlTester.document(createMutation)
            .execute()
            .path("createPolicy.id")
            .entity(String.class)
            .get();
        
        // Then query it
        String query = """
            query($id: ID!) {
                policy(id: $id) {
                    id
                    name
                    version
                    status
                    conditions {
                        field
                        operator
                        value
                    }
                }
            }
            """;
        
        graphQlTester.document(query)
            .variable("id", policyId)
            .execute()
            .path("policy.id").entity(String.class).isEqualTo(policyId)
            .path("policy.name").entity(String.class).isEqualTo("Simple Policy")
            .path("policy.status").entity(String.class).isEqualTo("DRAFT")
            .path("policy.conditions").entityList(Object.class).hasSize(1);
    }
    
    @Test
    void shouldListPoliciesWithPagination() {
        // Create multiple policies
        for (int i = 0; i < 3; i++) {
            String createMutation = String.format("""
                mutation {
                    createPolicy(input: {
                        name: "Policy %d",
                        conditions: [
                            {field: "field", operator: "EQUALS", value: "value"}
                        ]
                    }) {
                        id
                    }
                }
                """, i);
            
            graphQlTester.document(createMutation).execute();
        }
        
        // Query with pagination
        String query = """
            query {
                policies(page: 0, size: 2) {
                    content {
                        id
                        name
                    }
                    totalElements
                    totalPages
                    page
                    size
                }
            }
            """;
        
        graphQlTester.document(query)
                .execute()
                .path("policies.content").entityList(Object.class).hasSize(2)
            .path("policies.page").entity(Integer.class).isEqualTo(0)
            .path("policies.size").entity(Integer.class).isEqualTo(2)
            .path("policies.totalElements").entity(Integer.class).satisfies(total -> {
                assertThat(total).isGreaterThanOrEqualTo(3);
            });
    }
    
    @Test
    void shouldPublishPolicy() {
        // Create a policy
        String createMutation = """
            mutation {
                createPolicy(input: {
                    name: "Publishable Policy",
                    conditions: [
                        {field: "status", operator: "EQUALS", value: "active"}
                    ]
                }) {
                    id
                    status
                    version
                }
            }
            """;
        
        GraphQlTester.Response createResponse = graphQlTester.document(createMutation).execute();
        String policyId = createResponse.path("createPolicy.id").entity(String.class).get();
        
        createResponse
            .path("createPolicy.status").entity(String.class).isEqualTo("DRAFT")
            .path("createPolicy.version").entity(Integer.class).isEqualTo(1);
        
        // Publish it
        String publishMutation = """
            mutation($id: ID!) {
                publishPolicy(id: $id) {
                    id
                    status
                    version
                }
            }
            """;
        
        graphQlTester.document(publishMutation)
            .variable("id", policyId)
            .execute()
            .path("publishPolicy.id").entity(String.class).isEqualTo(policyId)
            .path("publishPolicy.status").entity(String.class).isEqualTo("PUBLISHED")
            .path("publishPolicy.version").entity(Integer.class).isEqualTo(2);
    }
    
    @Test
    void shouldReturnNotFoundErrorForNonExistentPolicy() {
        String query = """
            query {
                policy(id: "non-existent-id") {
                    id
                    name
                }
            }
            """;
        
        graphQlTester.document(query)
            .execute()
            .errors()
            .expect(error -> error.getErrorType().toString().equals("NOT_FOUND"));
    }
    
    @Test
    void shouldReturnValidationErrorForInvalidPolicy() {
        String mutation = """
            mutation {
                createPolicy(input: {
                    name: "",
                    conditions: []
                }) {
                    id
                }
            }
            """;
        
        graphQlTester.document(mutation)
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).isNotEmpty();
            });
    }
}
