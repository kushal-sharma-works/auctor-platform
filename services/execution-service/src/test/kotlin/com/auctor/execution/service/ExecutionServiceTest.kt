package com.auctor.execution.service

import com.auctor.execution.grpc.DefinitionGrpcClient
import com.auctor.execution.grpc.DefinitionDto
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import com.auctor.definition.grpc.v1.GetDefinitionResponse
import kotlin.test.assertEquals

class ExecutionServiceTest {

    private val grpcClient = mock<DefinitionGrpcClient>()
    private val service = ExecutionService(grpcClient)

    @Test
    fun `execute returns mapped result`() = runBlocking {
        whenever(grpcClient.getDefinition("123")).thenReturn(
            DefinitionDto(
                id = "123",
                name = "test",
                description = "desc"
            )
        )

        val result = service.execute("123")

        assertEquals("123", result.id)
        assertEquals("test", result.name)
        assertEquals("desc", result.description)
    }
}
