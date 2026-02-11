import { render, screen, waitFor } from "@testing-library/react"
import DashboardPage from "../app/page"
import { Provider } from "react-redux"
import { store } from "../store"
import { setSession } from "../store/sessionSlice"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { ChakraProvider } from "@chakra-ui/react"

// Mock the useGraphql hooks
jest.mock("../hooks/useGraphql", () => ({
  useWorkflows: jest.fn(),
  usePolicies: jest.fn(),
}))

const { useWorkflows, usePolicies } = require("../hooks/useGraphql")

describe("DashboardPage", () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  })

  beforeEach(() => {
    store.dispatch(setSession({ token: "Bearer test", subject: "tester", roles: ["VIEWER"] }))
    
    // Mock the useWorkflows hook
    useWorkflows.mockReturnValue({
      data: {
        workflows: {
          content: [
            {
              id: "123",
              name: "Test Workflow",
              version: 1,
              states: ["DRAFT", "APPROVED"],
              status: "DRAFT",
              description: "desc"
            }
          ],
          totalElements: 1
        }
      },
      isLoading: false,
      error: null,
    })

    // Mock the usePolicies hook
    usePolicies.mockReturnValue({
      data: {
        policies: {
          content: [
            {
              id: "456",
              name: "Test Policy",
              version: 1,
              status: "DRAFT",
              conditions: []
            }
          ],
          totalElements: 1
        }
      },
      isLoading: false,
      error: null,
    })
  })

  afterEach(() => {
    jest.clearAllMocks()
  })

  it("renders dashboard with workflows and policies", async () => {
    render(
      <ChakraProvider>
        <Provider store={store}>
          <QueryClientProvider client={queryClient}>
            <DashboardPage />
          </QueryClientProvider>
        </Provider>
      </ChakraProvider>
    )

    // Wait for the content to appear
    await waitFor(() => {
      expect(screen.getByText("Workflow & Policy Command Center")).toBeInTheDocument()
    })

    // Check for the workflow name
    await waitFor(() => {
      expect(screen.getByText("Test Workflow")).toBeInTheDocument()
    })
  })

  it("shows loading state initially", async () => {
    // Mock loading state
    useWorkflows.mockReturnValue({
      data: undefined,
      isLoading: true,
      error: null,
    })

    usePolicies.mockReturnValue({
      data: undefined,
      isLoading: true,
      error: null,
    })

    render(
      <ChakraProvider>
        <Provider store={store}>
          <QueryClientProvider client={queryClient}>
            <DashboardPage />
          </QueryClientProvider>
        </Provider>
      </ChakraProvider>
    )

    expect(screen.getByText("Loading dashboard…")).toBeInTheDocument()
  })
})
