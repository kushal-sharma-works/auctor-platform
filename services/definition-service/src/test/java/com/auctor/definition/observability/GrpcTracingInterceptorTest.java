package com.auctor.definition.observability;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class GrpcTracingInterceptorTest {

    @Test
    void shouldDelegateToGrpcTelemetryInterceptor() {
        GrpcTracingInterceptor interceptor = new GrpcTracingInterceptor(OpenTelemetry.noop());

        AtomicBoolean called = new AtomicBoolean(false);
        ServerCallHandler<String, String> next = (call, headers) -> {
            called.set(true);
            return new ServerCall.Listener<>() {};
        };

        ServerCall.Listener<String> listener = interceptor.interceptCall(
            new TestServerCall<>("definition.Test/Trace"),
            new Metadata(),
            next
        );

        assertNotNull(listener);
        assertTrue(called.get());
    }

    private static final class TestServerCall<ReqT, RespT> extends ServerCall<ReqT, RespT> {

        private final MethodDescriptor<ReqT, RespT> methodDescriptor;

        private TestServerCall(String fullMethodName) {
            this.methodDescriptor = MethodDescriptor.<ReqT, RespT>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(fullMethodName)
                .setRequestMarshaller(new NoopMarshaller<>())
                .setResponseMarshaller(new NoopMarshaller<>())
                .build();
        }

        @Override
        public void request(int numMessages) {
        }

        @Override
        public void sendHeaders(Metadata headers) {
        }

        @Override
        public void sendMessage(RespT message) {
        }

        @Override
        public void close(Status status, Metadata trailers) {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public MethodDescriptor<ReqT, RespT> getMethodDescriptor() {
            return methodDescriptor;
        }

        @Override
        public Attributes getAttributes() {
            return Attributes.EMPTY;
        }

        @Override
        public String getAuthority() {
            return null;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setMessageCompression(boolean enabled) {
        }

        @Override
        public void setCompression(String compressor) {
        }
    }

    private static final class NoopMarshaller<T> implements MethodDescriptor.Marshaller<T> {
        @Override
        public InputStream stream(T value) {
            return InputStream.nullInputStream();
        }

        @Override
        public T parse(InputStream stream) {
            return null;
        }
    }
}
