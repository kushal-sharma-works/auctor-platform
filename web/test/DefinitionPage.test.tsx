import { render, screen } from "@testing-library/react"
import DefinitionsPage from "../app/page"
import { Provider } from "react-redux"
import { store } from "../store"
import { setToken } from "../store/sessionSlice"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"

jest.mock("../graphql/client", () => ({
  graphqlRequest: jest.fn().mockResolvedValue({
    getWorkflow: {
      id: "123",
      name: "mock",
      version: 1,
      states: ["DRAFT", "APPROVED"],
      description: "desc"
    }
  })
}))

test("renders definition", async () => {
  store.dispatch(setToken("Bearer test"))

  render(
    <Provider store={store}>
      <QueryClientProvider client={new QueryClient()}>
        <DefinitionsPage />
      </QueryClientProvider>
    </Provider>
  )

  expect(await screen.findByText("mock")).toBeInTheDocument()
})
