package com.auctor.execution.grpc

import com.auctor.definition.grpc.v1.DefinitionServiceGrpc
import com.auctor.definition.grpc.v1.GetDefinitionRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

/**
 * Minimal coroutine-friendly wrapper around the generated Java gRPC client.
 *
 * - Adds deadline on calls
 * - Propagates `Authorization` metadata if provided
 * - Runs network work on Dispatchers.IO to avoid blocking event loop
 */
class DefinitionGrpcClient : AutoCloseable {

    private val channel: ManagedChannel
    private val callDeadlineMs: Long
    private val blockingStub: DefinitionServiceGrpc.DefinitionServiceBlockingStub
    private val ownsChannel: Boolean

    // Primary constructor for production use
    constructor(
        targetHost: String = "localhost",
        targetPort: Int = 9090,
        callDeadlineMs: Long = 2000L
    ) {
        this.callDeadlineMs = callDeadlineMs
        this.channel = ManagedChannelBuilder.forAddress(targetHost, targetPort)
            .usePlaintext()
            .build()
        this.blockingStub = DefinitionServiceGrpc.newBlockingStub(channel)
        this.ownsChannel = true
    }

    // Secondary constructor for testing with in-process channels
    constructor(
        channel: ManagedChannel,
        callDeadlineMs: Long = 2000L
    ) {
        this.channel = channel
        this.callDeadlineMs = callDeadlineMs
        this.blockingStub = DefinitionServiceGrpc.newBlockingStub(channel)
        this.ownsChannel = false
    }

    /**
     * Fetch definition by id. `authHeader` should be "Bearer <token>" if present.
     * Timeout is enforced at coroutine level (withTimeout) and gRPC deadline is set too.
     */
    suspend fun getDefinition(id: String, authHeader: String? = null): DefinitionDto? {
        return withContext(Dispatchers.IO) {
            try {
                // attach metadata if token present
                val stub = if (!authHeader.isNullOrBlank()) {
                    val metadata = Metadata().apply {
                        val authKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
                        put(authKey, authHeader)
                    }
                    blockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                } else blockingStub

                // use coroutine-level timeout slightly larger than gRPC deadline to be safe
                withTimeout(callDeadlineMs + 200) {
                    @Suppress("DEPRECATION")
                    val request = GetDefinitionRequest.newBuilder().apply {
                        setId(id)
                    }.build()
                    val response = stub.withDeadlineAfter(callDeadlineMs, TimeUnit.MILLISECONDS)
                        .getDefinition(request)
                    DefinitionDto(response.id, response.name, response.description)
                }
            } catch (e: StatusRuntimeException) {
                // map or log, return null if not found or error
                // In production, map Status to domain errors
                null
            }
        }
    }

    override fun close() {
        if (ownsChannel) {
            channel.shutdown().awaitTermination(1, TimeUnit.SECONDS)
        }
    }
}
