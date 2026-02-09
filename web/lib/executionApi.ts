/**
 * Execution Service API Client
 * GraphQL-based communication with execution service via Next.js proxy
 */

import { graphqlRequest } from '../graphql/client'
import {
  LIST_EXECUTIONS,
  GET_EXECUTION,
  GET_AUDIT_TRAIL,
  START_EXECUTION,
  ADVANCE_EXECUTION,
} from '../graphql/queries'

export interface Execution {
  id: string
  workflowId: string
  workflowVersion: number
  currentState: string
  status: ExecutionStatus
  input: Array<{ key: string; value: string }>
  createdAt: string
  updatedAt: string
  auditEvents?: AuditEvent[]
}

export interface ExecutionStatus {
  type: string
  reason?: string
}

export interface AuditEvent {
  id: string
  executionId: string
  eventType: string
  actor: string
  details: string
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

interface ListExecutionsResponse {
  listExecutions: ExecutionListResponse
}

interface GetExecutionResponse {
  getExecution: Execution
}

interface GetAuditTrailResponse {
  getAuditTrail: AuditEvent[]
}

interface StartExecutionResponse {
  startExecution: Execution
}

interface AdvanceExecutionResponse {
  advanceExecution: Execution
}

/**
 * List all executions with pagination
 */
export async function listExecutions(
  limit: number = 20,
  offset: number = 0,
  token?: string
): Promise<ExecutionListResponse> {
  try {
    const response = await graphqlRequest<ListExecutionsResponse>(
      LIST_EXECUTIONS,
      token,
      { limit, offset }
    )
    return response.listExecutions
  } catch (error) {
    throw new Error(
      error instanceof Error ? error.message : 'Failed to list executions'
    )
  }
}

/**
 * Get a single execution by ID
 */
export async function getExecution(
  executionId: string,
  token?: string
): Promise<Execution> {
  try {
    const response = await graphqlRequest<GetExecutionResponse>(
      GET_EXECUTION,
      token,
      { id: executionId }
    )
    return response.getExecution
  } catch (error) {
    throw new Error(
      error instanceof Error ? error.message : 'Failed to fetch execution'
    )
  }
}

/**
 * Get audit trail for an execution
 */
export async function getAuditTrail(
  executionId: string,
  token?: string
): Promise<AuditEvent[]> {
  try {
    const response = await graphqlRequest<GetAuditTrailResponse>(
      GET_AUDIT_TRAIL,
      token,
      { executionId }
    )
    return response.getAuditTrail
  } catch (error) {
    throw new Error(
      error instanceof Error
        ? error.message
        : 'Failed to fetch audit trail'
    )
  }
}

/**
 * Start a new execution
 */
export async function startExecution(
  request: StartExecutionRequest,
  token?: string
): Promise<Execution> {
  try {
    const input = {
      workflowId: request.workflowId,
      workflowVersion: request.workflowVersion,
      input: Object.entries(request.input).map(([key, value]) => ({
        key,
        value,
      })),
    }

    const response = await graphqlRequest<StartExecutionResponse>(
      START_EXECUTION,
      token,
      { input }
    )
    return response.startExecution
  } catch (error) {
    throw new Error(
      error instanceof Error
        ? error.message
        : 'Failed to start execution'
    )
  }
}

/**
 * Advance execution to next state
 */
export async function advanceExecution(
  executionId: string,
  request?: AdvanceExecutionRequest,
  token?: string
): Promise<Execution> {
  try {
    const input = request
      ? {
          correlationId: request.correlationId,
        }
      : undefined

    const response = await graphqlRequest<AdvanceExecutionResponse>(
      ADVANCE_EXECUTION,
      token,
      { executionId, input }
    )
    return response.advanceExecution
  } catch (error) {
    throw new Error(
      error instanceof Error
        ? error.message
        : 'Failed to advance execution'
    )
  }
}
