"use client"

import { useEffect } from "react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { Provider } from "react-redux"
import { store } from "../store"
import { initTracing } from "../src/observability/tracing"

const qc = new QueryClient()

export default function RootLayout({ children }: { children: React.ReactNode }) {
  useEffect(() => {
    initTracing()
  }, [])

  return (
    <html>
      <body>
        <Provider store={store}>
          <QueryClientProvider client={qc}>
            {children}
          </QueryClientProvider>
        </Provider>
      </body>
    </html>
  )
}
