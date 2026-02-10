const TOKEN_KEY = "auctor.auth.token"

const isBrowser = () => typeof window !== "undefined"

export const getStoredToken = (): string | null => {
  if (!isBrowser()) return null
  return window.localStorage.getItem(TOKEN_KEY)
}

export const setStoredToken = (token: string) => {
  if (!isBrowser()) return
  window.localStorage.setItem(TOKEN_KEY, token)
  const encoded = encodeURIComponent(token)
  document.cookie = `${TOKEN_KEY}=${encoded}; Path=/; SameSite=Lax; Max-Age=3600`
}

export const clearStoredToken = () => {
  if (!isBrowser()) return
  window.localStorage.removeItem(TOKEN_KEY)
  document.cookie = `${TOKEN_KEY}=; Path=/; Max-Age=0; SameSite=Lax`
}

export const getCookieToken = (cookieHeader?: string): string | null => {
  const source = cookieHeader ?? (isBrowser() ? document.cookie : "")
  if (!source) return null
  const prefix = `${TOKEN_KEY}=`
  const parts = source.split(";").map((part) => part.trim())
  const match = parts.find((part) => part.startsWith(prefix))
  if (!match) return null
  return decodeURIComponent(match.substring(prefix.length))
}

export const getStoredTokenPayload = () => {
  if (!isBrowser()) return null
  const token = window.localStorage.getItem(TOKEN_KEY)
  if (!token) return null
  return token
}
