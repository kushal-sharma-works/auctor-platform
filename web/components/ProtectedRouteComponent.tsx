"use client"

import { useRouter } from "next/navigation"
import { useEffect, useState } from "react"
import { useSelector } from "react-redux"
import type { RootState } from "@/store"

export function ProtectedRouteComponent({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const token = useSelector((state: RootState) => state.session.token)
  const [isLoading, setIsLoading] = useState(true)
  
  useEffect(() => {
    if (!token) {
      router.push("/login")
    } else {
      setIsLoading(false)
    }
  }, [token, router])
  
  if (isLoading) {
    return <div>Loading...</div>
  }
  
  return <>{children}</>
}
