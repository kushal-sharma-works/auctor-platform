import {
  listExecutions,
  getExecution,
  getAuditTrail,
  startExecution,
  advanceExecution,
} from "../lib/executionApi"
import * as graphqlClient from "../graphql/client"

jest.mock("../graphql/client")

describe("executionApi", () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  describe("listExecutions", () => {
    it("fetches executions list successfully", async () => {
      const mockResponse = {
        listExecutions: {
          items: [
            {
              id: "exec-1",
              workflowId: "wf-1",
              workflowVersion: 1,
              currentState: "STARTED",
              status: { type: "RUNNING" },
              input: [],
              createdAt: "2024-01-01T00:00:00Z",
              updatedAt: "2024-01-01T00:00:00Z",
            },
          ],
          total: 1,
          limit: 20,
          offset: 0,
        },
      }

      ;(graphqlClient.graphqlRequest as jest.Mock).mockResolvedValue(mockResponse)

      const result = await listExecutions(20, 0, "test-token")

      expect(result).toEqual(mockResponse.listExecutions)
      expect(graphqlClient.graphqlRequest).toHaveBeenCalledWith(
        expect.any(String),
        "test-token",
        { limit: 20, offset: 0 }
      )
    })

    it("handles errors when listing executions", async () => {
      ;(graphqlClient.graphqlRequest as jest.Mock).mockRejectedValue(new Error("Network error"))

      await expect(listExecutions(20, 0, "test-token")).rejects.toThrow("Network error")
    })

    it("uses default parameters", async () => {
      const mockResponse = {
        listExecutions: {
          items: [],
          total: 0,
          limit: 20,
          offset: 0,
        },
      }

      ;(graphqlClient.graphqlRequest as jest.Mock).mockResolvedValue(mockResponse)

      await listExecutions()

      expect(graphqlClient.graphqlRequest).toHaveBeenCalledWith(
        expect.any(String),
        undefined,
        { limit: 20, offset: 0 }
      )
    })
  })

  describe("getExecution", () => {
    it("fetches single execution successfully", async () => {
      const mockResponse = {
        getExecution: {
          id: "exec-1",
          workflowId: "wf-1",
          workflowVersion: 1,
          currentState: "COMPLETED",
          status: { type: "SUCCESS" },
          input: [{ key: "param1", value: "value1" }],
          createdAt: "2024-01-01T00:00:00Z",
          updatedAt: "2024-01-01T00:00:00Z",
        },
      }

      ;(graphqlClient.graphqlRequest as jest.Mock).mockResolvedValue(mockResponse)

      const result = await getExecution("exec-1", "test-token")

      expect(result).toEqual(mockResponse.getExecution)
      expect(graphqlClient.graphqlRequest).toHaveBeenCalledWith(
        expect.any(String),
        "test-token",
        { id: "exec-1" }
      )
    })

    it("handles errors when getting execution", async () => {
      ;(graphqlClient.graphqlRequest as jest.Mock).mockRejectedValue(new Error("Not found"))

      await expect(getExecution("exec-1", "test-token")).rejects.toThrow("Not found")
    })
  })

  describe("getAuditTrail", () => {
    it("fetches audit trail successfully", async () => {
      const mockResponse = {
        getAuditTrail: [
          {
            id: "1",
            executionId: "exec-1",
            eventType: "STARTED",
            actor: "system",
            details: "Execution started",
            timestamp: "2024-01-01T00:00:00Z",
          },
        ],
      }

      ;(graphqlClient.graphqlRequest as jest.Mock).mockResolvedValue(mockResponse)

      const result = await getAuditTrail("exec-1", "test-token")

      expect(result).toEqual(mockResponse.getAuditTrail)
      expect(graphqlClient.graphqlRequest).toHaveBeenCalledWith(
        expect.any(String),
        "test-token",
        { executionId: "exec-1" }
      )
    })

    it("handles errors when getting audit trail", async () => {
      ;(graphqlClient.graphqlRequest as jest.Mock).mockRejectedValue(new Error("Fetch failed"))

      await expect(getAuditTrail("exec-1", "test-token")).rejects.toThrow("Fetch failed")
    })
  })

  describe("startExecution", () => {
    it("starts execution successfully", async () => {
      const mockResponse = {
        startExecution: {
          id: "exec-new",
          workflowId: "wf-1",
          workflowVersion: 1,
          currentState: "STARTED",
          status: { type: "RUNNING" },
          input: [{ key: "param1", value: "value1" }],
          createdAt: "2024-01-01T00:00:00Z",
          updatedAt: "2024-01-01T00:00:00Z",
        },
      }

      ;(graphqlClient.graphqlRequest as jest.Mock).mockResolvedValue(mockResponse)

      const request = {
        workflowId: "wf-1",
        workflowVersion: 1,
        input: { param1: "value1" },
      }

      const result = await startExecution(request, "test-token")

      expect(result).toEqual(mockResponse.startExecution)
      expect(graphqlClient.graphqlRequest).toHaveBeenCalled()
    })

    it("handles errors when starting execution", async () => {
      ;(graphqlClient.graphqlRequest as jest.Mock).mockRejectedValue(new Error("Start failed"))

      const request = {
        workflowId: "wf-1",
        workflowVersion: 1,
        input: {},
      }

      await expect(startExecution(request, "test-token")).rejects.toThrow("Start failed")
    })
  })

  describe("advanceExecution", () => {
    it("advances execution successfully", async () => {
      const mockResponse = {
        advanceExecution: {
          id: "exec-1",
          workflowId: "wf-1",
          workflowVersion: 1,
          currentState: "COMPLETED",
          status: { type: "SUCCESS" },
          input: [],
          createdAt: "2024-01-01T00:00:00Z",
          updatedAt: "2024-01-01T00:00:00Z",
        },
      }

      ;(graphqlClient.graphqlRequest as jest.Mock).mockResolvedValue(mockResponse)

      const result = await advanceExecution("exec-1", { correlationId: "corr-1" }, "test-token")

      expect(result).toEqual(mockResponse.advanceExecution)
      expect(graphqlClient.graphqlRequest).toHaveBeenCalled()
    })

    it("advances execution without optional request parameter", async () => {
      const mockResponse = {
        advanceExecution: {
          id: "exec-1",
          workflowId: "wf-1",
          workflowVersion: 1,
          currentState: "COMPLETED",
          status: { type: "SUCCESS" },
          input: [],
          createdAt: "2024-01-01T00:00:00Z",
          updatedAt: "2024-01-01T00:00:00Z",
        },
      }

      ;(graphqlClient.graphqlRequest as jest.Mock).mockResolvedValue(mockResponse)

      const result = await advanceExecution("exec-1", undefined, "test-token")

      expect(result).toEqual(mockResponse.advanceExecution)
    })

    it("handles errors when advancing execution", async () => {
      ;(graphqlClient.graphqlRequest as jest.Mock).mockRejectedValue(new Error("Advance failed"))

      await expect(advanceExecution("exec-1", undefined, "test-token")).rejects.toThrow(
        "Advance failed"
      )
    })
  })
})
