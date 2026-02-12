const getAuthHeader = (request: Request): string | undefined => {
  const direct = request.headers.get("authorization") || request.headers.get("Authorization")
  if (direct) return direct
  const cookie = request.headers.get("cookie") || ""
  const match = cookie
    .split(";")
    .map((item) => item.trim())
    .find((item) => item.startsWith("auctor.auth.token="))
  if (!match) return undefined
  const token = decodeURIComponent(match.split("=")[1] || "")
  if (!token) return undefined
  return token.startsWith("Bearer ") ? token : `Bearer ${token}`
}

export async function POST(request: Request): Promise<Response> {
  let body: string
  try {
    body = await request.text()
  } catch (error) {
    return new Response(
      JSON.stringify({ error: "Invalid request body" }),
      {
        status: 400,
        headers: {
          "Content-Type": "application/json"
        }
      }
    )
  }

  const authHeader = getAuthHeader(request)
  const executionUrl = process.env.EXECUTION_SERVICE_URL || "http://localhost:8082"
  const upstreamResponse = await fetch(`${executionUrl}/graphql`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(authHeader ? { Authorization: authHeader } : {})
    },
    body,
  })

  const responseBody = await upstreamResponse.text()
  const contentType = upstreamResponse.headers.get("content-type") || "application/json"

  return new Response(responseBody, {
    status: upstreamResponse.status,
    headers: {
      "Content-Type": contentType,
    },
  })
}

export function GET(): Response {
  return new Response(
    JSON.stringify({
      message: "GraphQL endpoint expects POST requests at /api/graphql"
    }),
    {
      status: 200,
      headers: {
        "Content-Type": "application/json"
      }
    }
  )
}

export function OPTIONS(): Response {
  return new Response(null, {
    status: 204,
    headers: {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization"
    }
  })
}
