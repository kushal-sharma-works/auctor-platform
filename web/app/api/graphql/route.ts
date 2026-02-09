export async function POST(request: Request): Promise<Response> {
  const body = await request.text()
  const authHeader = request.headers.get("authorization")

  const upstreamResponse = await fetch("http://localhost:8081/graphql", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(authHeader ? { Authorization: authHeader } : {}),
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
