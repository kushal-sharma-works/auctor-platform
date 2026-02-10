"use client"

import { useEffect, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { useDispatch } from "react-redux"
import {
  Box,
  Button,
  Heading,
  Input,
  Text,
  VStack,
  HStack,
  Divider,
  Alert,
  AlertIcon,
} from "@chakra-ui/react"
import { setSession } from "../../store/sessionSlice"
import { buildSessionFromToken, getStoredSession, storeSessionToken } from "../../lib/auth-client"

const loadGoogleScript = () => {
  if (document.getElementById("google-identity")) return
  const script = document.createElement("script")
  script.src = "https://accounts.google.com/gsi/client"
  script.async = true
  script.defer = true
  script.id = "google-identity"
  document.body.appendChild(script)
}

export default function LoginPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const dispatch = useDispatch()
  const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID
  const devLoginEnabled = process.env.NEXT_PUBLIC_ENABLE_DEV_LOGIN === "true"
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (googleClientId) {
      loadGoogleScript()
    }
  }, [googleClientId])

  useEffect(() => {
    const session = getStoredSession()
    if (session) {
      const redirectTo = searchParams.get("redirect") || "/"
      router.push(redirectTo)
    }
  }, [router, searchParams])

  useEffect(() => {
    const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID
    if (!clientId) return

    const initGoogle = () => {
      if (!window.google?.accounts?.id) return
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: async (response: { credential?: string }) => {
          if (!response.credential) {
            setError("Google sign-in failed")
            return
          }
          await handleGoogleLogin(response.credential)
        },
      })
      window.google.accounts.id.renderButton(
        document.getElementById("google-signin")!,
        { theme: "outline", size: "large", width: "320" }
      )
    }

    if (window.google?.accounts?.id) {
      initGoogle()
      return
    }

    const interval = setInterval(() => {
      if (window.google?.accounts?.id) {
        clearInterval(interval)
        initGoogle()
      }
    }, 200)

    return () => clearInterval(interval)
  }, [])

  const applyToken = (token: string) => {
    const session = buildSessionFromToken(token)
    if (!session) {
      setError("Invalid token response")
      return
    }
    storeSessionToken(token)
    dispatch(setSession(session))
    const redirectTo = searchParams.get("redirect") || "/"
    router.push(redirectTo)
  }

  const handleGoogleLogin = async (idToken: string) => {
    setError(null)
    try {
      const response = await fetch("/api/auth/google", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ idToken }),
      })

      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || "Google login failed")
      }

      const data = await response.json()
      applyToken(data.token)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Google login failed")
    }
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    try {
      const response = await fetch("/api/auth/token", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      })

      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || "Login failed")
      }

      const data = await response.json()
      applyToken(data.token)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed")
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Box minH="100vh" bg="gray.50" display="flex" alignItems="center" justifyContent="center" px={6}>
      <Box bg="white" borderRadius="2xl" boxShadow="xl" p={10} w="full" maxW="md">
        <VStack align="stretch" spacing={6}>
          <Box textAlign="center">
            <Heading size="lg" color="gray.900">
              Sign in to Auctor
            </Heading>
            <Text color="gray.600" mt={2}>
              Use Google to continue.
            </Text>
          </Box>

          {error && (
            <Alert status="error" borderRadius="md">
              <AlertIcon />
              {error}
            </Alert>
          )}

          {!googleClientId ? (
            <Alert status="info" borderRadius="md">
              <AlertIcon />
              Google sign-in is not configured. Set `NEXT_PUBLIC_GOOGLE_CLIENT_ID` to enable it.
            </Alert>
          ) : (
            <Box id="google-signin" display="flex" justifyContent="center" />
          )}

          {devLoginEnabled && (
            <>
              <Divider />
              <form onSubmit={handleSubmit}>
                <VStack spacing={4} align="stretch">
                  <Input
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="Username or email"
                    autoComplete="username"
                    isRequired
                  />
                  <Input
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Password (mock)"
                    type="password"
                    autoComplete="current-password"
                    isRequired
                  />
                  <Button
                    type="submit"
                    colorScheme="blue"
                    isLoading={isSubmitting}
                    isDisabled={!username || !password}
                  >
                    Sign in (Dev)
                  </Button>
                </VStack>
              </form>

              <HStack justify="center" fontSize="xs" color="gray.500">
                <Text>Dev mode only: mock login issues app JWTs.</Text>
              </HStack>
            </>
          )}
        </VStack>
      </Box>
    </Box>
  )
}

declare global {
  interface Window {
    google?: {
      accounts?: {
        id?: {
          initialize: (config: { client_id: string; callback: (resp: { credential?: string }) => void }) => void
          renderButton: (element: HTMLElement, options: Record<string, unknown>) => void
        }
      }
    }
  }
}
