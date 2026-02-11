import { render, screen, fireEvent, waitFor } from "@testing-library/react"
import LoginPage from "../app/login/page"
import { Provider } from "react-redux"
import { store } from "../store"
import { useRouter } from "next/navigation"

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
  const pushMock = jest.fn()

  beforeEach(() => {
    pushMock.mockClear()
    ;(useRouter as jest.Mock).mockReturnValue({
      push: pushMock,
      replace: jest.fn(),
      prefetch: jest.fn(),
      back: jest.fn(),
    })
    process.env.NEXT_PUBLIC_ENABLE_DEV_LOGIN = "true"
    ;(global.fetch as jest.Mock) = jest.fn().mockResolvedValue({
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
      expect(global.fetch).toHaveBeenCalledWith(
        "/api/auth/token",
        expect.objectContaining({ method: "POST" })
      )
    })

    await waitFor(() => {
      expect(pushMock).toHaveBeenCalledWith("/")
    })
  })
})
