import { setupServer } from "msw/node"
import { graphql } from "msw"

export const server = setupServer(
  graphql.query("getDefinition", (_, res, ctx) => {
    return res(
      ctx.data({
        getDefinition: {
          id: "123",
          name: "mock",
          description: "desc"
        }
      })
    )
  })
)
