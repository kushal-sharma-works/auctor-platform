import { describe, it, expect, vi } from "vitest"
import { NextRequest } from "next/server"
import { middleware } from "../middleware"

describe("middleware", () => {
  it("redirects to login when no token", () => {
    const request = new NextRequest(new URL("http://localhost/workflows"))
    const response = middleware(request)

    expect(response?.headers.get("location")).toBe("http://localhost/login")
  })

  it("allows request when token cookie present", () => {
    const url = new URL("http://localhost/workflows")
    const request = new NextRequest(url)
    
    // Mock the cookies.get method to return a token
    vi.spyOn(request.cookies, 'get').mockReturnValue({ name: 'token', value: 'abc123' })

    const response = middleware(request)

    expect(response?.headers.get("location")).toBeNull()
  })

  it("allows public login path", () => {
    const request = new NextRequest(new URL("http://localhost/login"))
    const response = middleware(request)

    expect(response?.headers.get("location")).toBeNull()
  })
})

