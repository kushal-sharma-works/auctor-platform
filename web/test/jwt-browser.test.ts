import {
  decodeJwtPayload,
  isJwtExpiringSoon,
  type JwtPayload,
} from "../lib/jwt-browser"

describe("jwt-browser utilities", () => {
  describe("decodeJwtPayload", () => {
    it("decodes valid JWT token", () => {
      const payload: JwtPayload = {
        sub: "user123",
        roles: ["ADMIN", "VIEWER"],
        exp: Math.floor(Date.now() / 1000) + 3600,
        iss: "auctor-auth",
        aud: "auctor-service",
        email: "user@example.com",
      }

      const header = { alg: "HS256", typ: "JWT" }
      const encode = (value: object) =>
        Buffer.from(JSON.stringify(value))
          .toString("base64")
          .replace(/=/g, "")
          .replace(/\+/g, "-")
          .replace(/\//g, "_")

      const token = `${encode(header)}.${encode(payload)}.signature`

      const decoded = decodeJwtPayload(token)

      expect(decoded).toMatchObject({
        sub: "user123",
        roles: ["ADMIN", "VIEWER"],
        email: "user@example.com",
      })
    })

    it("returns null for invalid token format", () => {
      const result = decodeJwtPayload("invalid.token")
      expect(result).toBeNull()
    })

    it("returns null for malformed JWT", () => {
      const result = decodeJwtPayload("not-a-jwt-token")
      expect(result).toBeNull()
    })

    it("returns null for token with invalid JSON", () => {
      const result = decodeJwtPayload("header.invalid-json.signature")
      expect(result).toBeNull()
    })

    it("handles token without optional fields", () => {
      const payload = { sub: "user123" }
      const header = { alg: "HS256", typ: "JWT" }
      const encode = (value: object) =>
        Buffer.from(JSON.stringify(value))
          .toString("base64")
          .replace(/=/g, "")
          .replace(/\+/g, "-")
          .replace(/\//g, "_")

      const token = `${encode(header)}.${encode(payload)}.signature`
      const decoded = decodeJwtPayload(token)

      expect(decoded).toMatchObject({ sub: "user123" })
      expect(decoded?.roles).toBeUndefined()
      expect(decoded?.exp).toBeUndefined()
    })
  })

  describe("isJwtExpiringSoon", () => {
    const createToken = (expiresInSeconds: number) => {
      const payload = {
        sub: "user123",
        exp: Math.floor(Date.now() / 1000) + expiresInSeconds,
      }
      const header = { alg: "HS256", typ: "JWT" }
      const encode = (value: object) =>
        Buffer.from(JSON.stringify(value))
          .toString("base64")
          .replace(/=/g, "")
          .replace(/\+/g, "-")
          .replace(/\//g, "_")

      return `${encode(header)}.${encode(payload)}.signature`
    }

    it("returns true when token expires within skew window", () => {
      const token = createToken(30) // expires in 30 seconds
      const result = isJwtExpiringSoon(token, 60) // 60 second skew
      expect(result).toBe(true)
    })

    it("returns false when token expires after skew window", () => {
      const token = createToken(120) // expires in 120 seconds
      const result = isJwtExpiringSoon(token, 60) // 60 second skew
      expect(result).toBe(false)
    })

    it("returns true when token is already expired", () => {
      const token = createToken(-10) // expired 10 seconds ago
      const result = isJwtExpiringSoon(token, 60)
      expect(result).toBe(true)
    })

    it("returns false when token has no exp claim", () => {
      const payload = { sub: "user123" }
      const header = { alg: "HS256", typ: "JWT" }
      const encode = (value: object) =>
        Buffer.from(JSON.stringify(value))
          .toString("base64")
          .replace(/=/g, "")
          .replace(/\+/g, "-")
          .replace(/\//g, "_")

      const token = `${encode(header)}.${encode(payload)}.signature`
      const result = isJwtExpiringSoon(token, 60)
      expect(result).toBe(false)
    })

    it("uses default skew of 60 seconds", () => {
      const token = createToken(30)
      const result = isJwtExpiringSoon(token) // no skew specified
      expect(result).toBe(true)
    })
  })
})
