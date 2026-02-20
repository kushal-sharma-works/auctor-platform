import { render, screen, waitFor } from "@testing-library/react"
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
  let googleCallback: ((response: { credential?: string }) => void) | null = null

  beforeEach(() => {
    pushMock.mockClear()
    googleCallback = null
    ;(useRouter as jest.Mock).mockReturnValue({
      push: pushMock,
      replace: jest.fn(),
      prefetch: jest.fn(),
      back: jest.fn(),
    })
    process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID = "test-google-client-id.apps.googleusercontent.com"
    ;(global.fetch as jest.Mock) = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        token: buildToken({ sub: "user", roles: ["VIEWER", "EXECUTOR"], exp: 9999999999 }),
      }),
    })

    Object.defineProperty(window, "google", {
      configurable: true,
      writable: true,
      value: {
        accounts: {
          id: {
            initialize: jest.fn((config: { callback: (response: { credential?: string }) => void }) => {
              googleCallback = config.callback
            }),
            renderButton: jest.fn(),
          },
        },
      },
    })
  })

  it("handles successful Google sign-in and redirects", async () => {
    render(
      <Provider store={store}>
        <LoginPage />
      </Provider>
    )

    await waitFor(() => {
      expect(googleCallback).not.toBeNull()
    })

    googleCallback?.({ credential: "google-id-token" })

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        "/api/auth/google",
        expect.objectContaining({ method: "POST" })
      )
    })

    await waitFor(() => {
      expect(pushMock).toHaveBeenCalledWith("/")
    })
  })

  it("shows info when Google client id is missing", async () => {
    process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID = ""

    render(
      <Provider store={store}>
        <LoginPage />
      </Provider>
    )

    expect(
      screen.getByText("Google sign-in is not configured. Set `NEXT_PUBLIC_GOOGLE_CLIENT_ID` to enable it.")
    ).toBeInTheDocument()

    await waitFor(() => {
      expect(global.fetch).not.toHaveBeenCalled()
    })
  })
})
