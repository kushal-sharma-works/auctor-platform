import { GraphQLClient } from "graphql-request"
import { ClientError } from "graphql-request"
import { clearStoredToken, getStoredToken, setStoredToken } from "../store/token"

const refreshToken = async () => {
  const currentToken = getStoredToken()
  if (!currentToken) return null
  const response = await fetch("/api/auth/refresh", {
    method: "POST",
    headers: {
      Authorization: currentToken.startsWith("Bearer ") ? currentToken : `Bearer ${currentToken}`,
    },
  })
  if (!response.ok) return null
  const data = await response.json()
  if (data?.token) {
    setStoredToken(data.token)
    return data.token as string
  }
  return null
}

const definitionClient = new GraphQLClient("/api/definition-graphql")
const executionClient = new GraphQLClient("/api/execution-graphql")

const normalizeAuthHeader = (token?: string) => {
  if (!token) return undefined
  return token.startsWith("Bearer ") ? token : `Bearer ${token}`
}

const handleAuthError = async (error: unknown) => {
  if (error instanceof ClientError && error.response?.status === 401) {
    const refreshed = await refreshToken()
    if (refreshed) {
      return refreshed
    }
    clearStoredToken()
    if (typeof window !== "undefined") {
      window.location.href = "/login"
    }
  }
  return null
}

export async function requestDefinitionGraphQL<T>(
  query: string,
  variables?: Record<string, unknown>,
  token?: string
): Promise<T> {
  const authHeader = normalizeAuthHeader(token)
  const headers = authHeader ? { Authorization: authHeader } : undefined
  try {
    return await definitionClient.request<T>(query, variables, headers)
  } catch (error) {
    const refreshed = await handleAuthError(error)
    if (refreshed) {
      const retryHeader = normalizeAuthHeader(refreshed)
      return await definitionClient.request<T>(query, variables, retryHeader ? { Authorization: retryHeader } : undefined)
    }
    throw error
  }
}

export async function requestExecutionGraphQL<T>(
  query: string,
  variables?: Record<string, unknown>,
  token?: string
): Promise<T> {
  const authHeader = normalizeAuthHeader(token)
  const headers = authHeader ? { Authorization: authHeader } : undefined
  try {
    return await executionClient.request<T>(query, variables, headers)
  } catch (error) {
    const refreshed = await handleAuthError(error)
    if (refreshed) {
      const retryHeader = normalizeAuthHeader(refreshed)
      return await executionClient.request<T>(query, variables, retryHeader ? { Authorization: retryHeader } : undefined)
    }
    throw error
  }
}

// Legacy function for backward compatibility
export async function graphqlRequest<T>(
  query: string,
  token?: string,
  variables?: Record<string, any>
): Promise<T> {
  const authHeader = normalizeAuthHeader(token)
  const headers = authHeader ? { Authorization: authHeader } : undefined
  try {
    return await executionClient.request<T>(query, variables, headers)
  } catch (error) {
    const refreshed = await handleAuthError(error)
    if (refreshed) {
      const retryHeader = normalizeAuthHeader(refreshed)
      return await executionClient.request<T>(query, variables, retryHeader ? { Authorization: retryHeader } : undefined)
    }
    throw error
  }
}
