import { NextRequest, NextResponse } from 'next/server'

// Dev token for testing - TODO: implement proper auth
const DEV_TOKEN = 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhdWN0b3ItYXV0aCIsImF1ZCI6ImV4ZWN1dGlvbi1zZXJ2aWNlIiwic3ViIjoidGVzdC11c2VyIiwicm9sZXMiOlsiRVhFQ1VUT1IiXX0.gCCPXuJsb7HxYjcx7EskPN-cAsgv5doU1pA_nxjo9j8'

/**
 * Proxy for Execution Service REST API
 * Forwards requests from the browser to the execution service backend
 * Handles CORS and authentication headers
 */
export async function GET(
  request: NextRequest,
  { params }: any
) {
  const pathSegments = (params?.path || []) as string[]
  const path = pathSegments.join('/')

  const executionServiceUrl = process.env.EXECUTION_SERVICE_URL || 'http://localhost:8082'
  const targetUrl = new URL(`/api/v1/${path}`, executionServiceUrl)

  // Forward query parameters
  request.nextUrl.searchParams.forEach((value, key) => {
    targetUrl.searchParams.append(key, value)
  })

  try {
    const clientToken = request.cookies.get('token')?.value || request.headers.get('authorization')
    const token = clientToken || DEV_TOKEN // Use dev token if no client token

    const response = await fetch(targetUrl.toString(), {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}`,
      },
    })

    const contentType = response.headers.get('content-type')
    let data: unknown = null

    if (contentType?.includes('application/json')) {
      data = await response.json()
    } else {
      const text = await response.text()
      data = text ? { message: text } : null
    }

    return NextResponse.json(data ?? { status: 'ok' }, { status: response.status })
  } catch (error) {
    console.error('Error proxying GET request to execution service:', error)
    return NextResponse.json(
      {
        error: 'Failed to fetch from execution service',
        details: error instanceof Error ? error.message : String(error),
      },
      { status: 500 }
    )
  }
}

export async function POST(
  request: NextRequest,
  { params }: any
) {
  const pathSegments = (params?.path || []) as string[]
  const path = pathSegments.join('/')

  const executionServiceUrl = process.env.EXECUTION_SERVICE_URL || 'http://localhost:8082'
  const targetUrl = new URL(`/api/v1/${path}`, executionServiceUrl)

  try {
    const clientToken = request.cookies.get('token')?.value || request.headers.get('authorization')
    const token = clientToken || DEV_TOKEN // Use dev token if no client token
    const body = await request.text()

    const response = await fetch(targetUrl.toString(), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}`,
      },
      body: body || undefined,
    })

    const contentType = response.headers.get('content-type')
    let data: unknown = null

    if (contentType?.includes('application/json')) {
      data = await response.json()
    } else {
      const text = await response.text()
      data = text ? { message: text } : null
    }

    return NextResponse.json(data ?? { status: 'ok' }, { status: response.status })
  } catch (error) {
    console.error('Error proxying POST request to execution service:', error)
    return NextResponse.json(
      {
        error: 'Failed to fetch from execution service',
        details: error instanceof Error ? error.message : String(error),
      },
      { status: 500 }
    )
  }
}
