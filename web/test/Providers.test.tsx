import { render, screen } from "@testing-library/react"
import { Providers } from "../components/Providers"

test("renders providers children", () => {
  render(
    <Providers>
      <div>Provider Content</div>
    </Providers>
  )

  expect(screen.getByText("Provider Content")).toBeInTheDocument()
})
