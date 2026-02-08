import "@testing-library/jest-dom/vitest"
import { createElement, type ReactNode } from "react"
import { afterAll, afterEach, beforeAll, vi } from "vitest"
import { server } from "./msw"

// Establish API mocking before all tests
beforeAll(() => server.listen({ onUnhandledRequest: "error" }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

export const mockRouter = {
  push: vi.fn(),
  back: vi.fn(),
  replace: vi.fn(),
  prefetch: vi.fn(),
}

vi.mock("next/navigation", () => ({
  usePathname: () => "/",
  useRouter: () => mockRouter,
  useParams: () => ({ id: "workflow-1" }),
}))

vi.mock("next/link", () => ({
  default: ({ href, children, ...props }: { href: string; children: ReactNode }) =>
    createElement('a', { href, ...props }, children),
}))

// Mock Redux store with a default token
vi.mock("react-redux", async () => {
  const actual = await vi.importActual("react-redux")
  return {
    ...actual,
    useSelector: vi.fn((selector) => {
      // Mock state with a token
      const mockState = {
        session: {
          token: "mock-token-123"
        }
      }
      return selector(mockState)
    }),
    useDispatch: () => vi.fn(),
  }
})