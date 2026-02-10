// ========== Definition Service Queries ==========

export const GET_WORKFLOW = `
  query GetWorkflow($id: ID!) {
    workflow(id: $id) {
      id
      name
      version
      status
      states
      initialState
      transitions {
        fromState
        toState
        policyRef
      }
      createdAt
      updatedAt
    }
  }
`

export const LIST_WORKFLOWS = `
  query ListWorkflows($page: Int, $size: Int) {
    workflows(page: $page, size: $size) {
      content {
        id
        name
        version
        status
        states
        initialState
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      page
      size
    }
  }
`

export const CREATE_WORKFLOW = `
  mutation CreateWorkflow($input: CreateWorkflowInput!) {
    createWorkflow(input: $input) {
      id
      name
      version
      status
      states
      initialState
      transitions {
        fromState
        toState
        policyRef
      }
      createdAt
      updatedAt
    }
  }
`

export const PUBLISH_WORKFLOW = `
  mutation PublishWorkflow($id: ID!) {
    publishWorkflow(id: $id) {
      id
      name
      version
      status
      updatedAt
    }
  }
`

export const GET_POLICY = `
  query GetPolicy($id: ID!) {
    policy(id: $id) {
      id
      name
      version
      status
      conditions {
        field
        operator
        value
      }
      createdAt
    }
  }
`

export const LIST_POLICIES = `
  query ListPolicies($page: Int, $size: Int) {
    policies(page: $page, size: $size) {
      content {
        id
        name
        version
        status
        conditions {
          field
          operator
          value
        }
        createdAt
      }
      totalElements
      totalPages
      page
      size
    }
  }
`

export const CREATE_POLICY = `
  mutation CreatePolicy($input: CreatePolicyInput!) {
    createPolicy(input: $input) {
      id
      name
      version
      status
      conditions {
        field
        operator
        value
      }
      createdAt
    }
  }
`

export const PUBLISH_POLICY = `
  mutation PublishPolicy($id: ID!) {
    publishPolicy(id: $id) {
      id
      name
      version
      status
    }
  }
`

// ========== Execution Service Queries ==========

export const LIST_EXECUTIONS = `
  query ListExecutions($limit: Int, $offset: Int) {
    listExecutions(limit: $limit, offset: $offset) {
      limit
      offset
      total
      items {
        id
        workflowId
        workflowVersion
        currentState
        status {
          type
          reason
        }
        createdAt
        updatedAt
      }
    }
  }
`

export const GET_EXECUTION = `
  query GetExecution($id: ID!) {
    getExecution(id: $id) {
      id
      workflowId
      workflowVersion
      currentState
      status {
        type
        reason
      }
      input {
        key
        value
      }
      auditEvents {
        id
        executionId
        eventType
        actor
        details
        timestamp
      }
      createdAt
      updatedAt
    }
  }
`

export const START_EXECUTION = `
  mutation StartExecution($input: StartExecutionInput!) {
    startExecution(input: $input) {
      id
      workflowId
      workflowVersion
      currentState
      status {
        type
        reason
      }
      createdAt
    }
  }
`

export const ADVANCE_EXECUTION = `
  mutation AdvanceExecution($executionId: ID!, $input: AdvanceExecutionInput) {
    advanceExecution(executionId: $executionId, input: $input) {
      id
      currentState
      status {
        type
        reason
      }
      updatedAt
    }
  }
`
