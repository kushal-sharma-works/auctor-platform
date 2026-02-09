import { describe, it, expect, vi } from "vitest"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { Providers } from "../components/Providers"
import CreatePolicyPage from "../app/policies/new/page"

const mockMutateAsync = vi.fn().mockResolvedValue({
  createPolicy: { id: "policy-1", name: "Test", version: 1, status: "DRAFT" },
})

vi.mock("../hooks/useGraphql", () => ({
  useCreatePolicy: () => ({
    mutateAsync: mockMutateAsync,
    isPending: false,
  }),
}))

describe("CreatePolicy", () => {
  it("renders create policy form", () => {
    render(
      <Providers>
        <CreatePolicyPage />
      </Providers>
    )

    expect(
      screen.getByRole("heading", { name: /create policy/i })
    ).toBeInTheDocument()
    expect(
      screen.getByRole("button", { name: /create policy/i })
    ).toBeInTheDocument()
    expect(
      screen.getByPlaceholderText(/enter policy name/i)
    ).toBeInTheDocument()
  })

  it("validates required fields", async () => {
    const user = userEvent.setup()

    render(
      <Providers>
        <CreatePolicyPage />
      </Providers>
    )

    const submitButton = screen.getByRole("button", { name: /create policy/i })
    await user.click(submitButton)

    await waitFor(() => {
      expect(screen.getByText(/name is required/i)).toBeInTheDocument()
    })
  })

  it("submits form with valid data", async () => {
    const user = userEvent.setup()
    mockMutateAsync.mockClear()
    mockMutateAsync.mockResolvedValue({
      createPolicy: { id: "policy-1", name: "Test", version: 1, status: "DRAFT" },
    })

    render(
      <Providers>
        <CreatePolicyPage />
      </Providers>
    )

    // Fill in the name
    await user.type(screen.getByPlaceholderText(/enter policy name/i), "Test Policy")

    // Fill in condition fields
    await user.type(
      screen.getByPlaceholderText(/field \(e\.g\., order\.amount\)/i),
      "order.amount"
    )
    await user.type(screen.getByPlaceholderText("Value"), "1000")

    // Submit the form
    const submitButton = screen.getByRole("button", { name: /create policy/i })
    await user.click(submitButton)

    await waitFor(() => {
      expect(mockMutateAsync).toHaveBeenCalled()
    })
  })

  it("shows cancel button", () => {
    render(
      <Providers>
        <CreatePolicyPage />
      </Providers>
    )

    expect(
      screen.getByRole("button", { name: /cancel/i })
    ).toBeInTheDocument()
  })

  it("allows adding multiple conditions", async () => {
    const user = userEvent.setup()

    render(
      <Providers>
        <CreatePolicyPage />
      </Providers>
    )

    // Initially should have 1 condition row
    const fieldInputs = screen.getAllByPlaceholderText(
      /field \(e\.g\., order\.amount\)/i
    )
    expect(fieldInputs).toHaveLength(1)

    // Click "Add Condition" button
    const addButton = screen.getByRole("button", { name: /add condition/i })
    await user.click(addButton)

    // Now should have 2 condition rows
    await waitFor(() => {
      const updatedFieldInputs = screen.getAllByPlaceholderText(
        /field \(e\.g\., order\.amount\)/i
      )
      expect(updatedFieldInputs).toHaveLength(2)
    })
  })
})
