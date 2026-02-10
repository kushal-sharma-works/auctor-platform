import { render, screen, fireEvent, waitFor } from "@testing-library/react"
import LoginPage from "../app/login/page"
import { Provider } from "react-redux"
import { store } from "../store"

const pushMock = jest.fn()

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: pushMock }),
  useSearchParams: () => new URLSearchParams(),
}))

const buildToken = (payload: Record<string, unknown>) => {
  const header = { alg: "HS256", typ: "JWT" }
  const encode = (value: object) =>
    Buffer.from(JSON.stringify(value))
      .toString("base64")
      .replace(/=/g, "")
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
  return `${encode(header)}.${encode(payload)}.signature`
}

describe("LoginPage", () => {
  beforeEach(() => {
    pushMock.mockClear()
    process.env.NEXT_PUBLIC_ENABLE_DEV_LOGIN = "true"
    ;(global as any).fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        token: buildToken({ sub: "user", roles: ["VIEWER", "EXECUTOR"], exp: 9999999999 }),
      }),
    })
  })

  it("submits credentials and redirects", async () => {
    render(
      <Provider store={store}>
        <LoginPage />
      </Provider>
    )

    fireEvent.change(screen.getByPlaceholderText("Username or email"), {
      target: { value: "user@example.com" },
    })
    fireEvent.change(screen.getByPlaceholderText("Password (mock)"), {
      target: { value: "password" },
    })

    fireEvent.click(screen.getByRole("button", { name: "Sign in (Dev)" }))

    await waitFor(() => {
      expect((global as any).fetch).toHaveBeenCalledWith(
        "/api/auth/token",
        expect.objectContaining({ method: "POST" })
      )
    })

    await waitFor(() => {
      expect(pushMock).toHaveBeenCalledWith("/")
    })
  })
})
