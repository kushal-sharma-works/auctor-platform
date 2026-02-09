export async function POST(request: Request): Promise<Response> {
  const body = await request.text()

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
