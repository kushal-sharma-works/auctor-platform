// Workflow queries
export const GET_DEFINITIONS = `
  query {
    getWorkflow(id: "123", version: 1) {
      id
      name
      version
      states
    }
  }
`

// Execution queries
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
          state
          reason
        }
        input {
          key
          value
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
        state
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

export const GET_AUDIT_TRAIL = `
  query GetAuditTrail($executionId: ID!) {
    getAuditTrail(executionId: $executionId) {
      id
      executionId
      eventType
      actor
      details
      timestamp
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
        state
        reason
      }
      input {
        key
        value
      }
      createdAt
      updatedAt
    }
  }
`

export const ADVANCE_EXECUTION = `
  mutation AdvanceExecution($executionId: ID!, $input: AdvanceExecutionInput) {
    advanceExecution(executionId: $executionId, input: $input) {
      id
      workflowId
      workflowVersion
      currentState
      status {
        state
        reason
      }
      input {
        key
        value
      }
      createdAt
      updatedAt
    }
  }
`
