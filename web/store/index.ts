import { configureStore } from "@reduxjs/toolkit"
import session from "./sessionSlice"

export const store = configureStore({
  reducer: { session }
})

export type RootState = ReturnType<typeof store.getState>
