import { NextResponse } from "next/server"
import { signJwt } from "../../../../lib/jwt-server"
import { isAdminEmail } from "../../../../lib/admins"

const devLoginEnabled = process.env.ENABLE_DEV_LOGIN === "true"

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

const buildRoles = (email: string): string[] => {
  const roles = new Set<string>(["EXECUTOR"])
  if (isAdminEmail(email)) {
    roles.add("ADMIN")
  }
  return Array.from(roles)
}

export async function POST(request: Request) {
  if (!devLoginEnabled) {
    return NextResponse.json({ error: "Dev login disabled" }, { status: 404 })
  }

  try {
    const body = await request.json()
    const username = String(body?.username || "").trim()
    const password = String(body?.password || "").trim()

    if (!username || !password) {
      return NextResponse.json({ error: "Username and password are required" }, { status: 400 })
    }

    const email = username.includes("@") ? username : `${username}@local.test`
    const roles = buildRoles(email)

    const now = Math.floor(Date.now() / 1000)
    const token = signJwt(
      {
        sub: email,
        email,
        roles,
        iss: getIssuer(),
        aud: getAudience(),
        iat: now,
        exp: now + 60 * 60,
      },
      getSecret()
    )

    const response = NextResponse.json({ token })
    response.cookies.set("auctor.auth.token", token, {
      httpOnly: false,
      sameSite: "lax",
      path: "/",
      secure: process.env.NODE_ENV === "production",
      maxAge: 60 * 60,
    })
    return response
  } catch (error) {
    return NextResponse.json({ error: "Invalid request" }, { status: 400 })
  }
}
