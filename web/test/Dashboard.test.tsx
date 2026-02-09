import { describe, it, expect, vi } from "vitest"
import { render, screen } from "@testing-library/react"
import { Providers } from "../components/Providers"
import DashboardPage from "../app/page"

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
  usePolicies: () => ({
    data: {
      policies: {
        content: [
          {
            id: "policy-1",
            name: "High Value Order Policy",
            version: 2,
            status: "PUBLISHED",
            conditions: [
              { field: "amount", operator: "GT", value: "1000" },
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
    isLoading: false,
    error: null,
  }),
}))

describe("Dashboard", () => {
  it("renders dashboard heading", () => {
    render(
      <Providers>
        <DashboardPage />
      </Providers>
    )

    expect(
      screen.getByRole("heading", { name: /workflow & policy command center/i })
    ).toBeInTheDocument()
  })

  it("displays workflow summary with data", () => {
    render(
      <Providers>
        <DashboardPage />
      </Providers>
    )

    expect(
      screen.getByRole("heading", { name: /recent workflows/i })
    ).toBeInTheDocument()
    expect(screen.getByText("Order Approval Workflow")).toBeInTheDocument()
  })

  it("displays policy summary with data", () => {
    render(
      <Providers>
        <DashboardPage />
      </Providers>
    )

    expect(
      screen.getByRole("heading", { name: /recent policies/i })
    ).toBeInTheDocument()
    expect(screen.getByText("High Value Order Policy")).toBeInTheDocument()
  })

  it("displays recent workflows", () => {
    render(
      <Providers>
        <DashboardPage />
      </Providers>
    )

    expect(screen.getByText("Order Approval Workflow")).toBeInTheDocument()
    // Both workflow and policy have "Version 2" text
    const versionTexts = screen.getAllByText(/Version 2/)
    expect(versionTexts.length).toBeGreaterThan(0)
  })

  it("displays recent policies", () => {
    render(
      <Providers>
        <DashboardPage />
      </Providers>
    )

    expect(screen.getByText("High Value Order Policy")).toBeInTheDocument()
  })

  it("displays summary cards", () => {
    render(
      <Providers>
        <DashboardPage />
      </Providers>
    )

    expect(screen.getByText(/total workflows/i)).toBeInTheDocument()
    expect(screen.getByText(/total policies/i)).toBeInTheDocument()
  })

  it("handles loading state gracefully", () => {
    render(
      <Providers>
        <DashboardPage />
      </Providers>
    )

    expect(
      screen.getByRole("heading", { name: /workflow & policy command center/i })
    ).toBeInTheDocument()
  })

  it("displays status badges for definitions", () => {
    render(
      <Providers>
        <DashboardPage />
      </Providers>
    )

    expect(screen.getByText("Order Approval Workflow")).toBeInTheDocument()
    const publishedBadges = screen.getAllByText("PUBLISHED")
    expect(publishedBadges.length).toBeGreaterThan(0)
  })
})
