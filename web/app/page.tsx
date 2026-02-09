"use client"

import { useSelector } from "react-redux"
import { RootState } from "../store"
import { useDefinitions } from "../hooks/useDefinitions"
import { DefinitionCard } from "../components/DefinitionCard"
import { ProtectedRoute } from "../components/ProtectedRoute"
import { TokenSetter } from "../components/TokenSetter"

export default function DefinitionsPage() {
  const token = useSelector((s: RootState) => s.session.token)

  if (!token) {
    return <TokenSetter />
  }

  return <DefinitionsPageContent token={token} />
}

function DefinitionsPageContent({ token }: { token: string }) {
  const { data, isLoading, error } = useDefinitions(token)

  if (isLoading) return <p>Loading…</p>
  if (error) return <p>Error: {error.message}</p>
  if (!data?.getWorkflow) return <p>No workflow found</p>

  return <DefinitionCard def={data.getWorkflow} />
}
