"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useSelector } from "react-redux"
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
import type { RootState } from "../store"

// Workflow Hooks
export function useWorkflows(page: number = 0, size: number = 20) {
  const token = useSelector((state: RootState) => state.session.token)
  return useQuery({
    queryKey: ["workflows", page, size, token],
    queryFn: () =>
      requestDefinitionGraphQL<any>(LIST_WORKFLOWS, { page, size }, token || undefined),
    enabled: Boolean(token),
  })
}

export function useWorkflow(id: string) {
  const token = useSelector((state: RootState) => state.session.token)
  return useQuery({
    queryKey: ["workflow", id, token],
    queryFn: () => requestDefinitionGraphQL<any>(GET_WORKFLOW, { id }, token || undefined),
    enabled: Boolean(id) && Boolean(token),
  })
}

export function useCreateWorkflow() {
  const queryClient = useQueryClient()
  const token = useSelector((state: RootState) => state.session.token)
  
  return useMutation({
    mutationFn: (input: any) =>
      requestDefinitionGraphQL<any>(CREATE_WORKFLOW, { input }, token || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["workflows"] })
    },
  })
}

export function usePublishWorkflow() {
  const queryClient = useQueryClient()
  const token = useSelector((state: RootState) => state.session.token)
  
  return useMutation({
    mutationFn: (id: string) =>
      requestDefinitionGraphQL<any>(PUBLISH_WORKFLOW, { id }, token || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["workflows"] })
    },
  })
}

// Policy Hooks
export function usePolicies(page: number = 0, size: number = 20) {
  const token = useSelector((state: RootState) => state.session.token)
  return useQuery({
    queryKey: ["policies", page, size, token],
    queryFn: () =>
      requestDefinitionGraphQL<any>(LIST_POLICIES, { page, size }, token || undefined),
    enabled: Boolean(token),
  })
}

export function usePolicy(id: string) {
  const token = useSelector((state: RootState) => state.session.token)
  return useQuery({
    queryKey: ["policy", id, token],
    queryFn: () => requestDefinitionGraphQL<any>(GET_POLICY, { id }, token || undefined),
    enabled: Boolean(id) && Boolean(token),
  })
}

export function useCreatePolicy() {
  const queryClient = useQueryClient()
  const token = useSelector((state: RootState) => state.session.token)
  
  return useMutation({
    mutationFn: (input: any) =>
      requestDefinitionGraphQL<any>(CREATE_POLICY, { input }, token || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["policies"] })
    },
  })
}

export function usePublishPolicy() {
  const queryClient = useQueryClient()
  const token = useSelector((state: RootState) => state.session.token)
  
  return useMutation({
    mutationFn: (id: string) =>
      requestDefinitionGraphQL<any>(PUBLISH_POLICY, { id }, token || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["policies"] })
    },
  })
}

// Execution Hooks
export function useExecutions(limit: number = 20, offset: number = 0) {
  const token = useSelector((state: RootState) => state.session.token)
  return useQuery({
    queryKey: ["executions", limit, offset, token],
    queryFn: () =>
      requestExecutionGraphQL<any>(LIST_EXECUTIONS, { limit, offset }, token || undefined),
    enabled: Boolean(token),
  })
}

export function useExecution(id: string) {
  const token = useSelector((state: RootState) => state.session.token)
  return useQuery({
    queryKey: ["execution", id, token],
    queryFn: () => requestExecutionGraphQL<any>(GET_EXECUTION, { id }, token || undefined),
    enabled: Boolean(id) && Boolean(token),
  })
}

export function useStartExecution() {
  const queryClient = useQueryClient()
  const token = useSelector((state: RootState) => state.session.token)
  
  return useMutation({
    mutationFn: (input: any) =>
      requestExecutionGraphQL<any>(START_EXECUTION, { input }, token || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["executions"] })
    },
  })
}

export function useAdvanceExecution() {
  const queryClient = useQueryClient()
  const token = useSelector((state: RootState) => state.session.token)
  
  return useMutation({
    mutationFn: ({ executionId, input }: { executionId: string; input?: any }) =>
      requestExecutionGraphQL<any>(ADVANCE_EXECUTION, { executionId, input }, token || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["executions"] })
    },
  })
}
