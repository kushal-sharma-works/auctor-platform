import { NextResponse } from "next/server"
import { jwtVerify, createRemoteJWKSet } from "jose"
import { signJwt } from "../../../../lib/jwt-server"
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

const buildRoles = (email: string): string[] => {
  const roles = new Set<string>(["EXECUTOR"])
  if (isAdminEmail(email)) {
    roles.add("ADMIN")
  }
  return Array.from(roles)
}

export async function POST(request: Request) {
  try {
    const { idToken } = await request.json()
    if (!idToken) {
      return NextResponse.json({ error: "Missing idToken" }, { status: 400 })
    }

    const googleClientId = process.env.GOOGLE_CLIENT_ID || process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID
    if (!googleClientId) {
      return NextResponse.json({ error: "GOOGLE_CLIENT_ID is not configured" }, { status: 500 })
    }

    const jwks = createRemoteJWKSet(
      new URL("https://www.googleapis.com/oauth2/v3/certs")
    )

    const { payload } = await jwtVerify(idToken, jwks, {
      issuer: ["https://accounts.google.com", "accounts.google.com"],
      audience: googleClientId,
    })

    const email = payload.email as string | undefined
    const subject = payload.sub as string | undefined
    if (!email || !subject) {
      return NextResponse.json({ error: "Google token missing email" }, { status: 400 })
    }

    const roles = buildRoles(email)
    const now = Math.floor(Date.now() / 1000)
    const token = signJwt(
      {
        sub: subject,
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
