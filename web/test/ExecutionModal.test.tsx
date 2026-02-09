import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Provider } from 'react-redux'
import { ReactNode } from 'react'
import { ChakraProvider } from '@chakra-ui/react'
import { StartExecutionModal } from '../components/StartExecutionModal'
import { store } from '../store'

// Mock the hooks at module level
vi.mock('../hooks/useGraphql', () => ({
  useWorkflows: vi.fn(() => ({
    data: { workflows: { content: [] } },
    isLoading: false,
    error: null,
  })),
}))

vi.mock('../hooks/useExecution', () => ({
  useStartExecution: vi.fn(() => ({
    mutateAsync: vi.fn().mockResolvedValue({ id: 'exec-1' }),
    isPending: false,
  })),
}))

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
}))

function renderWithProviders(component: ReactNode) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        <ChakraProvider>{component}</ChakraProvider>
      </QueryClientProvider>
    </Provider>
  )
}

describe('StartExecutionModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the modal when open', () => {
    const mockOnClose = vi.fn()
    renderWithProviders(<StartExecutionModal isOpen={true} onClose={mockOnClose} />)

    expect(screen.getByText('Start New Execution')).toBeInTheDocument()
  })

  it('does not render when closed', () => {
    const mockOnClose = vi.fn()
    const { container } = renderWithProviders(<StartExecutionModal isOpen={false} onClose={mockOnClose} />)

    // Modal component should not be in the DOM when closed
    expect(container.querySelector('[role="dialog"]')).not.toBeInTheDocument()
  })

  it('displays cancel button when modal is open', () => {
    const mockOnClose = vi.fn()
    renderWithProviders(<StartExecutionModal isOpen={true} onClose={mockOnClose} />)

    const cancelButton = screen.getByRole('button', { name: /cancel/i })
    expect(cancelButton).toBeInTheDocument()
  })

  it('displays start execution button', () => {
    const mockOnClose = vi.fn()
    renderWithProviders(<StartExecutionModal isOpen={true} onClose={mockOnClose} />)

    const startButton = screen.getByRole('button', { name: /start execution/i })
    expect(startButton).toBeInTheDocument()
  })
})
