import { Workflow } from "../graphql/types"

export function DefinitionCard({ def }: { def: Workflow }) {
  return (
    <div>
      <div><strong>Name:</strong> {def.name}</div>
      <div><strong>ID:</strong> {def.id}</div>
      <div><strong>Version:</strong> {def.version}</div>
      <div><strong>States:</strong> {def.states.join(", ")}</div>
    </div>
  )
}
