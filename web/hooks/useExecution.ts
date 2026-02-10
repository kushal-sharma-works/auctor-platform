import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useSelector } from 'react-redux'
import type { RootState } from '../store'
import {
  listExecutions,
  getExecution,
  getAuditTrail,
  startExecution,
  advanceExecution,
  type Execution,
  type AuditEvent,
  type StartExecutionRequest,
  type AdvanceExecutionRequest,
} from '../lib/executionApi'

/**
 * Hook to list executions with pagination
 */
export function useExecutions(page: number, size: number) {
  const token = useSelector((state: RootState) => state.session.token)

  return useQuery({
    queryKey: ['executions', page, size, token],
    queryFn: () => listExecutions(size, page * size, token || undefined),
    enabled: Boolean(token),
  })
}

/**
 * Hook to get a single execution by ID
 */
export function useExecution(executionId: string) {
  const token = useSelector((state: RootState) => state.session.token)

  return useQuery({
    queryKey: ['execution', executionId, token],
    queryFn: () => getExecution(executionId, token || undefined),
    enabled: Boolean(executionId) && Boolean(token),
    refetchInterval: 2000, // Refetch every 2 seconds to get latest status
  })
}

/**
 * Hook to get audit trail for an execution
 */
export function useExecutionAuditTrail(executionId: string) {
  const token = useSelector((state: RootState) => state.session.token)

  return useQuery({
    queryKey: ['executionAuditTrail', executionId, token],
    queryFn: () => getAuditTrail(executionId, token || undefined),
    enabled: Boolean(executionId) && Boolean(token),
  })
}

/**
 * Hook to start a new execution
 */
export function useStartExecution() {
  const token = useSelector((state: RootState) => state.session.token)

  return useMutation({
    mutationFn: (request: StartExecutionRequest) => startExecution(request, token || undefined),
  })
}

/**
 * Hook to advance an execution to the next state
 */
export function useAdvanceExecution() {
  const queryClient = useQueryClient()
  const token = useSelector((state: RootState) => state.session.token)

  return useMutation({
    mutationFn: ({ executionId, request }: { executionId: string; request?: AdvanceExecutionRequest }) =>
      advanceExecution(executionId, request, token || undefined),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['execution', variables.executionId, token] })
      queryClient.invalidateQueries({ queryKey: ['executionAuditTrail', variables.executionId, token] })
      queryClient.invalidateQueries({ queryKey: ['executions'] })
    },
  })
}
