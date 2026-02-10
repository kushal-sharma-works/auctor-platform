"use client"

import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { Provider as ReduxProvider, useDispatch, useSelector } from "react-redux"
import { store } from "../store"
import { ReactNode, useEffect, useMemo } from "react"
import { ChakraProvider } from "@chakra-ui/react"
import { setSession } from "../store/sessionSlice"
import { buildSessionFromToken, getStoredSession, needsRefresh, storeSessionToken } from "../lib/auth-client"
import type { RootState } from "../store"

function SessionHydrator() {
  const dispatch = useDispatch()

  useEffect(() => {
    const session = getStoredSession()
    if (session) {
      dispatch(setSession(session))

      ;(async () => {
        try {
          const response = await fetch("/api/auth/refresh", {
            method: "POST",
            headers: {
              Authorization: session.token.startsWith("Bearer ")
                ? session.token
                : `Bearer ${session.token}`,
            },
          })
          if (!response.ok) return
          const data = (await response.json()) as { token?: string }
          if (!data.token) return
          const refreshed = buildSessionFromToken(data.token)
          if (!refreshed) return
          storeSessionToken(data.token)
          dispatch(setSession(refreshed))
        } catch {
          // ignore refresh errors on hydration
        }
      })()
    }
  }, [dispatch])

  return null
}

function SessionRefresher() {
  const dispatch = useDispatch()
  const token = useSelector((state: RootState) => state.session.token)

  useEffect(() => {
    if (!token) return
    const interval = setInterval(async () => {
      if (!token || !needsRefresh(token)) return
      try {
        const response = await fetch("/api/auth/refresh", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
        })
        if (!response.ok) return
        const data = (await response.json()) as { token?: string }
        if (!data.token) return
        const session = buildSessionFromToken(data.token)
        if (!session) return
        storeSessionToken(data.token)
        dispatch(setSession(session))
      } catch {
        // ignore refresh errors
      }
    }, 30_000)

    return () => clearInterval(interval)
  }, [dispatch, token])

  return null
}

export function Providers({ children }: { children: ReactNode }) {
  const queryClient = useMemo(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000,
            retry: 1,
          },
        },
      }),
    []
  )

  return (
    <ReduxProvider store={store}>
      <ChakraProvider>
        <QueryClientProvider client={queryClient}>
          <SessionHydrator />
          <SessionRefresher />
          {children}
        </QueryClientProvider>
      </ChakraProvider>
    </ReduxProvider>
  )
}
