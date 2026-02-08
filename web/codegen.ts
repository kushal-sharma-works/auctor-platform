import type { CodegenConfig } from '@graphql-codegen/cli'

const config: CodegenConfig = {
  schema: 'http://localhost:8081/graphql',
  documents: 'graphql/operations/**/*.graphql',
  generates: {
    'graphql/generated/': {
      preset: 'client',
      plugins: []
    }
  }
}
export default config
