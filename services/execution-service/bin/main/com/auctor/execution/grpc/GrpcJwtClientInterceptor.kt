package com.auctor.execution.grpc

import io.grpc.*

class GrpcJwtClientInterceptor(
    private val token: String
) : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {

        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)
        ) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                val key = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
                headers.put(key, "Bearer $token")
                super.start(responseListener, headers)
            }
        }
    }
}
