package com.auctor.execution.grpc

import com.auctor.definition.grpc.v1.DefinitionServiceGrpc
import com.auctor.definition.grpc.v1.GetDefinitionRequest
import io.grpc.ManagedChannelBuilder

class DefinitionClient {

    private val channel = ManagedChannelBuilder
        .forAddress("localhost", 9090)
        .usePlaintext()
        .build()

    private val stub = DefinitionServiceGrpc.newBlockingStub(channel)

    fun getDefinition(id: String): String {
        val response = stub.getDefinition(
            GetDefinitionRequest.newBuilder()
                .setId(id)
                .build()
        )

        return "${response.id} | ${response.name} | ${response.description}"
    }
}
