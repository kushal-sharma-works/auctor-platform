export interface JwtClaims {
  sub?: string
  roles?: string[]
  exp?: number
  iss?: string
  aud?: string | string[]
  email?: string
}

const decodeBase64Url = (value: string): string => {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/")
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=")
  if (typeof window === "undefined") {
    return Buffer.from(padded, "base64").toString("utf8")
  }
  return atob(padded)
}

export const decodeJwt = (token: string): JwtClaims | null => {
  try {
    const [, payload] = token.split(".")
    if (!payload) return null
    const json = decodeBase64Url(payload)
    return JSON.parse(json) as JwtClaims
  } catch {
    return null
  }
}

export const isJwtExpired = (token: string, skewSeconds: number = 30): boolean => {
  const claims = decodeJwt(token)
  if (!claims?.exp) return false
  const now = Math.floor(Date.now() / 1000)
  return claims.exp < now + skewSeconds
}
