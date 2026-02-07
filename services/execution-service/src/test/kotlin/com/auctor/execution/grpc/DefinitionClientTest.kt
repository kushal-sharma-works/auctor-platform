package com.auctor.execution.grpc

import com.auctor.definition.grpc.v1.DefinitionServiceGrpcKt
import com.auctor.definition.grpc.v1.GetDefinitionRequest
import com.auctor.definition.grpc.v1.GetDefinitionResponse
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import java.util.UUID

class DefinitionClientTest {

    @Test
    fun `should call definition-service via grpc`() {
        val serverName = "definition-client-test-${UUID.randomUUID()}"
        val definitionService = object : DefinitionServiceGrpcKt.DefinitionServiceCoroutineImplBase() {
            override suspend fun getDefinition(request: GetDefinitionRequest): GetDefinitionResponse {
                return GetDefinitionResponse.newBuilder()
                    .setId(request.id)
                    .setName("sample-definition")
                    .setDescription("stored in database")
                    .build()
            }
        }

        val server = InProcessServerBuilder
            .forName(serverName)
            .directExecutor()
            .addService(definitionService)
            .build()
            .start()

        val channel = InProcessChannelBuilder
            .forName(serverName)
            .directExecutor()
            .build()

        val client = DefinitionGrpcClient(channel = channel)

        try {
            val response = runBlocking {
                client.getDefinition("123")
            }

            assertEquals("123", response?.id)
            assertEquals("sample-definition", response?.name)
            assertEquals("stored in database", response?.description)
        } finally {
            client.close()
            server.shutdownNow()
        }
    }
}
