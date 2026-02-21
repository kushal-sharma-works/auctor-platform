import { createSlice, PayloadAction } from "@reduxjs/toolkit"

interface SessionState {
  token: string | null
  subject: string | null
  roles: string[]
}

const initialState: SessionState = { token: null, subject: null, roles: [] }

const slice = createSlice({
  name: "session",
  initialState,
  reducers: {
    setSession(state, action: PayloadAction<{ token: string; subject: string; roles: string[] }>) {
      state.token = action.payload.token
      state.subject = action.payload.subject
      state.roles = action.payload.roles
    },
    clearToken(state) {
      state.token = null
      state.subject = null
      state.roles = []
    }
  }
})

export const { setSession, clearToken } = slice.actions
export default slice.reducer
