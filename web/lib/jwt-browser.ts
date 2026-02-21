export type JwtPayload = {
  sub?: string
  roles?: string[]
  exp?: number
  iss?: string
  aud?: string | string[]
  email?: string
  [key: string]: unknown
}

const base64UrlDecode = (value: string): string => {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/")
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=")
  return atob(padded)
}

export const decodeJwtPayload = (token: string): JwtPayload | null => {
  const parts = token.split(".")
  if (parts.length !== 3) return null
  try {
    const json = base64UrlDecode(parts[1])
    return JSON.parse(json) as JwtPayload
  } catch {
    return null
  }
}

export const isJwtExpiringSoon = (token: string, skewSeconds: number = 60): boolean => {
  const payload = decodeJwtPayload(token)
  if (!payload?.exp) return false
  const now = Math.floor(Date.now() / 1000)
  return payload.exp < now + skewSeconds
}
