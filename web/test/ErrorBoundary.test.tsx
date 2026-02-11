import { render, screen } from "@testing-library/react"
import { ErrorBoundary } from "../components/ErrorBoundary"

const ThrowError = () => {
  throw new Error("Test error")
}

describe("ErrorBoundary", () => {
  // Suppress console.error for these tests
  const originalError = console.error
  beforeAll(() => {
    console.error = jest.fn()
  })

  afterAll(() => {
    console.error = originalError
  })

  it("renders children when there is no error", () => {
    render(
      <ErrorBoundary>
        <div>Test Content</div>
      </ErrorBoundary>
    )

    expect(screen.getByText("Test Content")).toBeInTheDocument()
  })

  it("renders error message when child component throws", () => {
    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    )

    expect(screen.getByText("Something went wrong.")).toBeInTheDocument()
    expect(screen.getByText("Please refresh and try again.")).toBeInTheDocument()
  })

  it("logs error to console when error occurs", () => {
    const consoleSpy = jest.spyOn(console, "error")
    
    render(
      <ErrorBoundary>
        <ThrowError />
      </ErrorBoundary>
    )

    expect(consoleSpy).toHaveBeenCalled()
  })
})
