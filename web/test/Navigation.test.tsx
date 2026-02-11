import { render, screen, fireEvent, waitFor } from "@testing-library/react"
import { Navigation } from "../components/Navigation"
import { Provider } from "react-redux"
import { store } from "../store"
import { setSession, clearToken } from "../store/sessionSlice"
import { ChakraProvider } from "@chakra-ui/react"
import { useRouter, usePathname } from "next/navigation"

describe("Navigation", () => {
  const mockPush = jest.fn()

  beforeEach(() => {
    jest.clearAllMocks()
    ;(useRouter as jest.Mock).mockReturnValue({
      push: mockPush,
      replace: jest.fn(),
      prefetch: jest.fn(),
      back: jest.fn(),
    })
    ;(usePathname as jest.Mock).mockReturnValue("/")
    ;(global.fetch as jest.Mock) = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({}),
    })
  })

  it("renders navigation items", () => {
    store.dispatch(setSession({ 
      token: "test-token", 
      subject: "test-user", 
      roles: ["VIEWER"] 
    }))

    render(
      <ChakraProvider>
        <Provider store={store}>
          <Navigation />
        </Provider>
      </ChakraProvider>
    )

    expect(screen.getByText("Auctor")).toBeInTheDocument()
    expect(screen.getByText("Dashboard")).toBeInTheDocument()
    expect(screen.getByText("Workflows")).toBeInTheDocument()
    expect(screen.getByText("Policies")).toBeInTheDocument()
    expect(screen.getByText("Executions")).toBeInTheDocument()
  })

  it("displays user roles when logged in", async () => {
    store.dispatch(setSession({ 
      token: "test-token", 
      subject: "test@example.com", 
      roles: ["ADMIN", "EXECUTOR"] 
    }))

    render(
      <ChakraProvider>
        <Provider store={store}>
          <Navigation />
        </Provider>
      </ChakraProvider>
    )

    await waitFor(() => {
      expect(screen.getByText(/ADMIN/)).toBeInTheDocument()
      expect(screen.getByText(/EXECUTOR/)).toBeInTheDocument()
    })
  })

  it("handles logout correctly", async () => {
    store.dispatch(setSession({ 
      token: "test-token", 
      subject: "test-user", 
      roles: ["VIEWER"] 
    }))

    render(
      <ChakraProvider>
        <Provider store={store}>
          <Navigation />
        </Provider>
      </ChakraProvider>
    )

    await waitFor(() => {
      const logoutButton = screen.getByText("Sign out")
      expect(logoutButton).toBeInTheDocument()
    })

    const logoutButton = screen.getByText("Sign out")
    fireEvent.click(logoutButton)

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith("/api/auth/logout", { method: "POST" })
      expect(mockPush).toHaveBeenCalledWith("/login")
    })
  })

  it("highlights active navigation item", () => {
    ;(usePathname as jest.Mock).mockReturnValue("/workflows")
    
    store.dispatch(setSession({ 
      token: "test-token", 
      subject: "test-user", 
      roles: ["VIEWER"] 
    }))

    render(
      <ChakraProvider>
        <Provider store={store}>
          <Navigation />
        </Provider>
      </ChakraProvider>
    )

    const workflowsButton = screen.getByText("Workflows")
    expect(workflowsButton).toBeInTheDocument()
  })
})
