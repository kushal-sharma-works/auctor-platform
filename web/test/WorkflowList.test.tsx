import { describe, it, expect, vi } from "vitest"
import { render, screen } from "@testing-library/react"
import { Providers } from "../components/Providers"
import WorkflowsPage from "../app/workflows/page"

vi.mock("../hooks/useGraphql", () => ({
  useWorkflows: () => ({
    data: {
      workflows: {
        content: [
          {
            id: "workflow-1",
            name: "Order Approval Workflow",
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
    isLoading: false,
    error: null,
  }),
}))

describe("WorkflowList", () => {
  it("renders workflow list with data", () => {
    render(
      <Providers>
        <WorkflowsPage />
      </Providers>
    )

    expect(screen.getByText("Order Approval Workflow")).toBeInTheDocument()
    expect(screen.getByText("Simple Workflow")).toBeInTheDocument()
    expect(screen.getByRole("heading", { name: /workflows/i })).toBeInTheDocument()
  })

  it("shows create workflow button", () => {
    render(
      <Providers>
        <WorkflowsPage />
      </Providers>
    )

    expect(screen.getByText("Create Workflow")).toBeInTheDocument()
  })

  it("shows workflow details in table", () => {
    render(
      <Providers>
        <WorkflowsPage />
      </Providers>
    )

    expect(screen.getByText("Order Approval Workflow")).toBeInTheDocument()
    expect(screen.getByText("Simple Workflow")).toBeInTheDocument()
    // Status badges render the raw status string
    expect(screen.getByText("PUBLISHED")).toBeInTheDocument()
    expect(screen.getByText("DRAFT")).toBeInTheDocument()
  })
})
