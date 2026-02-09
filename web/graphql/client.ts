import { GraphQLClient } from "graphql-request"

const definitionClient = new GraphQLClient("/api/definition-graphql")
const executionClient = new GraphQLClient("/api/execution-graphql")

export async function requestDefinitionGraphQL<T>(
  query: string,
  variables?: Record<string, unknown>
): Promise<T> {
  return definitionClient.request<T>(query, variables)
}

export async function requestExecutionGraphQL<T>(
  query: string,
  variables?: Record<string, unknown>
): Promise<T> {
  return executionClient.request<T>(query, variables)
}

// Legacy function for backward compatibility
export async function graphqlRequest<T>(
  query: string,
  variables?: Record<string, any>
): Promise<T> {
  return executionClient.request<T>(query, variables)
}
