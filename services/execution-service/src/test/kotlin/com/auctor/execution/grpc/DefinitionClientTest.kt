package com.auctor.execution.grpc

import com.auctor.definition.grpc.v1.DefinitionServiceGrpc
import com.auctor.definition.grpc.v1.GetDefinitionRequest
import io.grpc.ManagedChannelBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class DefinitionClientTest {

    @Test
    fun `should call definition-service via grpc`() {
        val channel = ManagedChannelBuilder
            .forAddress("localhost", 9090)
            .usePlaintext()
            .build()

        val stub = DefinitionServiceGrpc.newBlockingStub(channel)

        val response = stub.getDefinition(
            GetDefinitionRequest.newBuilder()
                .setId("123")
                .build()
        )

        assertEquals("123", response.id)
        assertEquals("sample-definition", response.name)
        assertEquals("mock definition response", response.description)

        channel.shutdown()
    }
}
