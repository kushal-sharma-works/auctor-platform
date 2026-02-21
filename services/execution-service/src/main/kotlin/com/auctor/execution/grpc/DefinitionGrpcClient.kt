package com.auctor.execution.grpc

import com.auctor.definition.grpc.v1.*
import com.auctor.execution.observability.ExecutionMetrics
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import io.grpc.ClientInterceptors
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Enhanced gRPC client for definition-service with retry logic and circuit breaker.
 * 
 * Features:
 * - Exponential backoff retry (3 attempts: 100ms → 200ms → 400ms)
 * - Circuit breaker (fails fast after 5 consecutive failures for 30s)
 * - Proper timeout handling with withTimeout and gRPC deadlines
 * - Auth header propagation
 * - Structured concurrency with Dispatchers.IO
 */
class DefinitionGrpcClient : AutoCloseable {

    private val logger = LoggerFactory.getLogger(DefinitionGrpcClient::class.java)
    
    private val channel: ManagedChannel
    private val callDeadlineMs: Long
    private val blockingStub: DefinitionServiceGrpc.DefinitionServiceBlockingStub
    private val ownsChannel: Boolean
    private val metrics: ExecutionMetrics
    
    // Circuit breaker state
    private val consecutiveFailures = AtomicInteger(0)
    private val circuitOpenUntil = AtomicLong(0)
    private val maxConsecutiveFailures = 5
    private val circuitOpenDurationMs = 30_000L

    // Primary constructor for production use
    constructor(
        targetHost: String = "localhost",
        targetPort: Int = 9090,
        callDeadlineMs: Long = 2000L,
        metrics: ExecutionMetrics = ExecutionMetrics.noop()
    ) {
        this.callDeadlineMs = callDeadlineMs
        this.metrics = metrics
        this.channel = ManagedChannelBuilder.forAddress(targetHost, targetPort)
            .usePlaintext()
            .build()
        val interceptor = GrpcTelemetry.create(GlobalOpenTelemetry.get()).newClientInterceptor()
        val interceptedChannel = ClientInterceptors.intercept(channel, interceptor)
        this.blockingStub = DefinitionServiceGrpc.newBlockingStub(interceptedChannel)
        this.ownsChannel = true
    }

    // Secondary constructor for testing with in-process channels
    constructor(
        channel: ManagedChannel,
        callDeadlineMs: Long = 2000L,
        metrics: ExecutionMetrics = ExecutionMetrics.noop()
    ) {
        this.channel = channel
        this.callDeadlineMs = callDeadlineMs
        this.metrics = metrics
        val interceptor = GrpcTelemetry.create(GlobalOpenTelemetry.get()).newClientInterceptor()
        val interceptedChannel = ClientInterceptors.intercept(channel, interceptor)
        this.blockingStub = DefinitionServiceGrpc.newBlockingStub(interceptedChannel)
        this.ownsChannel = false
    }

