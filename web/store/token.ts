const TOKEN_KEY = "auctor.auth.token"

const isBrowser = () => typeof window !== "undefined"

export const getStoredToken = (): string | null => {
  if (!isBrowser()) return null
  return window.localStorage.getItem(TOKEN_KEY)
}

export const setStoredToken = (token: string) => {
  if (!isBrowser()) return
  window.localStorage.setItem(TOKEN_KEY, token)
}

export const clearStoredToken = () => {
  if (!isBrowser()) return
  window.localStorage.removeItem(TOKEN_KEY)
}
