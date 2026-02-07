export async function graphqlRequest<T>(
  query: string,
  token?: string
): Promise<T> {
  try {
    const headers: Record<string, string> = {
      "Content-Type": "application/json"
    }
    
    if (token) {
      // Add Bearer prefix if not already present
      const authToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`
      headers["Authorization"] = authToken
    }
    
    const res = await fetch(process.env.NEXT_PUBLIC_GRAPHQL_URL!, {
      method: "POST",
      headers,
      body: JSON.stringify({ query })
    })

    if (!res.ok) {
      throw new Error(`GraphQL server error: ${res.status} ${res.statusText}`)
    }

    const json = await res.json()
    
    if (json.errors) {
      throw new Error(`GraphQL error: ${json.errors.map((e: any) => e.message).join(', ')}`)
    }
    
    return json.data
  } catch (error) {
    console.error('GraphQL request failed:', error)
    throw error
  }
}
