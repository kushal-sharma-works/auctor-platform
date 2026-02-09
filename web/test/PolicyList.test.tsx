import { describe, it, expect, vi } from "vitest"
import { render, screen } from "@testing-library/react"
import { Providers } from "../components/Providers"
import PolicyList from "../app/policies/page"

vi.mock("../hooks/useGraphql", () => ({
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

describe("PolicyList", () => {
  it("renders policy list with data", () => {
    render(
      <Providers>
        <PolicyList />
      </Providers>
    )

    expect(screen.getByText("High Value Order Policy")).toBeInTheDocument()
    expect(screen.getByText("PUBLISHED")).toBeInTheDocument()
  })

  it("shows create policy button", () => {
    render(
      <Providers>
        <PolicyList />
      </Providers>
    )

    expect(screen.getByText("Create Policy")).toBeInTheDocument()
  })

  it("shows policy status badges correctly", () => {
    render(
      <Providers>
        <PolicyList />
      </Providers>
    )

    expect(screen.getByText("PUBLISHED")).toBeInTheDocument()
  })

  it("displays policy metadata", () => {
    render(
      <Providers>
        <PolicyList />
      </Providers>
    )

    expect(screen.getByText("High Value Order Policy")).toBeInTheDocument()
    expect(screen.getByText("1 conditions")).toBeInTheDocument()
  })
})