    /**
     * Fetch workflow by id and version with retry logic.
     */
    suspend fun getWorkflow(id: String, version: Int, authHeader: String? = null): WorkflowDto? {
        return try {
            retryWithBackoff("getWorkflow") {
                recordGrpcDuration("DefinitionService/GetWorkflow") {
                    withContext(Dispatchers.IO) {
                        val stub = attachAuthHeader(authHeader)
                        withTimeout(callDeadlineMs + 200) {
                            val request = GetWorkflowRequest.newBuilder()
                                .setId(id)
                                .setVersion(version)
                                .build()
                            val response = stub.withDeadlineAfter(callDeadlineMs, TimeUnit.MILLISECONDS)
                                .getWorkflow(request)
                            WorkflowDto(
                                id = response.id,
                                name = response.name,
                                version = response.version,
                                status = response.status,
                                states = response.statesList,
                                initialState = response.initialState,
                                transitions = response.transitionsList.map {
                                    TransitionDto(
                                        fromState = it.fromState,
                                        toState = it.toState,
                                        policyRef = it.policyRef.ifBlank { null }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        } catch (e: StatusRuntimeException) {
            if (e.status.code == Status.Code.NOT_FOUND || e.status.code == Status.Code.INVALID_ARGUMENT) {
                null
            } else {
                throw e
            }
        }
    }

    /**
     * Fetch policy by id and version with retry logic.
     */
    suspend fun getPolicy(id: String, version: Int, authHeader: String? = null): PolicyDto? {
        return try {
            retryWithBackoff("getPolicy") {
                recordGrpcDuration("DefinitionService/GetPolicy") {
                    withContext(Dispatchers.IO) {
                        val stub = attachAuthHeader(authHeader)
                        withTimeout(callDeadlineMs + 200) {
                            val request = GetPolicyRequest.newBuilder()
                                .setId(id)
                                .setVersion(version)
                                .build()
                            val response = stub.withDeadlineAfter(callDeadlineMs, TimeUnit.MILLISECONDS)
                                .getPolicy(request)
                            PolicyDto(
                                id = response.id,
                                name = response.name,
                                version = response.version,
                                status = response.status,
                                conditions = response.conditionsList.map {
                                    PolicyConditionDto(
                                        field = it.field,
                                        operator = it.operator,
                                        value = it.value
                                    )
                                }
                            )
                        }
                    }
                }
            }
        } catch (e: StatusRuntimeException) {
            if (e.status.code == Status.Code.NOT_FOUND || e.status.code == Status.Code.INVALID_ARGUMENT) {
                null
            } else {
                throw e
            }
        }
    }

    /**
     * Evaluate policy with given context. Returns result with explanation.
     */
    suspend fun evaluatePolicy(
        policyId: String,
        version: Int,
        context: Map<String, String>,
        authHeader: String? = null
    ): PolicyEvaluationResultDto {
        return retryWithBackoff("evaluatePolicy") {
            recordGrpcDuration("DefinitionService/EvaluatePolicy") {
                withContext(Dispatchers.IO) {
                    val stub = attachAuthHeader(authHeader)
                    withTimeout(callDeadlineMs + 200) {
                        val request = EvaluatePolicyRequest.newBuilder()
                            .setPolicyId(policyId)
                            .setPolicyVersion(version)
                            .putAllContext(context)
                            .build()
                        val response = stub.withDeadlineAfter(callDeadlineMs, TimeUnit.MILLISECONDS)
                            .evaluatePolicy(request)
                        PolicyEvaluationResultDto(
                            allowed = response.allowed,
                            explanation = response.explanation
                        )
                    }
                }
            }
        }
    }

    fun isChannelReady(): Boolean {
        return channel.getState(true) == ConnectivityState.READY
    }

    /**
     * Retry logic with exponential backoff: 100ms, 200ms, 400ms
     */
    private suspend fun <T> retryWithBackoff(
        operation: String,
        maxAttempts: Int = 3,
        block: suspend () -> T
    ): T {
        checkCircuitBreaker()
        
        var lastException: Exception? = null
        for (attempt in 1..maxAttempts) {
            try {
                val result = block()
                recordSuccess()
                return result
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Preserve structured concurrency semantics
                throw e
            } catch (e: StatusRuntimeException) {
                lastException = e
                logger.warn("$operation attempt $attempt/$maxAttempts failed: ${e.status}")
                
                // Don't retry on NOT_FOUND or INVALID_ARGUMENT
                if (e.status.code == Status.Code.NOT_FOUND || 
                    e.status.code == Status.Code.INVALID_ARGUMENT) {
                    throw e
                }
                
                // Exponential backoff: 100ms, 200ms, 400ms
                if (attempt < maxAttempts) {
                    val delayMs = 100L * (1 shl (attempt - 1))
                    delay(delayMs)
                }
            } catch (e: Exception) {
                lastException = e
                logger.error("$operation attempt $attempt/$maxAttempts failed with exception", e)
                if (attempt < maxAttempts) {
                    val delayMs = 100L * (1 shl (attempt - 1))
                    delay(delayMs)
                }
            }
        }
        
        recordFailure()
        logger.error("$operation failed after $maxAttempts attempts", lastException)
        throw lastException ?: IllegalStateException("$operation failed")
    }

    private suspend fun <T> recordGrpcDuration(
        method: String,
        block: suspend () -> T
    ): T {
        val start = System.nanoTime()
        return try {
            val result = block()
            metrics.recordGrpcClientDuration(method, "OK", Duration.ofNanos(System.nanoTime() - start))
            result
        } catch (e: StatusRuntimeException) {
            metrics.recordGrpcClientDuration(method, e.status.code.name, Duration.ofNanos(System.nanoTime() - start))
            throw e
        } catch (e: Exception) {
            metrics.recordGrpcClientDuration(method, "ERROR", Duration.ofNanos(System.nanoTime() - start))
            throw e
        }
    }

    /**
     * Check if circuit breaker is open (fail-fast mode)
     */
    private fun checkCircuitBreaker() {
        val openUntil = circuitOpenUntil.get()
        if (openUntil > 0 && Instant.now().toEpochMilli() < openUntil) {
            throw IllegalStateException("Circuit breaker is OPEN - failing fast")
        }
    }

    /**
     * Record successful call - reset failure counter and close circuit
     */
    private fun recordSuccess() {
        consecutiveFailures.set(0)
        circuitOpenUntil.set(0)
    }

    /**
     * Record failed call - increment counter and potentially open circuit
     */
    private fun recordFailure() {
        val failures = consecutiveFailures.incrementAndGet()
        if (failures >= maxConsecutiveFailures) {
            val openUntil = Instant.now().toEpochMilli() + circuitOpenDurationMs
            circuitOpenUntil.set(openUntil)
            logger.error("Circuit breaker OPENED after $failures consecutive failures")
        }
    }

    /**
     * Attach authorization header to stub if provided
     */
    private fun attachAuthHeader(authHeader: String?): DefinitionServiceGrpc.DefinitionServiceBlockingStub {
        return if (!authHeader.isNullOrBlank()) {
            val metadata = Metadata().apply {
                val authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
                put(authKey, authHeader)
            }
            blockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
        } else {
            blockingStub
        }
    }

    override fun close() {
        if (ownsChannel) {
            channel.shutdown().awaitTermination(1, TimeUnit.SECONDS)
        }
    }
}
