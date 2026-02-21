package com.auctor.definition.observability;

import io.grpc.*;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.stereotype.Component;

@Component
@GrpcGlobalServerInterceptor
public class GrpcTracingInterceptor implements ServerInterceptor {

    private final ServerInterceptor delegate;

    public GrpcTracingInterceptor(OpenTelemetry openTelemetry) {
        this.delegate = GrpcTelemetry.create(openTelemetry).newServerInterceptor();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call,
        Metadata headers,
        ServerCallHandler<ReqT, RespT> next
    ) {
        return delegate.interceptCall(call, headers, next);
    }
}
