import {
  buildSessionFromToken,
  needsRefresh,
} from "../lib/auth-client"

// Mock the token store module
jest.mock("../store/token", () => ({
  getStoredToken: jest.fn(),
  getCookieToken: jest.fn(),
  setStoredToken: jest.fn(),
  clearStoredToken: jest.fn(),
}))

describe("auth-client utilities", () => {
  const createToken = (payload: Record<string, any>) => {
    const header = { alg: "HS256", typ: "JWT" }
    const encode = (value: object) =>
      Buffer.from(JSON.stringify(value))
        .toString("base64")
        .replace(/=/g, "")
        .replace(/\+/g, "-")
        .replace(/\//g, "_")

    return `${encode(header)}.${encode(payload)}.signature`
  }

  beforeEach(() => {
    jest.clearAllMocks()
  })

  describe("buildSessionFromToken", () => {
    it("builds session from valid token", () => {
      const token = createToken({
        sub: "user@example.com",
        roles: ["ADMIN", "VIEWER"],
        exp: Math.floor(Date.now() / 1000) + 3600,
      })

      const session = buildSessionFromToken(token)

      expect(session).toEqual({
        token,
        subject: "user@example.com",
        roles: ["ADMIN", "VIEWER"],
      })
    })

    it("returns null for invalid token", () => {
      const session = buildSessionFromToken("invalid-token")
      expect(session).toBeNull()
    })

    it("uses 'unknown' for missing subject", () => {
      const token = createToken({
        roles: ["VIEWER"],
      })

      const session = buildSessionFromToken(token)

      expect(session?.subject).toBe("unknown")
    })

    it("uses empty array for missing roles", () => {
      const token = createToken({
        sub: "user@example.com",
      })

      const session = buildSessionFromToken(token)

      expect(session?.roles).toEqual([])
    })

    it("handles non-array roles", () => {
      const token = createToken({
        sub: "user@example.com",
        roles: "ADMIN", // not an array
      })

      const session = buildSessionFromToken(token)

      expect(session?.roles).toEqual([])
    })
  })

  describe("needsRefresh", () => {
    it("returns true when token expires soon", () => {
      const token = createToken({
        sub: "user",
        exp: Math.floor(Date.now() / 1000) + 60, // expires in 60 seconds
      })

      const result = needsRefresh(token)

      expect(result).toBe(true) // within 120 second window
    })

    it("returns false when token has time remaining", () => {
      const token = createToken({
        sub: "user",
        exp: Math.floor(Date.now() / 1000) + 300, // expires in 5 minutes
      })

      const result = needsRefresh(token)

      expect(result).toBe(false)
    })

    it("returns true when token is expired", () => {
      const token = createToken({
        sub: "user",
        exp: Math.floor(Date.now() / 1000) - 10, // expired 10 seconds ago
      })

      const result = needsRefresh(token)

      expect(result).toBe(true)
    })
  })
})
