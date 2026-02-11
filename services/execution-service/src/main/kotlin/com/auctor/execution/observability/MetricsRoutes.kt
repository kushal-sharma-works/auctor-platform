package com.auctor.execution.observability

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags

fun Route.installMetricsRoute(meterRegistry: MeterRegistry) {
    get("/metrics") {
        // Collect all metrics in Prometheus text format
        val sb = StringBuilder()
        
        // Helper to output Prometheus metric line
        fun addMetric(name: String, value: Double, tags: List<*> = emptyList<Any>()) {
            val tagString = if(tags.isNotEmpty()) {
                tags.joinToString(",", "{", "}") { "${it.toString().split("=")[0]}=\"${it.toString().split("=").getOrNull(1) ?: ""}" }
            } else {
                ""
            }
            sb.append("$name$tagString $value\n")
        }

        // Export counters
        sb.append("# TYPE execution_started_total counter\n")
        meterRegistry.find("execution.started.total").counters().forEach { counter ->
            addMetric("execution_started_total", counter.count(), counter.id.tags)
        }
        
        sb.append("# TYPE execution_completed_total counter\n")
        meterRegistry.find("execution.completed.total").counters().forEach { counter ->
            addMetric("execution_completed_total", counter.count(), counter.id.tags)
        }
        
        sb.append("# TYPE execution_failed_total counter\n")
        meterRegistry.find("execution.failed.total").counters().forEach { counter ->
            addMetric("execution_failed_total", counter.count(), counter.id.tags)
        }
        
        sb.append("# TYPE cache_hit_total counter\n")
        meterRegistry.find("cache.hit.total").counters().forEach { counter ->
            addMetric("cache_hit_total", counter.count(), counter.id.tags)
        }
        
        sb.append("# TYPE cache_miss_total counter\n")
        meterRegistry.find("cache.miss.total").counters().forEach { counter ->
            addMetric("cache_miss_total", counter.count(), counter.id.tags)
        }

        // Export JVM metrics
        sb.append("# TYPE jvm_memory_used_bytes gauge\n")
        meterRegistry.find("jvm.memory.used").gauges().forEach { gauge ->
            addMetric("jvm_memory_used_bytes", gauge.value(), gauge.id.tags)
        }
        
        sb.append("# TYPE jvm_memory_max_bytes gauge\n")
        meterRegistry.find("jvm.memory.max").gauges().forEach { gauge ->
            addMetric("jvm_memory_max_bytes", gauge.value(), gauge.id.tags)
        }

        call.respondText(
            sb.toString(),
            ContentType.parse("text/plain; version=0.0.4; charset=utf-8")
        )
    }
}
