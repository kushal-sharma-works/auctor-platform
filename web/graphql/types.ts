export interface Workflow {
  id: string
  name: string
  version: number
  states: string[]
}

export interface GetDefinitionResponse {
  getWorkflow: Workflow
}
