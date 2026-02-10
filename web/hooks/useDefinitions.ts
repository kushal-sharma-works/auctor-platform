"use client"

import { useQuery } from "@tanstack/react-query"
import { useSelector } from "react-redux"
import { requestDefinitionGraphQL } from "../graphql/client"
import { GET_DEFINITIONS } from "../graphql/queries"
import { GetDefinitionResponse } from "../graphql/types"
import type { RootState } from "../store"

export function useDefinitions() {
  const token = useSelector((state: RootState) => state.session.token)
  return useQuery({
    queryKey: ["definitions", token],
    queryFn: () =>
      requestDefinitionGraphQL<GetDefinitionResponse>(GET_DEFINITIONS, undefined, token || undefined),
    enabled: Boolean(token),
    staleTime: 30_000,
    retry: 1,
    retryDelay: 1000
  })
}
