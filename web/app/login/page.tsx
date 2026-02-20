"use client"

import { useEffect, useRef, useState } from "react"
import { useRouter } from "next/navigation"
import { useDispatch } from "react-redux"
import {
  Box,
  Heading,
  Text,
  VStack,
  Alert,
  AlertIcon,
  Spinner,
  Center,
} from "@chakra-ui/react"
import { setSession } from "../../store/sessionSlice"
import { buildSessionFromToken, storeSessionToken } from "../../lib/auth-client"

const loadGoogleScript = () => {
  if (document.getElementById("google-identity")) return
  const script = document.createElement("script")
  script.src = "https://accounts.google.com/gsi/client"
  script.async = true
  script.defer = true
  script.id = "google-identity"
  document.body.appendChild(script)
}

function LoginPageContent() {
  const router = useRouter()
  const dispatch = useDispatch()
  const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID
  const [error, setError] = useState<string | null>(null)
  const [isAuthenticating, setIsAuthenticating] = useState(false)
  const activeLoginRef = useRef(false)

  const getRedirectTarget = () => {
    const redirectTo =
      typeof window !== "undefined"
        ? new URLSearchParams(window.location.search).get("redirect") || "/"
        : "/"
    if (!redirectTo.startsWith("/") || redirectTo.startsWith("/login")) {
      return "/"
    }
    return redirectTo
  }

  useEffect(() => {
    if (googleClientId) {
      loadGoogleScript()
    }
  }, [googleClientId])

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
      activeLoginRef.current = false
      setIsAuthenticating(false)
      return
    }
    storeSessionToken(token)
    dispatch(setSession(session))
    router.push(getRedirectTarget())
  }

  const handleGoogleLogin = async (idToken: string) => {
    if (activeLoginRef.current) return
    activeLoginRef.current = true
    setIsAuthenticating(true)
    setError(null)
    try {
      const response = await fetch("/api/auth/google", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "same-origin",
        body: JSON.stringify({ idToken }),
      })

      if (!response.ok) {
        let message = "Google login failed"
        try {
          const data = await response.json() as { error?: string }
          message = data.error || message
        } catch {
          const text = await response.text()
          if (text) {
            message = text
          }
        }
        throw new Error(message || "Google login failed")
      }

      const data = await response.json()
      applyToken(data.token)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Google login failed")
      activeLoginRef.current = false
      setIsAuthenticating(false)
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
            <>
              <Box id="google-signin" display="flex" justifyContent="center" />
              {isAuthenticating && (
                <Center>
                  <Spinner size="md" color="blue.500" />
                </Center>
              )}
            </>
          )}
        </VStack>
      </Box>
    </Box>
  )
}

export default function LoginPage() {
  return <LoginPageContent />
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
