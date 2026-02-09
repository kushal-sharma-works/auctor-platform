import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { requestDefinitionGraphQL, requestExecutionGraphQL } from "../graphql/client"
import {
  CREATE_POLICY,
  CREATE_WORKFLOW,
  GET_EXECUTION,
  GET_POLICY,
  GET_WORKFLOW,
  LIST_EXECUTIONS,
  LIST_POLICIES,
  LIST_WORKFLOWS,
  PUBLISH_POLICY,
  PUBLISH_WORKFLOW,
  START_EXECUTION,
  ADVANCE_EXECUTION,
} from "../graphql/documents"

// Workflow Hooks
export function useWorkflows(page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ["workflows", page, size],
    queryFn: () =>
      requestDefinitionGraphQL<any>(LIST_WORKFLOWS, { page, size }),
  })
}

export function useWorkflow(id: string) {
  return useQuery({
    queryKey: ["workflow", id],
    queryFn: () => requestDefinitionGraphQL<any>(GET_WORKFLOW, { id }),
    enabled: Boolean(id),
  })
}

export function useCreateWorkflow() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (input: any) =>
      requestDefinitionGraphQL<any>(CREATE_WORKFLOW, { input }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["workflows"] })
    },
  })
}

export function usePublishWorkflow() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (id: string) =>
      requestDefinitionGraphQL<any>(PUBLISH_WORKFLOW, { id }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["workflows"] })
    },
  })
}

// Policy Hooks
export function usePolicies(page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ["policies", page, size],
    queryFn: () =>
      requestDefinitionGraphQL<any>(LIST_POLICIES, { page, size }),
  })
}

export function usePolicy(id: string) {
  return useQuery({
    queryKey: ["policy", id],
    queryFn: () => requestDefinitionGraphQL<any>(GET_POLICY, { id }),
    enabled: Boolean(id),
  })
}

export function useCreatePolicy() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (input: any) =>
      requestDefinitionGraphQL<any>(CREATE_POLICY, { input }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["policies"] })
    },
  })
}

export function usePublishPolicy() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (id: string) =>
      requestDefinitionGraphQL<any>(PUBLISH_POLICY, { id }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["policies"] })
    },
  })
}

// Execution Hooks
export function useExecutions(limit: number = 20, offset: number = 0) {
  return useQuery({
    queryKey: ["executions", limit, offset],
    queryFn: () =>
      requestExecutionGraphQL<any>(LIST_EXECUTIONS, { limit, offset }),
  })
}

export function useExecution(id: string) {
  return useQuery({
    queryKey: ["execution", id],
    queryFn: () => requestExecutionGraphQL<any>(GET_EXECUTION, { id }),
    enabled: Boolean(id),
  })
}

export function useStartExecution() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (input: any) =>
      requestExecutionGraphQL<any>(START_EXECUTION, { input }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["executions"] })
    },
  })
}

export function useAdvanceExecution() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ executionId, input }: { executionId: string; input?: any }) =>
      requestExecutionGraphQL<any>(ADVANCE_EXECUTION, { executionId, input }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["executions"] })
    },
  })
}
