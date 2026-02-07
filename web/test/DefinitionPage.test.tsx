import { render, screen } from "@testing-library/react"
import DefinitionsPage from "../app/definitions/page"
import { Provider } from "react-redux"
import { store } from "../store"
import { setToken } from "../store/sessionSlice"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"

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
