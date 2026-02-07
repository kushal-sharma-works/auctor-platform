"use client"

import { useState } from "react"
import { useDispatch } from "react-redux"
import { setToken } from "../store/sessionSlice"

export function TokenSetter() {
  const [tokenInput, setTokenInput] = useState("")
  const dispatch = useDispatch()

  const handleSetToken = () => {
    if (tokenInput.trim()) {
      dispatch(setToken(tokenInput.trim()))
    }
  }

  const handleUseTestToken = async () => {
    // Show instructions for generating a real token
    alert('Run this command to generate a valid JWT token:\n\ncd services/execution-service && ./generate-token.sh\n\nThen copy the token and paste it here (including any Bearer prefix)')
  }

  return (
    <div style={{ padding: "20px", border: "1px solid #ccc", borderRadius: "8px", maxWidth: "500px", margin: "20px auto" }}>
      <h2>Set Authentication Token</h2>
      <div style={{ marginBottom: "10px" }}>
        <input
          type="text"
          value={tokenInput}
          onChange={(e) => setTokenInput(e.target.value)}
          placeholder="Paste your JWT token here"
          style={{ width: "100%", padding: "8px", marginBottom: "10px" }}
        />
      </div>
      <div style={{ display: "flex", gap: "10px" }}>
        <button onClick={handleSetToken} style={{ padding: "8px 16px", cursor: "pointer" }}>
          Set Token
        </button>
        <button onClick={handleUseTestToken} style={{ padding: "8px 16px", cursor: "pointer" }}>
          Use Test Token
        </button>
      </div>
      <p style={{ fontSize: "12px", marginTop: "10px", color: "#666" }}>
        Generate a real token by running: <code>./generate-token.sh</code> in the execution-service directory
      </p>
    </div>
  )
}
