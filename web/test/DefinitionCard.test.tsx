import { render, screen } from "@testing-library/react"
import { DefinitionCard } from "../components/DefinitionCard"
import { ChakraProvider } from "@chakra-ui/react"

describe("DefinitionCard", () => {
  it("renders with title, value, and subtitle", () => {
    render(
      <ChakraProvider>
        <DefinitionCard
          title="Test Title"
          value="42"
          subtitle="Test subtitle"
        />
      </ChakraProvider>
    )

    expect(screen.getByText("Test Title")).toBeInTheDocument()
    expect(screen.getByText("42")).toBeInTheDocument()
    expect(screen.getByText("Test subtitle")).toBeInTheDocument()
  })

  it("renders different values correctly", () => {
    render(
      <ChakraProvider>
        <DefinitionCard
          title="Total Workflows"
          value="100"
          subtitle="Active definitions"
        />
      </ChakraProvider>
    )

    expect(screen.getByText("Total Workflows")).toBeInTheDocument()
    expect(screen.getByText("100")).toBeInTheDocument()
    expect(screen.getByText("Active definitions")).toBeInTheDocument()
  })

  it("renders zero value", () => {
    render(
      <ChakraProvider>
        <DefinitionCard
          title="Total Policies"
          value="0"
          subtitle="No policies yet"
        />
      </ChakraProvider>
    )

    expect(screen.getByText("0")).toBeInTheDocument()
  })
})
