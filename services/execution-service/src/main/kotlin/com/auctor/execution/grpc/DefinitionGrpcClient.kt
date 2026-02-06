package com.auctor.execution.grpc

import com.auctor.definition.grpc.v1.DefinitionServiceGrpcKt
import com.auctor.definition.grpc.v1.GetDefinitionRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.util.concurrent.TimeUnit

class DefinitionGrpcClient : Closeable {

    private val channel: ManagedChannel
    private val stub: DefinitionServiceGrpcKt.DefinitionServiceCoroutineStub

    constructor(host: String = "localhost", port: Int = 9090) {
        channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .build()
        stub = DefinitionServiceGrpcKt.DefinitionServiceCoroutineStub(channel)
    }

    constructor(channel: ManagedChannel) {
        this.channel = channel
        stub = DefinitionServiceGrpcKt.DefinitionServiceCoroutineStub(channel)
    }

    suspend fun getDefinition(id: String) =
        withTimeout(1_000) {
            stub.getDefinition(
                GetDefinitionRequest.newBuilder()
                    .setId(id)
                    .build()
            )
        }

    override fun close() {
        channel.shutdown()
        channel.awaitTermination(5, TimeUnit.SECONDS)
    }
}
