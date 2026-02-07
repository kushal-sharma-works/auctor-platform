package com.auctor.execution.service

import com.auctor.execution.grpc.DefinitionGrpcClient
import com.auctor.execution.model.ExecutionResult
import com.auctor.execution.observability.ExecutionMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

class ExecutionService(
    private val definitionClient: DefinitionGrpcClient = DefinitionGrpcClient()
) {
    private val metrics = ExecutionMetrics(SimpleMeterRegistry())

    suspend fun execute(definitionId: String): ExecutionResult {
        val definition = definitionClient.getDefinition(definitionId)
            ?: throw IllegalArgumentException("Definition not found: $definitionId")
        
        metrics.executeCounter.increment()

        return ExecutionResult(
            id =definition.id,
            name = definition.name,
            description = definition.description ?: ""
        )
    }
}
