package com.auctor.execution.grpc

import com.auctor.definition.grpc.v1.DefinitionServiceGrpc
import com.auctor.definition.grpc.v1.GetWorkflowRequest
import com.auctor.definition.grpc.v1.WorkflowResponse
import io.grpc.*
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TokenPropagationIT {

    private lateinit var server: Server
    private lateinit var channel: ManagedChannel
    private lateinit var client: DefinitionGrpcClient
    private val capturedAuthHeader = AtomicReference<String?>()

    @BeforeEach
    fun setup() {
        val serverName = UUID.randomUUID().toString()
        val interceptor = object : ServerInterceptor {
            override fun <ReqT : Any?, RespT : Any?> interceptCall(
                call: ServerCall<ReqT, RespT>,
                headers: Metadata,
                next: ServerCallHandler<ReqT, RespT>
            ): ServerCall.Listener<ReqT> {
                val key = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
                capturedAuthHeader.set(headers.get(key))
                return next.startCall(call, headers)
            }
        }

        server = InProcessServerBuilder.forName(serverName)
            .addService(ServerInterceptors.intercept(TestDefinitionService(), interceptor))
            .directExecutor()
            .build()
            .start()

        channel = InProcessChannelBuilder.forName(serverName)
            .directExecutor()
            .build()

        client = DefinitionGrpcClient(channel, callDeadlineMs = 1000)
    }

    @AfterEach
    fun teardown() {
        client.close()
        channel.shutdownNow()
        server.shutdownNow()
    }

    @Test
    fun `forwards authorization header to definition-service`() = runTest {
        val header = "Bearer test.jwt.token"

        val workflow = client.getWorkflow("wf-1", 1, header)

        assertNotNull(workflow)
        assertEquals(header, capturedAuthHeader.get())
    }

    private class TestDefinitionService : DefinitionServiceGrpc.DefinitionServiceImplBase() {
        override fun getWorkflow(
            request: GetWorkflowRequest,
            responseObserver: StreamObserver<WorkflowResponse>
        ) {
            responseObserver.onNext(
                WorkflowResponse.newBuilder()
                    .setId(request.id)
                    .setName("Test")
                    .setVersion(1)
                    .setStatus("DRAFT")
                    .setInitialState("DRAFT")
                    .build()
            )
            responseObserver.onCompleted()
        }
    }
}
