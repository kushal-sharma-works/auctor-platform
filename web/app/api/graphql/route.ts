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

  const upstreamResponse = await fetch("http://localhost:8082/graphql", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
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
      "Access-Control-Allow-Headers": "Content-Type"
    }
  })
}
