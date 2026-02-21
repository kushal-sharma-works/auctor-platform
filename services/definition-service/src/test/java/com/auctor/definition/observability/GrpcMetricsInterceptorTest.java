package com.auctor.definition.observability;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GrpcMetricsInterceptorTest {

    @Test
    void shouldRecordTimerWithMethodAndStatusTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        GrpcMetricsInterceptor interceptor = new GrpcMetricsInterceptor(registry);

        TestServerCall<String, String> baseCall = new TestServerCall<>("definition.Test/Call");
        Metadata headers = new Metadata();
        AtomicReference<ServerCall<String, String>> capturedCall = new AtomicReference<>();

        ServerCallHandler<String, String> next = (call, unusedHeaders) -> {
            capturedCall.set(call);
            return new ServerCall.Listener<>() {};
        };

        interceptor.interceptCall(baseCall, headers, next);

        capturedCall.get().close(Status.OK, new Metadata());

        assertNotNull(registry.find("grpc.server.request.duration")
            .tags("method", "definition.Test/Call", "status", "OK")
            .timer());
        assertEquals(1,
            registry.find("grpc.server.request.duration")
                .tags("method", "definition.Test/Call", "status", "OK")
                .timer()
                .count()
        );
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
