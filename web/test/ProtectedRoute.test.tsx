import { render, screen } from "@testing-library/react"
import { ProtectedRoute } from "../components/ProtectedRoute"
import { Provider } from "react-redux"
import { store } from "../store"
import { setSession, clearToken } from "../store/sessionSlice"

describe("ProtectedRoute", () => {
  afterEach(() => {
    store.dispatch(clearToken())
  })

  it("renders children when user is authenticated", () => {
    store.dispatch(setSession({ 
      token: "test-token", 
      subject: "test-user", 
      roles: ["VIEWER"] 
    }))

    render(
      <Provider store={store}>
        <ProtectedRoute>
          <div>Protected Content</div>
        </ProtectedRoute>
      </Provider>
    )

    expect(screen.getByText("Protected Content")).toBeInTheDocument()
  })

  it("renders unauthorized message when no token", () => {
    render(
      <Provider store={store}>
        <ProtectedRoute>
          <div>Protected Content</div>
        </ProtectedRoute>
      </Provider>
    )

    expect(screen.getByText("Unauthorized")).toBeInTheDocument()
    expect(screen.queryByText("Protected Content")).not.toBeInTheDocument()
  })
})
