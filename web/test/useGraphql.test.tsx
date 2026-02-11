import { renderHook, waitFor } from "@testing-library/react"
import { Provider } from "react-redux"
import { store } from "../store"
import { setSession } from "../store/sessionSlice"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import {
  useWorkflows,
  useWorkflow,
  useCreateWorkflow,
  usePublishWorkflow,
  usePolicies,
  usePolicy,
  useCreatePolicy,
} from "../hooks/useGraphql"
import * as graphqlClient from "../graphql/client"

jest.mock("../graphql/client")

describe("useGraphql hooks", () => {
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

  describe("useWorkflows", () => {
    it("fetches workflows list", async () => {
      const mockData = {
        workflows: {
          content: [{ id: "1", name: "Test Workflow" }],
          totalElements: 1,
        },
      }

      ;(graphqlClient.requestDefinitionGraphQL as jest.Mock).mockResolvedValue(mockData)

      const { result } = renderHook(() => useWorkflows(0, 10), { wrapper })

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true)
      })

      expect(result.current.data).toEqual(mockData)
    })
  })

  describe("useWorkflow", () => {
    it("fetches single workflow", async () => {
      const mockData = {
        workflow: { id: "1", name: "Test Workflow" },
      }

      ;(graphqlClient.requestDefinitionGraphQL as jest.Mock).mockResolvedValue(mockData)

      const { result } = renderHook(() => useWorkflow("1"), { wrapper })

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true)
      })

      expect(result.current.data).toEqual(mockData)
    })

    it("does not fetch when id is empty", () => {
      const { result } = renderHook(() => useWorkflow(""), { wrapper })

      expect(result.current.isFetching).toBe(false)
      expect(graphqlClient.requestDefinitionGraphQL).not.toHaveBeenCalled()
    })
  })

  describe("useCreateWorkflow", () => {
    it("creates workflow successfully", async () => {
      const mockResponse = { createWorkflow: { id: "1", name: "New Workflow" } }
      ;(graphqlClient.requestDefinitionGraphQL as jest.Mock).mockResolvedValue(mockResponse)

      const { result } = renderHook(() => useCreateWorkflow(), { wrapper })

      const input = { name: "New Workflow", states: ["DRAFT"] }
      await result.current.mutateAsync(input)

      expect(graphqlClient.requestDefinitionGraphQL).toHaveBeenCalled()
    })
  })

  describe("usePublishWorkflow", () => {
    it("publishes workflow successfully", async () => {
      const mockResponse = { publishWorkflow: { id: "1", status: "PUBLISHED" } }
      ;(graphqlClient.requestDefinitionGraphQL as jest.Mock).mockResolvedValue(mockResponse)

      const { result } = renderHook(() => usePublishWorkflow(), { wrapper })

      await result.current.mutateAsync("1")

      expect(graphqlClient.requestDefinitionGraphQL).toHaveBeenCalled()
    })
  })

  describe("usePolicies", () => {
    it("fetches policies list", async () => {
      const mockData = {
        policies: {
          content: [{ id: "1", name: "Test Policy" }],
          totalElements: 1,
        },
      }

      ;(graphqlClient.requestDefinitionGraphQL as jest.Mock).mockResolvedValue(mockData)

      const { result } = renderHook(() => usePolicies(0, 10), { wrapper })

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true)
      })

      expect(result.current.data).toEqual(mockData)
    })
  })

  describe("usePolicy", () => {
    it("fetches single policy", async () => {
      const mockData = {
        policy: { id: "1", name: "Test Policy" },
      }

      ;(graphqlClient.requestDefinitionGraphQL as jest.Mock).mockResolvedValue(mockData)

      const { result } = renderHook(() => usePolicy("1"), { wrapper })

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true)
      })

      expect(result.current.data).toEqual(mockData)
    })
  })

  describe("useCreatePolicy", () => {
    it("creates policy successfully", async () => {
      const mockResponse = { createPolicy: { id: "1", name: "New Policy" } }
      ;(graphqlClient.requestDefinitionGraphQL as jest.Mock).mockResolvedValue(mockResponse)

      const { result } = renderHook(() => useCreatePolicy(), { wrapper })

      const input = { name: "New Policy", conditions: [] }
      await result.current.mutateAsync(input)

      expect(graphqlClient.requestDefinitionGraphQL).toHaveBeenCalled()
    })
  })
})
