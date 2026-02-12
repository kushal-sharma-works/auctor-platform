import { renderHook, waitFor } from "@testing-library/react"
import { Provider } from "react-redux"
import { store } from "../store"
import { setSession } from "../store/sessionSlice"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import {
  useExecutions,
  useExecution,
  useExecutionAuditTrail,
  useStartExecution,
  useAdvanceExecution,
} from "../hooks/useExecution"
import * as executionApi from "../lib/executionApi"

jest.mock("../lib/executionApi")

describe("useExecution hooks", () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  })

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </Provider>
  )

  beforeEach(() => {
    jest.clearAllMocks()
    queryClient.clear()
    store.dispatch(setSession({ token: "test-token", subject: "test-user", roles: ["VIEWER"] }))
  })

  describe("useExecutions", () => {
    it("fetches executions list", async () => {
      const mockExecutions = {
        items: [
          { id: "exec-1", workflowId: "wf-1", status: { type: "RUNNING" } },
        ],
        total: 1,
        limit: 10,
        offset: 0,
      }

      ;(executionApi.listExecutions as jest.Mock).mockResolvedValue(mockExecutions)

      const { result } = renderHook(() => useExecutions(0, 10), { wrapper })

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true)
      })

      expect(result.current.data).toEqual(mockExecutions)
      expect(executionApi.listExecutions).toHaveBeenCalledWith(10, 0, "test-token")
    })

    it("does not fetch when token is missing", () => {
      store.dispatch(setSession({ token: "", subject: "", roles: [] }))

      const { result } = renderHook(() => useExecutions(0, 10), { wrapper })

      expect(result.current.isFetching).toBe(false)
      expect(executionApi.listExecutions).not.toHaveBeenCalled()
    })
  })

  describe("useExecution", () => {
    it("fetches single execution", async () => {
      const mockExecution = {
        id: "exec-1",
        workflowId: "wf-1",
        status: { type: "COMPLETED" },
      }

      ;(executionApi.getExecution as jest.Mock).mockResolvedValue(mockExecution)

      const { result } = renderHook(() => useExecution("exec-1"), { wrapper })

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true)
      })

      expect(result.current.data).toEqual(mockExecution)
      expect(executionApi.getExecution).toHaveBeenCalledWith("exec-1", "test-token")
    })

    it("polls when execution is running", async () => {
      const mockExecution = {
        id: "exec-1",
        workflowId: "wf-1",
        status: { type: "RUNNING" },
      }

      ;(executionApi.getExecution as jest.Mock).mockResolvedValue(mockExecution)

      const { result } = renderHook(() => useExecution("exec-1"), { wrapper })

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true)
      })

      expect(result.current.data).toEqual(mockExecution)
    })
  })

  describe("useExecutionAuditTrail", () => {
    it("fetches audit trail", async () => {
      const mockAuditTrail = [
        { id: "1", eventType: "STARTED", timestamp: "2024-01-01T00:00:00Z" },
      ]

      ;(executionApi.getAuditTrail as jest.Mock).mockResolvedValue(mockAuditTrail)

      const { result } = renderHook(() => useExecutionAuditTrail("exec-1"), { wrapper })

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true)
      })

      expect(result.current.data).toEqual(mockAuditTrail)
      expect(executionApi.getAuditTrail).toHaveBeenCalledWith("exec-1", "test-token")
    })
  })

  describe("useStartExecution", () => {
    it("starts execution successfully", async () => {
      const mockResponse = { id: "exec-1", workflowId: "wf-1" }
      ;(executionApi.startExecution as jest.Mock).mockResolvedValue(mockResponse)

      const { result } = renderHook(() => useStartExecution(), { wrapper })

      const request = {
        workflowId: "wf-1",
        workflowVersion: 1,
        input: {},
      }

      await result.current.mutateAsync(request)

      expect(executionApi.startExecution).toHaveBeenCalledWith(request, "test-token")
    })
  })

  describe("useAdvanceExecution", () => {
    it("advances execution successfully", async () => {
      const mockResponse = { id: "exec-1", status: { type: "COMPLETED" } }
      ;(executionApi.advanceExecution as jest.Mock).mockResolvedValue(mockResponse)

      const { result } = renderHook(() => useAdvanceExecution(), { wrapper })

      await result.current.mutateAsync({
        executionId: "exec-1",
        request: { correlationId: "corr-1" },
      })

      expect(executionApi.advanceExecution).toHaveBeenCalledWith(
        "exec-1",
        { correlationId: "corr-1" },
        "test-token"
      )
    })
  })
})
