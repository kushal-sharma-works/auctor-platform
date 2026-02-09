export type Transition = {
  fromState: string
  toState: string
  policyRef?: string | null
}

export type Workflow = {
  id: string
  name: string
  version: number
  status: string
  states: string[]
  initialState: string
  transitions: Transition[]
  createdAt: string
  updatedAt: string
}

export type WorkflowPage = {
  content: Workflow[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export type PolicyCondition = {
  field: string
  operator: string
  value: string
}

export type Policy = {
  id: string
  name: string
  version: number
  status: string
  conditions: PolicyCondition[]
  createdAt: string
}

export type PolicyPage = {
  content: Policy[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export type CreateWorkflowInput = {
  name: string
  states: string[]
  initialState: string
  transitions: TransitionInput[]
}

export type TransitionInput = {
  fromState: string
  toState: string
  policyRef?: string | null
}

export type CreatePolicyInput = {
  name: string
  conditions: PolicyConditionInput[]
}

export type PolicyConditionInput = {
  field: string
  operator: string
  value: string
}

export type GetWorkflowQuery = {
  workflow: Workflow | null
}

export type ListWorkflowsQuery = {
  workflows: WorkflowPage
}

export type CreateWorkflowMutation = {
  createWorkflow: Workflow
}

export type PublishWorkflowMutation = {
  publishWorkflow: Workflow
}

export type GetPolicyQuery = {
  policy: Policy | null
}

export type ListPoliciesQuery = {
  policies: PolicyPage
}

export type CreatePolicyMutation = {
  createPolicy: Policy
}

export type PublishPolicyMutation = {
  publishPolicy: Policy
}
