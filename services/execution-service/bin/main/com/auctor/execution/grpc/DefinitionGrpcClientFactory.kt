package com.auctor.execution.grpc

import com.auctor.definition.grpc.v1.DefinitionServiceGrpc
import com.auctor.execution.security.AuthContext
import io.grpc.ManagedChannelBuilder
import io.grpc.ClientInterceptors

object DefinitionGrpcClientFactory {

    fun create(authContext: AuthContext): DefinitionServiceGrpc.DefinitionServiceBlockingStub {
        val channel =
            ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build()

        val interceptedChannel =
            ClientInterceptors.intercept(
                channel,
                AuthGrpcClientInterceptor(authContext)
            )

        return DefinitionServiceGrpc.newBlockingStub(interceptedChannel)
    }
}
