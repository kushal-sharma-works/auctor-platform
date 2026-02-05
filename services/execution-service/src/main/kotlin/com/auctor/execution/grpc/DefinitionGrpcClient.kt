package com.auctor.execution.grpc

import com.auctor.definition.grpc.v1.DefinitionServiceGrpc
import com.auctor.definition.grpc.v1.GetDefinitionRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

class DefinitionGrpcClient(
    host: String = "localhost",
    port: Int = 9090
) {

    private val channel: ManagedChannel =
        ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build()

    private val stub: DefinitionServiceGrpc.DefinitionServiceBlockingStub =
        DefinitionServiceGrpc.newBlockingStub(channel)

    fun getDefinition(id: String): String {
        val request = GetDefinitionRequest.newBuilder()
            .setId(id)
            .build()

        val response = stub.getDefinition(request)

        return "id=${response.id}, name=${response.name}, desc=${response.description}"
    }

    fun shutdown() {
        channel.shutdown()
    }
}
