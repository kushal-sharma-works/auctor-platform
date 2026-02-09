import { GraphQLClient } from "graphql-request"

const graphqlClient = new GraphQLClient("/api/graphql")

export async function requestGraphQL<T>(
  query: string,
  variables?: Record<string, unknown>,
  token?: string | null
): Promise<T> {
  if (token) {
    const authToken = token.startsWith("Bearer ") ? token : `Bearer ${token}`
    graphqlClient.setHeader("Authorization", authToken)
  } else {
    graphqlClient.setHeader("Authorization", "")
  }

  return graphqlClient.request<T>(query, variables)
}
