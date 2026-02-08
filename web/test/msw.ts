import { setupServer } from "msw/node"
import { http, HttpResponse } from "msw"

export const server = setupServer(
  // Handle both relative and absolute URLs
  http.post("*/api/graphql", async ({ request }) => {
    let body
    try {
      const clonedRequest = request.clone()
      body = await clonedRequest.json() as { operationName?: string; query: string; variables?: any }
    } catch (e) {
      return HttpResponse.json({
        errors: [{ message: "Invalid request body" }]
      })
    }
    
    const query = body.query || ""
    
    // List Workflows
    if (query.includes("workflows(")) {
      return HttpResponse.json({
        data: {
          workflows: {
            content: [
              {
                id: "workflow-1",
                name: "Order Approval Workflow",
                description: "Workflow for approving orders",
                version: 2,
                status: "PUBLISHED",
                states: ["PENDING", "APPROVED"],
                initialState: "PENDING",
                createdAt: "2024-01-01T00:00:00Z",
                updatedAt: "2024-01-01T00:00:00Z",
              },
              {
                id: "workflow-2",
                name: "Simple Workflow",
                description: "A simple workflow",
                version: 1,
                status: "DRAFT",
                states: ["START", "END"],
                initialState: "START",
                createdAt: "2024-01-02T00:00:00Z",
                updatedAt: "2024-01-02T00:00:00Z",
              },
            ],
            totalElements: 2,
            totalPages: 1,
            page: 0,
            size: 10,
          },
        },
      })
    }
    
    // Get Workflow by ID
    if (query.includes("workflow(")) {
      return HttpResponse.json({
        data: {
          workflow: {
            id: "workflow-1",
            name: "Order Approval Workflow",
            description: "Workflow for approving orders",
            version: 2,
            status: "PUBLISHED",
            states: ["PENDING", "APPROVED"],
            initialState: "PENDING",
            transitions: [
              {
                fromState: "PENDING",
                toState: "APPROVED",
                policyRef: "policy-1",
              },
            ],
            createdAt: "2024-01-01T00:00:00Z",
            updatedAt: "2024-01-01T00:00:00Z",
          },
        },
      })
    }
    
    // Create Workflow
    if (query.includes("createWorkflow")) {
      return HttpResponse.json({
        data: {
          createWorkflow: {
            id: "workflow-1",
            name: "Order Approval",
            version: 1,
            status: "DRAFT",
            states: ["PENDING", "APPROVED"],
            initialState: "PENDING",
            transitions: [],
            createdAt: "2024-01-01T00:00:00Z",
            updatedAt: "2024-01-01T00:00:00Z",
          },
        },
      })
    }
    
    // List Policies  
    if (query.includes("policies(")) {
      return HttpResponse.json({
        data: {
          policies: {
            content: [
              {
                id: "policy-1",
                name: "High Value Order Policy",
                description: "Requires approval for high-value orders",
                version: 2,
                status: "PUBLISHED",
                expression: "order.amount > 1000",
                conditions: [
                  { field: "amount", operator: "GREATER_THAN", value: "1000" },
                ],
                createdAt: "2024-01-01T00:00:00Z",
              },
            ],
            totalElements: 1,
            totalPages: 1,
            page: 0,
            size: 10,
          },
        },
      })
    }
    
    // Get Policy by ID
    if (query.includes("policy(")) {
      return HttpResponse.json({
        data: {
          policy: {
            id: "policy-1",
            name: "High Value Order Policy",
            description: "Requires approval for high-value orders",
            version: 2,
            status: "PUBLISHED",
            expression: "order.amount > 1000",
            conditions: [
              { field: "amount", operator: "GREATER_THAN", value: "1000" },
            ],
            createdAt: "2024-01-01T00:00:00Z",
          },
        },
      })
    }
    
    // Create Policy
    if (query.includes("createPolicy")) {
      return HttpResponse.json({
        data: {
          createPolicy: {
            id: "policy-1",
            name: "Test Policy",
            version: 1,
            status: "DRAFT",
            conditions: [
              { field: "order.amount", operator: "EQ", value: "1000" },
            ],
            createdAt: "2024-01-01T00:00:00Z",
          },
        },
      })
    }
    
    // Publish Policy
    if (query.includes("publishPolicy")) {
      return HttpResponse.json({
        data: {
          publishPolicy: {
            id: "policy-1",
            name: "Test Policy",
            version: 2,
            status: "PUBLISHED",
          },
        },
      })
    }
    
    // Publish Workflow
    if (query.includes("publishWorkflow")) {
      return HttpResponse.json({
        data: {
          publishWorkflow: {
            id: "workflow-1",
            name: "Order Approval",
            version: 2,
            status: "PUBLISHED",
          },
        },
      })
    }
    
    // Default fallback
    return HttpResponse.json({
      data: null,
      errors: [{ message: "Unhandled GraphQL operation" }]
    })
  })
)