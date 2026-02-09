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
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*",
        },
      }
    )
  }

  try {
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
        "Access-Control-Allow-Origin": "*",
      },
    })
  } catch (error) {
    return new Response(
      JSON.stringify({ error: "Execution service unavailable" }),
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
      "Access-Control-Allow-Headers": "Content-Type"
    }
  })
}
