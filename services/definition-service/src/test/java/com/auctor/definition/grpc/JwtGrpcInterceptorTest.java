package com.auctor.definition.grpc;

import com.auctor.definition.grpc.v1.DefinitionServiceGrpc;
import com.auctor.definition.grpc.v1.GetWorkflowRequest;
import com.auctor.definition.grpc.v1.WorkflowResponse;
import com.auctor.definition.util.JwtTokenGenerator;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtGrpcInterceptorTest {

    private Server server;
    private ManagedChannel channel;

    @BeforeEach
    void setup() throws Exception {
        String serverName = java.util.UUID.randomUUID().toString();
        JwtGrpcInterceptor interceptor = new JwtGrpcInterceptor(jwtDecoder(), jwtAuthenticationConverter());

        server = InProcessServerBuilder.forName(serverName)
            .addService(ServerInterceptors.intercept(new TestDefinitionService(), interceptor))
            .directExecutor()
            .build()
            .start();

        channel = InProcessChannelBuilder.forName(serverName)
            .directExecutor()
            .build();
    }

    @AfterEach
    void teardown() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void validJwtAllowsGrpcCall() {
        String token = JwtTokenGenerator.generate("viewer", List.of("VIEWER"));
        DefinitionServiceGrpc.DefinitionServiceBlockingStub stub = DefinitionServiceGrpc.newBlockingStub(channel);

        Metadata metadata = new Metadata();
        Metadata.Key<String> authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        metadata.put(authKey, "Bearer " + token);
        DefinitionServiceGrpc.DefinitionServiceBlockingStub authedStub =
            stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

        WorkflowResponse response = authedStub.getWorkflow(
            GetWorkflowRequest.newBuilder().setId("wf-1").build()
        );

        assertEquals("wf-1", response.getId());
    }

    @Test
    void missingJwtIsRejected() {
        DefinitionServiceGrpc.DefinitionServiceBlockingStub stub = DefinitionServiceGrpc.newBlockingStub(channel);

        StatusRuntimeException ex = assertThrows(
            StatusRuntimeException.class,
            () -> stub.getWorkflow(GetWorkflowRequest.newBuilder().setId("wf-1").build())
        );

        assertEquals(Status.Code.UNAUTHENTICATED, ex.getStatus().getCode());
    }

    @Test
    void invalidJwtIsRejected() {
        DefinitionServiceGrpc.DefinitionServiceBlockingStub stub = DefinitionServiceGrpc.newBlockingStub(channel);

        Metadata metadata = new Metadata();
        Metadata.Key<String> authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        metadata.put(authKey, "Bearer invalid.jwt.token");
        DefinitionServiceGrpc.DefinitionServiceBlockingStub authedStub =
            stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

        StatusRuntimeException ex = assertThrows(
            StatusRuntimeException.class,
            () -> authedStub.getWorkflow(GetWorkflowRequest.newBuilder().setId("wf-1").build())
        );

        assertEquals(Status.Code.UNAUTHENTICATED, ex.getStatus().getCode());
    }

    private JwtDecoder jwtDecoder() {
        String secret = System.getenv().getOrDefault("DEFINITION_JWT_SECRET", "dev-secret-change-later-dev-secret-change-later");
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");
        converter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtAuthenticationConverter;
    }

    private static class TestDefinitionService extends DefinitionServiceGrpc.DefinitionServiceImplBase {
        @Override
        public void getWorkflow(GetWorkflowRequest request, StreamObserver<WorkflowResponse> responseObserver) {
            responseObserver.onNext(
                WorkflowResponse.newBuilder()
                    .setId(request.getId())
                    .setName("Test")
                    .setVersion(1)
                    .setStatus("DRAFT")
                    .setInitialState("DRAFT")
                    .build()
            );
            responseObserver.onCompleted();
        }
    }
}
