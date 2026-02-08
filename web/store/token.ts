export function getStoredToken(): string | null {
  if (typeof window === "undefined") {
    return null
  }

  const fromStorage = window.localStorage.getItem("token")
  if (fromStorage) {
    return fromStorage
  }

  const cookie = document.cookie
    .split(";")
    .map((value) => value.trim())
    .find((value) => value.startsWith("token="))

  if (!cookie) {
    return null
  }

  return decodeURIComponent(cookie.split("=")[1])
}

export function setStoredToken(token: string) {
  if (typeof window === "undefined") {
    return
  }

  window.localStorage.setItem("token", token)
  document.cookie = `token=${encodeURIComponent(token)}; path=/`
}

export function clearStoredToken() {
  if (typeof window === "undefined") {
    return
  }

  window.localStorage.removeItem("token")
  document.cookie = "token=; Max-Age=0; path=/"
}
