import { render, screen } from "@testing-library/react"
import { DefinitionCard } from "../components/DefinitionCard"

test("renders definition card", () => {
  render(
    <DefinitionCard
      title="Total Workflows"
      value="12"
      subtitle="Active workflows"
    />
  )

  expect(screen.getByText("Total Workflows")).toBeInTheDocument()
  expect(screen.getByText("12")).toBeInTheDocument()
  expect(screen.getByText("Active workflows")).toBeInTheDocument()
})
