import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest'
import { server } from './msw'

describe('MSW', () => {
  beforeAll(() => server.listen())
  afterEach(() => server.resetHandlers())
  afterAll(() => server.close())

  it('intercepts graphql requests', async () => {
    const response = await fetch('/api/graphql', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        query: 'query ListWorkflows { workflows(page: 0, size: 10) { content { id name } } }',
      }),
    })

    const data = await response.json()
    expect(data.data).toBeDefined()
    expect(data.data.workflows).toBeDefined()
    expect(data.data.workflows.content).toHaveLength(2)
  })
})
