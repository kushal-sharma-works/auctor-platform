import { NextResponse } from "next/server"

const shouldUseSecureCookies = (request: Request): boolean => {
  const forwardedProto = request.headers.get("x-forwarded-proto")
  if (forwardedProto) {
    return forwardedProto.split(",")[0]?.trim() === "https"
  }
  return new URL(request.url).protocol === "https:"
}

export async function POST(request: Request) {
  const response = NextResponse.json({ ok: true })
  response.cookies.set("auctor.auth.token", "", {
    httpOnly: false,
    sameSite: "lax",
    path: "/",
    secure: shouldUseSecureCookies(request),
    maxAge: 0,
  })
  return response
}
