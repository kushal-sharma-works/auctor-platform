import { Definition } from "../graphql/types"

export function DefinitionCard({ def }: { def: Definition }) {
  return (
    <div>
      <div><strong>Name:</strong> {def.name}</div>
      <div><strong>ID:</strong> {def.id}</div>
      <div><strong>Description:</strong> {def.description}</div>
    </div>
  )
}
