package com.auctor.execution.grpc

import com.auctor.execution.security.AuthContext
import io.grpc.*

class AuthGrpcClientInterceptor(
    private val authContext: AuthContext
) : ClientInterceptor {

    companion object {
        val AUTHORIZATION_KEY: Metadata.Key<String> =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

        val USER_ID_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER)

        val ROLES_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-roles", Metadata.ASCII_STRING_MARSHALLER)
    }

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {

        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)
        ) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                headers.put(USER_ID_KEY, authContext.subject)
                headers.put(ROLES_KEY, authContext.roles.joinToString(","))
                authContext.rawToken?.let { headers.put(AUTHORIZATION_KEY, it) }

                super.start(responseListener, headers)
            }
        }
    }
}
