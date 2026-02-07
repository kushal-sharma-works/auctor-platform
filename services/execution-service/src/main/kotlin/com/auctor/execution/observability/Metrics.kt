package com.auctor.execution.observability

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry

class ExecutionMetrics(registry: MeterRegistry) {
    val executeCounter: Counter =
        Counter.builder("execution.request.count")
            .description("Execution requests")
            .register(registry)
}
