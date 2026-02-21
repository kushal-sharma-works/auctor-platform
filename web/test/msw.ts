import { setupServer } from "msw/node"
import { graphql, HttpResponse } from "msw"

export const server = setupServer(
  graphql.query("getDefinition", () => {
    return HttpResponse.json({
      data: {
        getDefinition: {
          id: "123",
          name: "mock",
          description: "desc"
        }
      }
    })
  }),
  graphql.query("ListExecutions", () => {
    return HttpResponse.json({
      data: {
        listExecutions: {
          limit: 10,
          offset: 0,
          total: 1,
          items: [
            {
              id: "exec-1",
              workflowId: "workflow-1",
              workflowVersion: 1,
              currentState: "STARTED",
              status: {
                type: "RUNNING",
                reason: null
              },
              input: [
                { key: "param1", value: "value1" }
              ],
              createdAt: "2024-01-01T00:00:00Z",
              updatedAt: "2024-01-01T00:00:00Z"
            }
          ]
        }
      }
    })
  }),
  graphql.query("GetExecution", () => {
    return HttpResponse.json({
      data: {
        getExecution: {
          id: "exec-1",
          workflowId: "workflow-1",
          workflowVersion: 1,
          currentState: "STARTED",
          status: {
            type: "RUNNING",
            reason: null
          },
          input: [
            { key: "param1", value: "value1" }
          ],
          auditEvents: [
            {
              id: "audit-1",
              executionId: "exec-1",
              eventType: "EXECUTION_STARTED",
              actor: "system",
              details: "Execution started",
              timestamp: "2024-01-01T00:00:00Z"
            }
          ],
          createdAt: "2024-01-01T00:00:00Z",
          updatedAt: "2024-01-01T00:00:00Z"
        }
      }
    })
  }),
  graphql.query("GetAuditTrail", () => {
    return HttpResponse.json({
      data: {
        getAuditTrail: [
          {
            id: "audit-1",
            executionId: "exec-1",
            eventType: "EXECUTION_STARTED",
            actor: "system",
            details: "Execution started",
            timestamp: "2024-01-01T00:00:00Z"
          }
        ]
      }
    })
  }),
  graphql.mutation("StartExecution", () => {
    return HttpResponse.json({
      data: {
        startExecution: {
          id: "exec-new",
          workflowId: "workflow-1",
          workflowVersion: 1,
          currentState: "STARTED",
          status: {
            type: "RUNNING",
            reason: null
          },
          input: [
            { key: "param1", value: "value1" }
          ],
          createdAt: "2024-01-01T00:00:00Z",
          updatedAt: "2024-01-01T00:00:00Z"
        }
      }
    })
  }),
  graphql.mutation("AdvanceExecution", () => {
    return HttpResponse.json({
      data: {
        advanceExecution: {
          id: "exec-1",
          workflowId: "workflow-1",
          workflowVersion: 1,
          currentState: "NEXT_STATE",
          status: {
            type: "RUNNING",
            reason: null
          },
          input: [
            { key: "param1", value: "value1" }
          ],
          createdAt: "2024-01-01T00:00:00Z",
          updatedAt: "2024-01-01T00:00:00Z"
        }
      }
    })
  })
)
