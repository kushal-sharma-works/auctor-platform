import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Provider } from 'react-redux'
import { ReactNode } from 'react'
import ExecutionDetailPage from '../app/executions/[id]/page'
import { store } from '../store'

// Mock the hooks
vi.mock('../hooks/useExecution', () => ({
  useExecution: vi.fn(() => ({
    data: {
      id: 'exec-123',
      workflowId: 'workflow-1',
      workflowVersion: 1,
      currentState: 'INITIAL',
      status: { state: 'RUNNING' },
      input: {},
      createdAt: '2026-02-08T10:00:00Z',
      updatedAt: '2026-02-08T10:00:00Z',
    },
    isLoading: false,
    error: null,
  })),
  useExecutionAuditTrail: vi.fn(() => ({
    data: [],
    isLoading: false,
  })),
  useAdvanceExecution: vi.fn(() => ({
    mutateAsync: vi.fn(),
    isPending: false,
  })),
}))

vi.mock('../components/Layout', () => ({
  Layout: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}))

vi.mock('../components/UI', () => ({
  Badge: ({ status }: { status: string }) => <span>{status}</span>,
}))

vi.mock('next/navigation', () => ({
  useParams: () => ({ id: 'exec-123' }),
  useRouter: () => ({
    back: vi.fn(),
    push: vi.fn(),
  }),
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

describe('ExecutionDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders execution details heading', () => {
    renderWithProviders(<ExecutionDetailPage />)
    expect(screen.getByText('Execution Details')).toBeInTheDocument()
  })

  it('displays  workflow ID', () => {
    renderWithProviders(<ExecutionDetailPage />)
    expect(screen.getByText('workflow-1')).toBeInTheDocument()
  })

  it('displays advance button when running', () => {
    renderWithProviders(<ExecutionDetailPage />)
    const button = screen.getByRole('button', { name: /advance to next state/i })
    expect(button).toBeInTheDocument()
  })

  it('displays audit trail section', () => {
    renderWithProviders(<ExecutionDetailPage />)
    expect(screen.getByText('Audit Trail')).toBeInTheDocument()
  })
})
