import crypto from "crypto"

export type JwtClaims = {
  sub: string
  roles: string[]
  iss: string
  aud: string[]
  exp: number
  iat: number
  email?: string
}

const base64UrlEncode = (value: Buffer | string) => {
  const buffer = typeof value === "string" ? Buffer.from(value, "utf8") : value
  return buffer
    .toString("base64")
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
}

const base64UrlDecode = (value: string): Buffer => {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/")
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=")
  return Buffer.from(padded, "base64")
}

export const signJwt = (claims: JwtClaims, secret: string): string => {
  const header = { typ: "JWT", alg: "HS256" }
  const headerBase64 = base64UrlEncode(JSON.stringify(header))
  const payloadBase64 = base64UrlEncode(JSON.stringify(claims))
  const data = `${headerBase64}.${payloadBase64}`
  const signature = crypto.createHmac("sha256", secret).update(data).digest()
  const signatureBase64 = base64UrlEncode(signature)
  return `${data}.${signatureBase64}`
}

export type VerifyResult = {
  valid: boolean
  claims?: JwtClaims
  error?: string
}

export const verifyJwt = (token: string, secret: string): VerifyResult => {
  const parts = token.split(".")
  if (parts.length !== 3) {
    return { valid: false, error: "Invalid token format" }
  }
  const [headerBase64, payloadBase64, signatureBase64] = parts
  const data = `${headerBase64}.${payloadBase64}`
  const expected = crypto.createHmac("sha256", secret).update(data).digest()
  const actual = base64UrlDecode(signatureBase64)

  if (expected.length !== actual.length || !crypto.timingSafeEqual(expected, actual)) {
    return { valid: false, error: "Invalid signature" }
  }

  try {
    const payloadJson = base64UrlDecode(payloadBase64).toString("utf8")
    const claims = JSON.parse(payloadJson) as JwtClaims
    const now = Math.floor(Date.now() / 1000)
    if (claims.exp && claims.exp < now) {
      return { valid: false, error: "Token expired" }
    }
    return { valid: true, claims }
  } catch {
    return { valid: false, error: "Invalid payload" }
  }
}
