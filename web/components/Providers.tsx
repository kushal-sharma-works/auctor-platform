"use client"

import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { Provider as ReduxProvider, useDispatch } from "react-redux"
import { store } from "../store"
import { ReactNode, useEffect, useMemo } from "react"
import { getStoredToken } from "../store/token"
import { setToken } from "../store/sessionSlice"
import { Provider as ChakraProvider } from "./ui/provider"

function ProvidersInner({ children }: { children: ReactNode }) {
  const dispatch = useDispatch()

  useEffect(() => {
    const token = getStoredToken()
    if (token) {
      dispatch(setToken(token))
    }
  }, [dispatch])

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

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

export function Providers({ children }: { children: ReactNode }) {
  return (
    <ReduxProvider store={store}>
      <ChakraProvider>
        <ProvidersInner>{children}</ProvidersInner>
      </ChakraProvider>
    </ReduxProvider>
  )
}
