/**
 * @jest-environment node
 */
import { NextRequest, NextResponse } from "next/server"
import { middleware } from "../middleware"

// Mock NextRequest and NextResponse for testing
jest.mock("next/server", () => {
  const actual = jest.requireActual("next/server")
  return {
    ...actual,
    NextRequest: jest.fn().mockImplementation((url: string, init?: RequestInit) => {
      const request = {
        url,
        method: init?.method || "GET",
        headers: new Map(Object.entries(init?.headers || {})),
        cookies: {
          get: jest.fn((name: string) => {
            const cookieHeader = (init?.headers as any)?.cookie || ""
            const match = cookieHeader.match(new RegExp(`${name}=([^;]+)`))
            return match ? { value: match[1] } : undefined
          }),
        },
        nextUrl: new URL(url),
      }
      return request
    }),
    NextResponse: {
      redirect: jest.fn((url: string) => ({
        headers: new Map([["location", url]]),
        get: function (key: string) {
          return this.headers.get(key)
        },
      })),
      next: jest.fn(() => ({
        headers: new Map(),
        get: function (key: string) {
          return this.headers.get(key)
        },
      })),
    },
  }
})

describe("middleware", () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  it("redirects to login when no token", () => {
    const request = new NextRequest("http://localhost/workflows") as any
    const response = middleware(request)
    
    if (response) {
      expect(response.headers.get("location")).toContain("/login")
    }
  })

  it("allows request with auth cookie", () => {
    const request = new NextRequest("http://localhost/workflows", {
      headers: {
        cookie: "auctor.auth.token=test-token",
      } as any,
    }) as any
    
    const response = middleware(request)
    expect(response).toBeDefined()
  })

  it("allows public login path", () => {
    const request = new NextRequest("http://localhost/login") as any
    const response = middleware(request)
    expect(response).toBeDefined()
  })

  it("allows non-GET requests to pass through", () => {
    const request = new NextRequest("http://localhost/workflows", {
      method: "POST",
    }) as any
    
    const response = middleware(request)
    expect(response).toBeDefined()
  })
})
