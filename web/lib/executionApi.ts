/**
 * Execution Service API Client
 * REST-based communication with execution service via Next.js proxy
 */

const EXECUTION_API_BASE = '/api/execution'

export interface Execution {
  id: string
  workflowId: string
  workflowVersion: number
  currentState: string
  status: ExecutionStatus
  input: Record<string, string>
  createdAt: string
  updatedAt: string
}

export interface ExecutionStatus {
  state: string
  reason?: string
}

export interface AuditEvent {
  id: string
  executionId: string
  eventType: string
  actor: string
  details: Record<string, unknown>
  timestamp: string
}

export interface ExecutionListResponse {
  limit: number
  offset: number
  total: number
  items: Execution[]
}

export interface StartExecutionRequest {
  workflowId: string
  workflowVersion: number
  input: Record<string, string>
}

export interface AdvanceExecutionRequest {
  correlationId?: string
}

/**
 * List all executions with pagination
 */
export async function listExecutions(
  limit: number = 20,
  offset: number = 0,
  token?: string
): Promise<ExecutionListResponse> {
  const url = new URL(EXECUTION_API_BASE, typeof window === 'undefined' ? 'http://localhost:3000' : window.location.origin)
  url.pathname = `${EXECUTION_API_BASE}/executions`
  url.searchParams.append('limit', String(limit))
  url.searchParams.append('offset', String(offset))

  const response = await fetchWithAuth(url.toString(), {
    method: 'GET',
  }, token)

  if (!response.ok) {
    const error = await response.json()
    throw new Error(error.message || 'Failed to list executions')
  }

  const items = await response.json()
  return {
    limit,
    offset,
    total: items.length,
    items,
  }
}

/**
 * Get a single execution by ID
 */
export async function getExecution(
  executionId: string,
  token?: string
): Promise<Execution> {
  const response = await fetchWithAuth(
    `${EXECUTION_API_BASE}/executions/${executionId}`,
    { method: 'GET' },
    token
  )

  if (!response.ok) {
    const error = await response.json()
    throw new Error(error.message || 'Failed to fetch execution')
  }

  return response.json()
}

/**
 * Get audit trail for an execution
 */
export async function getAuditTrail(
  executionId: string,
  token?: string
): Promise<AuditEvent[]> {
  const response = await fetchWithAuth(
    `${EXECUTION_API_BASE}/executions/${executionId}/audit`,
    { method: 'GET' },
    token
  )

  if (!response.ok) {
    const error = await response.json()
    throw new Error(error.message || 'Failed to fetch audit trail')
  }

  return response.json()
}

/**
 * Start a new execution
 */
export async function startExecution(
  request: StartExecutionRequest,
  token?: string
): Promise<Execution> {
  const response = await fetchWithAuth(
    `${EXECUTION_API_BASE}/executions`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    },
    token
  )

  if (!response.ok) {
    const error = await response.json()
    throw new Error(error.message || 'Failed to start execution')
  }

  return response.json()
}

/**
 * Advance execution to next state
 */
export async function advanceExecution(
  executionId: string,
  request?: AdvanceExecutionRequest,
  token?: string
): Promise<Execution> {
  const response = await fetchWithAuth(
    `${EXECUTION_API_BASE}/executions/${executionId}/advance`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request || {}),
    },
    token
  )

  if (!response.ok) {
    const error = await response.json()
    throw new Error(error.message || 'Failed to advance execution')
  }

  return response.json()
}

/**
 * Helper function to add auth header to requests
 */
async function fetchWithAuth(
  url: string,
  init: RequestInit,
  token?: string
): Promise<Response> {
  const headers = new Headers(init.headers)
  
  if (token) {
    // Token might already include 'Bearer ' prefix
    const authValue = token.startsWith('Bearer ') ? token : `Bearer ${token}`
    headers.set('Authorization', authValue)
  }

  return fetch(url, { ...init, headers })
}
