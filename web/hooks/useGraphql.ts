import { useMutation, useQuery } from "@tanstack/react-query"
import { requestGraphQL } from "../graphql/client"
import {
  CREATE_POLICY,
  CREATE_WORKFLOW,
  GET_POLICY,
  GET_WORKFLOW,
  LIST_POLICIES,
  LIST_WORKFLOWS,
  PUBLISH_POLICY,
  PUBLISH_WORKFLOW,
} from "../graphql/documents"
import type {
  CreatePolicyInput,
  CreatePolicyMutation,
  CreateWorkflowInput,
  CreateWorkflowMutation,
  GetPolicyQuery,
  GetWorkflowQuery,
  ListPoliciesQuery,
  ListWorkflowsQuery,
  PublishPolicyMutation,
  PublishWorkflowMutation,
} from "../graphql/generated/types"
import { useSelector } from "react-redux"
import type { RootState } from "../store"

export function useWorkflows(page: number, size: number) {
  const token = useSelector((state: RootState) => state.session.token)

  return useQuery({
    queryKey: ["workflows", page, size],
    queryFn: () =>
      requestGraphQL<ListWorkflowsQuery>(
        LIST_WORKFLOWS,
        { page, size },
        token
      ),
  })
}

export function useWorkflow(id: string) {
  const token = useSelector((state: RootState) => state.session.token)

  return useQuery({
    queryKey: ["workflow", id],
    queryFn: () =>
      requestGraphQL<GetWorkflowQuery>(GET_WORKFLOW, { id }, token),
    enabled: Boolean(id),
  })
}

export function useCreateWorkflow() {
  const token = useSelector((state: RootState) => state.session.token)

  return useMutation({
    mutationFn: (input: CreateWorkflowInput) =>
      requestGraphQL<CreateWorkflowMutation>(
        CREATE_WORKFLOW,
        { input },
        token
      ),
  })
}

export function usePublishWorkflow() {
  const token = useSelector((state: RootState) => state.session.token)

  return useMutation({
    mutationFn: (id: string) =>
      requestGraphQL<PublishWorkflowMutation>(
        PUBLISH_WORKFLOW,
        { id },
        token
      ),
  })
}

export function usePolicies(page: number, size: number) {
  const token = useSelector((state: RootState) => state.session.token)

  return useQuery({
    queryKey: ["policies", page, size],
    queryFn: () =>
      requestGraphQL<ListPoliciesQuery>(
        LIST_POLICIES,
        { page, size },
        token
      ),
  })
}

export function usePolicy(id: string) {
  const token = useSelector((state: RootState) => state.session.token)

  return useQuery({
    queryKey: ["policy", id],
    queryFn: () =>
      requestGraphQL<GetPolicyQuery>(GET_POLICY, { id }, token),
    enabled: Boolean(id),
  })
}

export function useCreatePolicy() {
  const token = useSelector((state: RootState) => state.session.token)

  return useMutation({
    mutationFn: (input: CreatePolicyInput) =>
      requestGraphQL<CreatePolicyMutation>(
        CREATE_POLICY,
        { input },
        token
      ),
  })
}

export function usePublishPolicy() {
  const token = useSelector((state: RootState) => state.session.token)

  return useMutation({
    mutationFn: (id: string) =>
      requestGraphQL<PublishPolicyMutation>(
        PUBLISH_POLICY,
        { id },
        token
      ),
  })
}
