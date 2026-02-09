import { GraphQLClient } from "graphql-request"

const definitionClient = new GraphQLClient("/api/definition-graphql")
const executionClient = new GraphQLClient("/api/execution-graphql")

const normalizeAuthHeader = (token?: string) => {
  if (!token) return undefined
  return token.startsWith("Bearer ") ? token : `Bearer ${token}`
}

export async function requestDefinitionGraphQL<T>(
  query: string,
  variables?: Record<string, unknown>
): Promise<T> {
  return definitionClient.request<T>(query, variables)
}

export async function requestExecutionGraphQL<T>(
  query: string,
  variables?: Record<string, unknown>,
  token?: string
): Promise<T> {
  const authHeader = normalizeAuthHeader(token)
  const headers = authHeader ? { Authorization: authHeader } : undefined
  return executionClient.request<T>(query, variables, headers)
}

// Legacy function for backward compatibility
export async function graphqlRequest<T>(
  query: string,
  token?: string,
  variables?: Record<string, any>
): Promise<T> {
  const authHeader = normalizeAuthHeader(token)
  const headers = authHeader ? { Authorization: authHeader } : undefined
  return executionClient.request<T>(query, variables, headers)
}
