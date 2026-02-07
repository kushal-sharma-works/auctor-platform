import { useQuery } from "@tanstack/react-query"
import { graphqlRequest } from "../graphql/client"
import { GET_DEFINITIONS } from "../graphql/queries"
import { GetDefinitionResponse } from "../graphql/types"

export function useDefinitions(token: string) {
  return useQuery({
    queryKey: ["definitions"],
    queryFn: () =>
      graphqlRequest<GetDefinitionResponse>(GET_DEFINITIONS, token),
    staleTime: 30_000
  })
}
