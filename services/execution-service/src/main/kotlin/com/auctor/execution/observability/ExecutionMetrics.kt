package com.auctor.execution.observability

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.time.Duration

class ExecutionMetrics(private val meterRegistry: MeterRegistry) {
    private val executionStarted = meterRegistry.counter("execution.started.total")
    private val executionCompleted = meterRegistry.counter("execution.completed.total")
    private val executionFailed = meterRegistry.counter("execution.failed.total")
    private val cacheHit = meterRegistry.counter("cache.hit.total")
    private val cacheMiss = meterRegistry.counter("cache.miss.total")
    private val executionDuration = meterRegistry.timer("execution.duration")

    fun recordExecutionStarted() {
        executionStarted.increment()
    }

    fun recordExecutionCompleted(duration: Duration) {
        executionCompleted.increment()
        executionDuration.record(duration)
    }

    fun recordExecutionFailed() {
        executionFailed.increment()
    }

    fun recordStateTransition(fromState: String, toState: String) {
        Counter.builder("execution.state_transition.total")
            .tag("from_state", fromState)
            .tag("to_state", toState)
            .register(meterRegistry)
            .increment()
    }

    fun recordGrpcClientDuration(method: String, status: String, duration: Duration) {
        Timer.builder("grpc.client.request.duration")
            .tag("method", method)
            .tag("status", status)
            .register(meterRegistry)
            .record(duration)
    }

    fun recordCacheHit() {
        cacheHit.increment()
    }

    fun recordCacheMiss() {
        cacheMiss.increment()
    }

    companion object {
        fun noop(): ExecutionMetrics {
            return ExecutionMetrics(io.micrometer.core.instrument.simple.SimpleMeterRegistry())
        }
    }
}
