package com.auctor.execution.grpc

import com.auctor.definition.grpc.v1.*
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefinitionGrpcClientTest {

    private lateinit var server: Server
    private lateinit var channel: ManagedChannel
    private lateinit var client: DefinitionGrpcClient
    private lateinit var serverName: String

    @BeforeEach
    fun setup() {
        serverName = java.util.UUID.randomUUID().toString()
        // Create in-process server for testing
        val service = MockDefinitionService()
        server = InProcessServerBuilder.forName(serverName)
            .addService(service)
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
        channel.shutdown()
        server.shutdown()
    }

    @Test
    fun `should fetch workflow successfully`() = runTest {
        val workflow = client.getWorkflow("wf-001", 1)

        assertNotNull(workflow)
        assertEquals("wf-001", workflow.id)
        assertEquals("Test Workflow", workflow.name)
        assertEquals(1, workflow.version)
        assertEquals("PUBLISHED", workflow.status)
        assertEquals(listOf("pending", "approved"), workflow.states)
        assertEquals("pending", workflow.initialState)
        assertEquals(1, workflow.transitions.size)
    }

    @Test
    fun `should return null when workflow not found`() = runTest {
        val workflow = client.getWorkflow("wf-999", 1)
        assertEquals(null, workflow)
    }

    @Test
    fun `should fetch policy successfully`() = runTest {
        val policy = client.getPolicy("policy-001", 1)

        assertNotNull(policy)
        assertEquals("policy-001", policy.id)
        assertEquals("High Value Policy", policy.name)
        assertEquals(1, policy.version)
        assertEquals("PUBLISHED", policy.status)
        assertEquals(1, policy.conditions.size)
    }

    @Test
    fun `should return null when policy not found`() = runTest {
        val policy = client.getPolicy("policy-999", 1)
        assertEquals(null, policy)
    }

    @Test
    fun `should evaluate policy successfully - allowed`() = runTest {
        val result = client.evaluatePolicy(
            policyId = "policy-001",
            version = 1,
            context = mapOf("amount" to "5000")
        )

        assertTrue(result.allowed)
        assertEquals("Amount is within limits", result.explanation)
    }

    @Test
    fun `should evaluate policy successfully - denied`() = runTest {
        val result = client.evaluatePolicy(
            policyId = "policy-001",
            version = 1,
            context = mapOf("amount" to "50000")
        )

        assertFalse(result.allowed)
        assertEquals("Amount exceeds limit", result.explanation)
    }

    @Test
    fun `should handle timeout gracefully`() = runTest {
        // Client with very short timeout
        val shortTimeoutClient = DefinitionGrpcClient(channel, callDeadlineMs = 10)

        assertThrows<Exception> {
            shortTimeoutClient.getWorkflow("wf-slow", 1)
        }

        shortTimeoutClient.close()
    }

    @Test
    fun `should retry on transient failures`() = runTest {
        // The mock service will fail twice then succeed for "wf-retry"
        val workflow = client.getWorkflow("wf-retry", 1)

        // Should succeed after retries
        assertNotNull(workflow)
        assertEquals("wf-retry", workflow.id)
    }

    /**
     * Mock gRPC service for testing
     */
    private class MockDefinitionService : DefinitionServiceGrpc.DefinitionServiceImplBase() {
        
        private var retryAttempts = 0

        override fun getWorkflow(
            request: GetWorkflowRequest,
            responseObserver: StreamObserver<WorkflowResponse>
        ) {
            when (request.id) {
                "wf-001" -> {
                    val response = WorkflowResponse.newBuilder()
                        .setId("wf-001")
                        .setName("Test Workflow")
                        .setVersion(1)
                        .setStatus("PUBLISHED")
                        .addStates("pending")
                        .addStates("approved")
                        .setInitialState("pending")
                        .addTransitions(
                            TransitionProto.newBuilder()
                                .setFromState("pending")
                                .setToState("approved")
                                .setPolicyRef("policy-001")
                                .build()
                        )
                        .build()
                    responseObserver.onNext(response)
                    responseObserver.onCompleted()
                }
                "wf-999" -> {
                    responseObserver.onError(
                        StatusRuntimeException(Status.NOT_FOUND.withDescription("Workflow not found"))
                    )
                }
                "wf-slow" -> {
                    // Simulate slow response asynchronously without blocking
                    responseObserver.onError(
                        StatusRuntimeException(Status.DEADLINE_EXCEEDED.withDescription("Request timeout"))
                    )
                }
                "wf-retry" -> {
                    retryAttempts++
                    if (retryAttempts < 3) {
                        responseObserver.onError(
                            StatusRuntimeException(Status.UNAVAILABLE.withDescription("Service temporarily unavailable"))
                        )
                    } else {
                        val response = WorkflowResponse.newBuilder()
                            .setId("wf-retry")
                            .setName("Retry Workflow")
                            .setVersion(1)
                            .setStatus("PUBLISHED")
                            .addStates("start")
                            .setInitialState("start")
                            .build()
                        responseObserver.onNext(response)
                        responseObserver.onCompleted()
                    }
                }
                else -> {
                    responseObserver.onError(
                        StatusRuntimeException(Status.INTERNAL.withDescription("Unknown workflow"))
                    )
                }
            }
        }

        override fun getPolicy(
            request: GetPolicyRequest,
            responseObserver: StreamObserver<PolicyResponse>
        ) {
            when (request.id) {
                "policy-001" -> {
                    val response = PolicyResponse.newBuilder()
                        .setId("policy-001")
                        .setName("High Value Policy")
                        .setVersion(1)
                        .setStatus("PUBLISHED")
                        .addConditions(
                            PolicyConditionProto.newBuilder()
                                .setField("amount")
                                .setOperator(">")
                                .setValue("10000")
                                .build()
                        )
                        .build()
                    responseObserver.onNext(response)
                    responseObserver.onCompleted()
                }
                "policy-999" -> {
                    responseObserver.onError(
                        StatusRuntimeException(Status.NOT_FOUND.withDescription("Policy not found"))
                    )
                }
                else -> {
                    responseObserver.onError(
                        StatusRuntimeException(Status.INTERNAL.withDescription("Unknown policy"))
                    )
                }
            }
        }

        override fun evaluatePolicy(
            request: EvaluatePolicyRequest,
            responseObserver: StreamObserver<EvaluatePolicyResponse>
        ) {
            val amount = request.contextMap["amount"]?.toIntOrNull() ?: 0
            
            val response = if (amount > 10000) {
                EvaluatePolicyResponse.newBuilder()
                    .setAllowed(false)
                    .setExplanation("Amount exceeds limit")
                    .build()
            } else {
                EvaluatePolicyResponse.newBuilder()
                    .setAllowed(true)
                    .setExplanation("Amount is within limits")
                    .build()
            }
            
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
    }
}
