package com.auctor.execution.observability

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals

class ExecutionMetricsTest {

    private val meterRegistry = SimpleMeterRegistry()
    private val metrics = ExecutionMetrics(meterRegistry)

    @Test
    fun `should record execution started`() {
        metrics.recordExecutionStarted()
        metrics.recordExecutionStarted()

        val counter = meterRegistry.find("execution.started.total").counter()
        assertEquals(2.0, counter?.count())
    }

    @Test
    fun `should record execution completed with duration`() {
        metrics.recordExecutionCompleted(Duration.ofMillis(100))
        metrics.recordExecutionCompleted(Duration.ofMillis(200))

        val completedCounter = meterRegistry.find("execution.completed.total").counter()
        assertEquals(2.0, completedCounter?.count())

        val durationTimer = meterRegistry.find("execution.duration").timer()
        assertEquals(2L, durationTimer?.count())
    }

    @Test
    fun `should record execution failed`() {
        metrics.recordExecutionFailed()
        metrics.recordExecutionFailed()
        metrics.recordExecutionFailed()

        val counter = meterRegistry.find("execution.failed.total").counter()
        assertEquals(3.0, counter?.count())
    }

    @Test
    fun `should record state transition`() {
        metrics.recordStateTransition("START", "APPROVED")
        metrics.recordStateTransition("APPROVED", "END")

        val transitions = meterRegistry.find("execution.state_transition.total").counters()
        assertEquals(2, transitions.size)
    }

    @Test
    fun `should record gRPC client duration`() {
        metrics.recordGrpcClientDuration("GetWorkflow", "OK", Duration.ofMillis(50))
        metrics.recordGrpcClientDuration("GetPolicy", "FAILED", Duration.ofMillis(100))

        val timers = meterRegistry.find("grpc.client.request.duration").timers()
        assertEquals(2, timers.size)
    }

    @Test
    fun `should record cache hits and misses`() {
        metrics.recordCacheHit()
        metrics.recordCacheHit()
        metrics.recordCacheMiss()

        val hitCounter = meterRegistry.find("cache.hit.total").counter()
        val missCounter = meterRegistry.find("cache.miss.total").counter()

        assertEquals(2.0, hitCounter?.count())
        assertEquals(1.0, missCounter?.count())
    }

    @Test
    fun `should create noop metrics instance`() {
        val noop = ExecutionMetrics.noop()
        noop.recordExecutionStarted()
        noop.recordCacheHit()
        noop.recordStateTransition("A", "B")
        // Should not throw
    }
}
