import { render, screen } from "@testing-library/react"
import { Card, Badge, Button } from "../components/UI"
import { ChakraProvider } from "@chakra-ui/react"

describe("UI Components", () => {
  describe("Card", () => {
    it("renders children correctly", () => {
      render(
        <ChakraProvider>
          <Card>
            <div>Card Content</div>
          </Card>
        </ChakraProvider>
      )

      expect(screen.getByText("Card Content")).toBeInTheDocument()
    })

    it("applies custom className", () => {
      const { container } = render(
        <ChakraProvider>
          <Card className="custom-class">
            <div>Content</div>
          </Card>
        </ChakraProvider>
      )

      const card = container.firstChild as HTMLElement
      expect(card.className).toContain("custom-class")
    })
  })

  describe("Badge", () => {
    it("renders DRAFT status correctly", () => {
      render(
        <ChakraProvider>
          <Badge status="DRAFT" />
        </ChakraProvider>
      )

      expect(screen.getByText("DRAFT")).toBeInTheDocument()
    })

    it("renders PUBLISHED status correctly", () => {
      render(
        <ChakraProvider>
          <Badge status="PUBLISHED" />
        </ChakraProvider>
      )

      expect(screen.getByText("PUBLISHED")).toBeInTheDocument()
    })

    it("renders ARCHIVED status correctly", () => {
      render(
        <ChakraProvider>
          <Badge status="ARCHIVED" />
        </ChakraProvider>
      )

      expect(screen.getByText("ARCHIVED")).toBeInTheDocument()
    })

    it("handles unknown status", () => {
      render(
        <ChakraProvider>
          <Badge status="UNKNOWN" />
        </ChakraProvider>
      )

      expect(screen.getByText("UNKNOWN")).toBeInTheDocument()
    })
  })

  describe("Button", () => {
    it("renders primary button", () => {
      render(
        <ChakraProvider>
          <Button variant="primary">Click me</Button>
        </ChakraProvider>
      )

      expect(screen.getByText("Click me")).toBeInTheDocument()
    })

    it("renders secondary button", () => {
      render(
        <ChakraProvider>
          <Button variant="secondary">Secondary</Button>
        </ChakraProvider>
      )

      expect(screen.getByText("Secondary")).toBeInTheDocument()
    })

    it("renders danger button", () => {
      render(
        <ChakraProvider>
          <Button variant="danger">Delete</Button>
        </ChakraProvider>
      )

      expect(screen.getByText("Delete")).toBeInTheDocument()
    })

    it("handles different sizes", () => {
      const { rerender } = render(
        <ChakraProvider>
          <Button size="sm">Small</Button>
        </ChakraProvider>
      )
      expect(screen.getByText("Small")).toBeInTheDocument()

      rerender(
        <ChakraProvider>
          <Button size="md">Medium</Button>
        </ChakraProvider>
      )
      expect(screen.getByText("Medium")).toBeInTheDocument()

      rerender(
        <ChakraProvider>
          <Button size="lg">Large</Button>
        </ChakraProvider>
      )
      expect(screen.getByText("Large")).toBeInTheDocument()
    })
  })
})
