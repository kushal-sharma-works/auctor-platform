import { useQuery } from "@tanstack/react-query"
import { requestDefinitionGraphQL } from "../graphql/client"
import { GET_DEFINITIONS } from "../graphql/queries"
import { GetDefinitionResponse } from "../graphql/types"

export function useDefinitions() {
  return useQuery({
    queryKey: ["definitions"],
    queryFn: () =>
      requestDefinitionGraphQL<GetDefinitionResponse>(GET_DEFINITIONS),
    staleTime: 30_000,
    retry: 1,
    retryDelay: 1000
  })
}
