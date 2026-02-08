import { ReactNode } from 'react'
import * as rtl from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Provider as ReduxProvider } from 'react-redux'
import { store } from '../store'
import { Provider as ChakraProvider } from '../components/ui/provider'

// Create a test-specific QueryClient with retries disabled for faster tests
export function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
      },
      mutations: {
        retry: false,
      },
    },
  })
}

interface CustomRenderOptions extends rtl.RenderOptions {
  queryClient?: QueryClient
}

function TestWrapper({ children, queryClient }: { children: ReactNode; queryClient: QueryClient }) {
  return (
    <ReduxProvider store={store}>
      <ChakraProvider>
        <QueryClientProvider client={queryClient}>
          {children}
        </QueryClientProvider>
      </ChakraProvider>
    </ReduxProvider>
  )
}

export function render(
  ui: ReactNode,
  { queryClient = createTestQueryClient(), ...renderOptions }: CustomRenderOptions & { queryClient?: QueryClient } = {}
) {
  return rtl.render(ui, {
    ...renderOptions,
    wrapper: ({ children }) => (
      <TestWrapper queryClient={queryClient}>{children}</TestWrapper>
    ),
  })
}

export * from '@testing-library/react'
