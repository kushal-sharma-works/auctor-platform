import { GraphQLClient } from 'graphql-request'

const endpoint = process.env.NEXT_PUBLIC_GRAPHQL_URL || 'http://localhost:8081/graphql'

export const graphqlClient = new GraphQLClient(endpoint, {
  headers: {},
})

export function setAuthToken(token: string) {
  if (token) {
    graphqlClient.setHeader('Authorization', `Bearer ${token}`)
  } else {
    graphqlClient.setHeader('Authorization', '')
  }
}
