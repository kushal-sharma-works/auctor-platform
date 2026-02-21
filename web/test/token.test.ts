/**
 * @jest-environment node
 */
import { getStoredToken, setStoredToken, clearStoredToken, getCookieToken } from "../store/token"

describe("token utilities", () => {
  describe("Browser environment", () => {
    beforeEach(() => {
      // Mock browser environment
      Object.defineProperty(global, "window", {
        value: {
          localStorage: {
            getItem: jest.fn(),
            setItem: jest.fn(),
            removeItem: jest.fn(),
          },
        },
        writable: true,
      })

      Object.defineProperty(global, "document", {
        value: {
          cookie: "",
        },
        writable: true,
      })
    })

    afterEach(() => {
      delete (global as any).window
      delete (global as any).document
    })

    describe("getStoredToken", () => {
      it("returns token from localStorage", () => {
        ;(window.localStorage.getItem as jest.Mock).mockReturnValue("test-token")

        const token = getStoredToken()

        expect(token).toBe("test-token")
        expect(window.localStorage.getItem).toHaveBeenCalledWith("auctor.auth.token")
      })

      it("returns null when no token stored", () => {
        ;(window.localStorage.getItem as jest.Mock).mockReturnValue(null)

        const token = getStoredToken()

        expect(token).toBeNull()
      })
    })

    describe("setStoredToken", () => {
      it("stores token in localStorage and cookie", () => {
        setStoredToken("new-token")

        expect(window.localStorage.setItem).toHaveBeenCalledWith(
          "auctor.auth.token",
          "new-token"
        )
      })
    })

    describe("clearStoredToken", () => {
      it("removes token from localStorage", () => {
        clearStoredToken()

        expect(window.localStorage.removeItem).toHaveBeenCalledWith("auctor.auth.token")
      })
    })
  })

  describe("getCookieToken", () => {
    it("extracts token from cookie string", () => {
      const cookieString = "auctor.auth.token=test-token; other=value"

      const token = getCookieToken(cookieString)

      expect(token).toBe("test-token")
    })

    it("handles URL encoded token", () => {
      const encodedToken = encodeURIComponent("token-with-special-chars")
      const cookieString = `auctor.auth.token=${encodedToken}`

      const token = getCookieToken(cookieString)

      expect(token).toBe("token-with-special-chars")
    })

    it("returns null when token not found", () => {
      const cookieString = "other=value; another=thing"

      const token = getCookieToken(cookieString)

      expect(token).toBeNull()
    })

    it("returns null for empty cookie string", () => {
      const token = getCookieToken("")

      expect(token).toBeNull()
    })

    it("handles cookie with spaces", () => {
      const cookieString = "  auctor.auth.token=test-token  ; other=value"

      const token = getCookieToken(cookieString)

      expect(token).toBe("test-token")
    })
  })
})
