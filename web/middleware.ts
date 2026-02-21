import { NextRequest, NextResponse } from "next/server"

const PUBLIC_PATHS = ["/login", "/api/auth"]

const isPublicPath = (pathname: string) =>
  PUBLIC_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`)) ||
  pathname.startsWith("/_next") ||
  pathname.startsWith("/favicon.ico")

export function middleware(request: NextRequest) {
  if (request.method !== "GET") {
    return NextResponse.next()
  }
  const { pathname } = request.nextUrl
  if (isPublicPath(pathname)) {
    return NextResponse.next()
  }

  const token = request.cookies.get("auctor.auth.token")?.value || request.headers.get("authorization")
  if (!token) {
    const loginUrl = request.nextUrl.clone()
    loginUrl.pathname = "/login"
    loginUrl.searchParams.set("redirect", pathname)
    return NextResponse.redirect(loginUrl)
  }

  return NextResponse.next()
}

export const config = {
  matcher: ["/((?!api|_next|favicon.ico).*)"],
}
