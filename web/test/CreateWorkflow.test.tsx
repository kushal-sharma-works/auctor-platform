import { describe, it, expect, vi, beforeEach } from "vitest"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { Providers } from "../components/Providers"
import CreateWorkflowPage from "../app/workflows/new/page"

const mockMutateAsync = vi.fn().mockResolvedValue({
  createWorkflow: { id: "wf-1", name: "Test", version: 1, status: "DRAFT" },
})

vi.mock("../hooks/useGraphql", () => ({
  useCreateWorkflow: () => ({
    mutateAsync: mockMutateAsync,
    isPending: false,
  }),
}))

describe("CreateWorkflow", () => {
  beforeEach(() => {
    mockMutateAsync.mockClear()
  })

  it("renders create workflow form", () => {
    render(
      <Providers>
        <CreateWorkflowPage />
      </Providers>
    )

    expect(
      screen.getByRole("heading", { name: /create workflow/i })
    ).toBeInTheDocument()
    expect(
      screen.getByRole("button", { name: /create workflow/i })
    ).toBeInTheDocument()
    expect(
      screen.getByPlaceholderText(/enter workflow name/i)
    ).toBeInTheDocument()
  })

  it("validates required fields on submit", async () => {
    const user = userEvent.setup()

    render(
      <Providers>
        <CreateWorkflowPage />
      </Providers>
    )

    const submitButton = screen.getByRole("button", { name: /create workflow/i })
    await user.click(submitButton)

    await waitFor(() => {
      expect(screen.getByText(/name is required/i)).toBeInTheDocument()
    })
  })

  it("shows dynamic state dropdowns after entering states", async () => {
    const user = userEvent.setup()

    render(
      <Providers>
        <CreateWorkflowPage />
      </Providers>
    )

    // Before entering states, there's an info alert
    expect(screen.getByText(/enter states first to select initial state/i)).toBeInTheDocument()

    // Type states into the input
    const statesInput = screen.getByPlaceholderText("PENDING, APPROVED, REJECTED")
    await user.type(statesInput, "PENDING, APPROVED")

    // Now the Select for initial state should appear
    await waitFor(() => {
      expect(screen.getByText("Select initial state")).toBeInTheDocument()
    })
  })

  it("submits form with valid data", async () => {
    const user = userEvent.setup()

    render(
      <Providers>
        <CreateWorkflowPage />
      </Providers>
    )

    // Fill in name
    await user.type(
      screen.getByPlaceholderText(/enter workflow name/i),
      "Test Workflow"
    )

    // Fill in states
    await user.type(
      screen.getByPlaceholderText("PENDING, APPROVED, REJECTED"),
      "PENDING, APPROVED"
    )

    // Wait for dropdowns to appear
    await waitFor(() => {
      expect(screen.getByText("Select initial state")).toBeInTheDocument()
    })

    // Select initial state
    const initialStateSelect = screen.getByText("Select initial state").closest("select")!
    await user.selectOptions(initialStateSelect, "PENDING")

    // Select from/to state for transition
    const fromSelect = screen.getByText("From State").closest("select")!
    await user.selectOptions(fromSelect, "PENDING")

    const toSelect = screen.getByText("To State").closest("select")!
    await user.selectOptions(toSelect, "APPROVED")

    // Submit
    const submitButton = screen.getByRole("button", { name: /create workflow/i })
    await user.click(submitButton)

    await waitFor(() => {
      expect(mockMutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({
          name: "Test Workflow",
          states: ["PENDING", "APPROVED"],
          initialState: "PENDING",
        })
      )
    })
  })

  it("shows cancel button", () => {
    render(
      <Providers>
        <CreateWorkflowPage />
      </Providers>
    )

    expect(
      screen.getByRole("button", { name: /cancel/i })
    ).toBeInTheDocument()
  })
})
