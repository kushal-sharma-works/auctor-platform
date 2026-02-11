import sessionReducer, { setSession, clearToken } from "../store/sessionSlice"

describe("sessionSlice", () => {
  const initialState = {
    token: null,
    subject: null,
    roles: [],
  }

  it("returns initial state", () => {
    expect(sessionReducer(undefined, { type: "unknown" })).toEqual(initialState)
  })

  describe("setSession", () => {
    it("sets session data", () => {
      const payload = {
        token: "test-token",
        subject: "user@example.com",
        roles: ["ADMIN", "VIEWER"],
      }

      const state = sessionReducer(initialState, setSession(payload))

      expect(state).toEqual(payload)
    })

    it("overwrites existing session", () => {
      const existingState = {
        token: "old-token",
        subject: "old-user",
        roles: ["VIEWER"],
      }

      const payload = {
        token: "new-token",
        subject: "new-user",
        roles: ["ADMIN"],
      }

      const state = sessionReducer(existingState, setSession(payload))

      expect(state).toEqual(payload)
    })

    it("handles empty roles array", () => {
      const payload = {
        token: "test-token",
        subject: "user",
        roles: [],
      }

      const state = sessionReducer(initialState, setSession(payload))

      expect(state.roles).toEqual([])
    })
  })

  describe("clearToken", () => {
    it("clears session data", () => {
      const existingState = {
        token: "test-token",
        subject: "user@example.com",
        roles: ["ADMIN"],
      }

      const state = sessionReducer(existingState, clearToken())

      expect(state).toEqual(initialState)
    })

    it("resets to initial state from any state", () => {
      const state1 = sessionReducer(
        { token: "token1", subject: "user1", roles: ["ROLE1"] },
        clearToken()
      )

      const state2 = sessionReducer(
        { token: "token2", subject: "user2", roles: ["ROLE2", "ROLE3"] },
        clearToken()
      )

      expect(state1).toEqual(initialState)
      expect(state2).toEqual(initialState)
    })
  })

  describe("state immutability", () => {
    it("does not mutate original state", () => {
      const originalState = {
        token: "original-token",
        subject: "original-user",
        roles: ["ROLE1"],
      }

      const stateCopy = { ...originalState }

      sessionReducer(originalState, clearToken())

      expect(originalState).toEqual(stateCopy)
    })
  })
})
