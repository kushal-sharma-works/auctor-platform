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
  } catch {
    return new Response(
      JSON.stringify({ error: "Invalid request body" }),
      {
        status: 400,
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*",
        },
      }
    )
  }

  try {
    const authHeader = getAuthHeader(request)
    const definitionUrl = process.env.DEFINITION_SERVICE_URL || "http://localhost:8081"
    const upstreamResponse = await fetch(`${definitionUrl}/graphql`, {
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
        "Access-Control-Allow-Origin": "*",
      },
    })
  } catch {
    return new Response(
      JSON.stringify({ error: "Definition service unavailable" }),
      {
        status: 502,
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*",
        },
      }
    )
  }
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
