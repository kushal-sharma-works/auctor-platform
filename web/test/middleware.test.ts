/**
 * @jest-environment node
 */
import { NextRequest } from "next/server"
import { middleware } from "../middleware"

describe("middleware", () => {
  it("redirects to login when no token", () => {
    const request = new NextRequest("http://localhost/workflows")
    const response = middleware(request)
    
    expect(response).toBeDefined()
    expect(response.status).toBe(307)
    const location = response.headers.get("location")
    expect(location).toContain("/login")
    expect(location).toContain("redirect=%2Fworkflows")
  })

  it("allows request with auth cookie", () => {
    const request = new NextRequest("http://localhost/workflows", {
      headers: {
        cookie: "auctor.auth.token=test-token",
      },
    })
    
    const response = middleware(request)
    expect(response).toBeDefined()
    // NextResponse.next() returns a response with 200 status
    expect(response.status).toBe(200)
  })

  it("allows public login path", () => {
    const request = new NextRequest("http://localhost/login")
    const response = middleware(request)
    expect(response).toBeDefined()
    expect(response.status).toBe(200)
  })

  it("allows non-GET requests to pass through", () => {
    const request = new NextRequest("http://localhost/workflows", {
      method: "POST",
    })
    
    const response = middleware(request)
    expect(response).toBeDefined()
    expect(response.status).toBe(200)
  })
})
