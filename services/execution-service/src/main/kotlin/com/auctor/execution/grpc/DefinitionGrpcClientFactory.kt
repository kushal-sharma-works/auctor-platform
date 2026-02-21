package com.auctor.execution.grpc

import com.auctor.definition.grpc.v1.DefinitionServiceGrpc
import com.auctor.execution.security.AuthContext
import io.grpc.ClientInterceptors
import io.grpc.ManagedChannelBuilder
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry

object DefinitionGrpcClientFactory {
    private val otelClientInterceptor =
        GrpcTelemetry.create(GlobalOpenTelemetry.get()).newClientInterceptor()

    fun create(authContext: AuthContext): DefinitionServiceGrpc.DefinitionServiceBlockingStub {
        val channel =
            ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build()

        val interceptedChannel =
            ClientInterceptors.intercept(
                channel,
                AuthGrpcClientInterceptor(authContext),
                otelClientInterceptor
            )

        return DefinitionServiceGrpc.newBlockingStub(interceptedChannel)
    }
}
