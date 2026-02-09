package com.auctor.definition.api.graphql;

import com.auctor.definition.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.graphql.test.tester.GraphQlTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Workflow GraphQL API.
 * Tests all queries and mutations with various scenarios.
 */
@AutoConfigureGraphQlTester
class WorkflowGraphQLControllerIT extends IntegrationTestBase {
    
    @Autowired
    private GraphQlTester graphQlTester;
    
    @Test
    void shouldCreateWorkflow() {
        String mutation = """
            mutation {
                createWorkflow(input: {
                    name: "Order Approval",
                    states: ["PENDING", "APPROVED", "REJECTED"],
                    initialState: "PENDING",
                    transitions: [
                        {
                            fromState: "PENDING",
                            toState: "APPROVED",
                            policyRef: "approval-policy"
                        },
                        {
                            fromState: "PENDING",
                            toState: "REJECTED"
                        }
                    ]
                }) {
                    id
                    name
                    version
                    status
                    states
                    initialState
                    transitions {
                        fromState
                        toState
                        policyRef
                    }
                    createdAt
                    updatedAt
                }
            }
            """;
        
        graphQlTester.document(mutation)
            .execute()
            .path("createWorkflow")
            .entity(Object.class)
            .satisfies(workflow -> {
                assertThat(workflow).isNotNull();
            })
            .path("createWorkflow.id").entity(String.class).satisfies(id -> {
                assertThat(id).isNotNull();
            })
            .path("createWorkflow.name").entity(String.class).isEqualTo("Order Approval")
            .path("createWorkflow.version").entity(Integer.class).isEqualTo(1)
            .path("createWorkflow.status").entity(String.class).isEqualTo("DRAFT")
            .path("createWorkflow.states").entityList(String.class).hasSize(3)
            .path("createWorkflow.initialState").entity(String.class).isEqualTo("PENDING")
            .path("createWorkflow.transitions").entityList(Object.class).hasSize(2);
    }
    
    @Test
    void shouldGetWorkflowById() {
        // First create a workflow
        String createMutation = """
            mutation {
                createWorkflow(input: {
                    name: "Simple Workflow",
                    states: ["START", "END"],
                    initialState: "START",
                    transitions: [
                        {
                            fromState: "START",
                            toState: "END"
                        }
                    ]
                }) {
                    id
                }
            }
            """;
        
        String workflowId = graphQlTester.document(createMutation)
            .execute()
            .path("createWorkflow.id")
            .entity(String.class)
            .get();
        
        // Then query it
        String query = """
            query($id: ID!) {
                workflow(id: $id) {
                    id
                    name
                    version
                    status
                    states
                    initialState
                }
            }
            """;
        
        graphQlTester.document(query)
            .variable("id", workflowId)
            .execute()
            .path("workflow.id").entity(String.class).isEqualTo(workflowId)
            .path("workflow.name").entity(String.class).isEqualTo("Simple Workflow")
            .path("workflow.status").entity(String.class).isEqualTo("DRAFT");
    }
    
    @Test
    void shouldListWorkflowsWithPagination() {
        // Create multiple workflows
        for (int i = 0; i < 3; i++) {
            String createMutation = String.format("""
                mutation {
                    createWorkflow(input: {
                        name: "Workflow %d",
                        states: ["A", "B"],
                        initialState: "A",
                        transitions: [{fromState: "A", toState: "B"}]
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
                workflows(page: 0, size: 2) {
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
                .path("workflows.content").entityList(Object.class).hasSize(2)
            .path("workflows.page").entity(Integer.class).isEqualTo(0)
            .path("workflows.size").entity(Integer.class).isEqualTo(2)
            .path("workflows.totalElements").entity(Integer.class).satisfies(total -> {
                assertThat(total).isGreaterThanOrEqualTo(3);
            });
    }
    
    @Test
    void shouldPublishWorkflow() {
        // Create a workflow
        String createMutation = """
            mutation {
                createWorkflow(input: {
                    name: "Publishable Workflow",
                    states: ["START", "END"],
                    initialState: "START",
                    transitions: [{fromState: "START", toState: "END"}]
                }) {
                    id
                    status
                    version
                }
            }
            """;
        
        GraphQlTester.Response createResponse = graphQlTester.document(createMutation).execute();
        String workflowId = createResponse.path("createWorkflow.id").entity(String.class).get();
        
        createResponse
            .path("createWorkflow.status").entity(String.class).isEqualTo("DRAFT")
            .path("createWorkflow.version").entity(Integer.class).isEqualTo(1);
        
        // Publish it
        String publishMutation = """
            mutation($id: ID!) {
                publishWorkflow(id: $id) {
                    id
                    status
                    version
                }
            }
            """;
        
        graphQlTester.document(publishMutation)
            .variable("id", workflowId)
            .execute()
            .path("publishWorkflow.id").entity(String.class).isEqualTo(workflowId)
            .path("publishWorkflow.status").entity(String.class).isEqualTo("PUBLISHED")
            .path("publishWorkflow.version").entity(Integer.class).isEqualTo(2);
    }
    
    @Test
    void shouldReturnNotFoundErrorForNonExistentWorkflow() {
        String query = """
            query {
                workflow(id: "non-existent-id") {
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
    void shouldReturnValidationErrorForInvalidWorkflow() {
        String mutation = """
            mutation {
                createWorkflow(input: {
                    name: "",
                    states: [],
                    initialState: "START",
                    transitions: []
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
