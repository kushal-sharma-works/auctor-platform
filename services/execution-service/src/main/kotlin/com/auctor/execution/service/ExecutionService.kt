package com.auctor.execution.service

import com.auctor.execution.grpc.DefinitionGrpcClient
import com.auctor.execution.model.ExecutionResult

class ExecutionService(
    private val definitionClient: DefinitionGrpcClient = DefinitionGrpcClient()
) {

    suspend fun execute(definitionId: String): ExecutionResult {
        val definition = definitionClient.getDefinition(definitionId)

        return ExecutionResult(
            id = definition.id,
            name = definition.name,
            description = definition.description
        )
    }
}
