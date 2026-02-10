import { NextRequest } from "next/server"
import { middleware } from "../middleware"

describe("middleware", () => {
  it("redirects to login when no token", () => {
    const request = new NextRequest("http://localhost/workflows")
    const response = middleware(request)
    expect(response?.headers.get("location")).toBe("http://localhost/login?redirect=/workflows")
  })

  it("allows request with auth cookie", () => {
    const request = new NextRequest("http://localhost/workflows", {
      headers: {
        cookie: "auctor.auth.token=test-token",
      },
    })
    const response = middleware(request)
    expect(response?.headers.get("location")).toBeNull()
  })

  it("allows public login path", () => {
    const request = new NextRequest("http://localhost/login")
    const response = middleware(request)
    expect(response?.headers.get("location")).toBeNull()
  })

  it("skips auth for non-GET requests", () => {
    const request = new NextRequest("http://localhost/workflows", {
      method: "POST",
    })
    const response = middleware(request)
    expect(response?.headers.get("location")).toBeNull()
  })
})
