import { NextResponse } from "next/server"
import { signJwt, verifyJwt } from "../../../../lib/jwt-server"
import { isAdminEmail } from "../../../../lib/admins"

const getSecret = () =>
  process.env.AUCTOR_JWT_SECRET ||
  process.env.DEFINITION_JWT_SECRET ||
  process.env.EXECUTION_JWT_SECRET ||
  "dev-secret-change-later-dev-secret-change-later"

const getIssuer = () => process.env.AUCTOR_JWT_ISSUER || "auctor-auth"

const getAudience = () =>
  (process.env.AUCTOR_JWT_AUDIENCE || "definition-service,execution-service")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)

export async function POST(request: Request) {
  const token = request.headers.get("authorization")?.replace("Bearer ", "")
    || request.headers.get("Authorization")?.replace("Bearer ", "")
    || request.headers.get("cookie")
      ?.split(";")
      .map((item) => item.trim())
      .find((item) => item.startsWith("auctor.auth.token="))
      ?.split("=")[1]

  if (!token) {
    return NextResponse.json({ error: "Missing token" }, { status: 401 })
  }

  const verify = verifyJwt(token, getSecret())
  if (!verify.valid || !verify.claims) {
    return NextResponse.json({ error: verify.error || "Invalid token" }, { status: 401 })
  }

  const now = Math.floor(Date.now() / 1000)
  const existingRoles = Array.isArray(verify.claims.roles)
    ? verify.claims.roles.map((role) => String(role))
    : []
  const roleSet = new Set(existingRoles)
  roleSet.add("EXECUTOR")
  const email = typeof verify.claims.email === "string" ? verify.claims.email : ""
  if (email && isAdminEmail(email)) {
    roleSet.add("ADMIN")
  }

  const newToken = signJwt(
    {
      ...verify.claims,
      roles: Array.from(roleSet),
      iss: getIssuer(),
      aud: getAudience(),
      iat: now,
      exp: now + 60 * 60,
    },
    getSecret()
  )

  const response = NextResponse.json({ token: newToken })
  response.cookies.set("auctor.auth.token", newToken, {
    httpOnly: false,
    sameSite: "lax",
    path: "/",
    secure: process.env.NODE_ENV === "production",
    maxAge: 60 * 60,
  })
  return response
}
