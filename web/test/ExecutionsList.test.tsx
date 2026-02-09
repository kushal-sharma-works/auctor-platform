import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Provider } from 'react-redux'
import { ReactNode } from 'react'
import ExecutionsPage from '../app/executions/page'
import { store } from '../store'

// Mock the hooks
vi.mock('../hooks/useExecution', () => ({
  useExecutions: vi.fn(() => ({
    data: { items: [], limit: 20, offset: 0, total: 0 },
    isLoading: false,
    error: null,
  })),
}))

vi.mock('../components/StartExecutionModal', () => ({
  StartExecutionModal: ({ isOpen }: { isOpen: boolean }) =>
    isOpen ? <div data-testid="start-execution-modal">Modal</div> : null,
}))

vi.mock('../components/Layout', () => ({
  Layout: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}))

vi.mock('../components/UI', () => ({
  Badge: ({ status }: { status: string }) => <span>{status}</span>,
}))

function renderWithProviders(component: ReactNode) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>{component}</QueryClientProvider>
    </Provider>
  )
}

describe('ExecutionsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders  the page title', () => {
    renderWithProviders(<ExecutionsPage />)
    expect(screen.getByText('Executions')).toBeInTheDocument()
  })

  it('displays start execution button', () => {
    renderWithProviders(<ExecutionsPage />)
    const button = screen.getByRole('button', { name: /start execution/i })
    expect(button).toBeInTheDocument()
  })

  it('displays empty state message', () => {
    renderWithProviders(<ExecutionsPage />)
    expect(screen.getByText('No executions found. Start a new execution to get started.')).toBeInTheDocument()
  })
})
