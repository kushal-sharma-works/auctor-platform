"use client"

import { useSelector } from "react-redux"
import { RootState } from "../store"

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = useSelector((s: RootState) => s.session.token)
  if (!token) return <p>Unauthorized</p>
  return <>{children}</>
}
