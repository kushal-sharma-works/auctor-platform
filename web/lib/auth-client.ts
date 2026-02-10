import { decodeJwtPayload, isJwtExpiringSoon } from "./jwt-browser"
import { getStoredToken, setStoredToken, clearStoredToken, getCookieToken } from "../store/token"

export type AuthSession = {
  token: string
  subject: string
  roles: string[]
}

export const buildSessionFromToken = (token: string): AuthSession | null => {
  const payload = decodeJwtPayload(token)
  if (!payload) return null
  return {
    token,
    subject: payload.sub ?? "unknown",
    roles: Array.isArray(payload.roles) ? payload.roles : [],
  }
}

export const getStoredSession = (): AuthSession | null => {
  const token = getStoredToken() || getCookieToken()
  if (!token) return null
  return buildSessionFromToken(token)
}

export const storeSessionToken = (token: string) => {
  setStoredToken(token)
}

export const clearSessionToken = () => {
  clearStoredToken()
}

export const needsRefresh = (token: string): boolean => isJwtExpiringSoon(token, 120)
