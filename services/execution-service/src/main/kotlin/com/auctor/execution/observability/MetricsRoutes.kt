package com.auctor.execution.observability

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Route.installMetricsRoute(meterRegistry: MeterRegistry) {
    get("/metrics") {
        if (meterRegistry is PrometheusMeterRegistry) {
            call.respondText(
                meterRegistry.scrape(),
                ContentType.parse("text/plain; version=0.0.4; charset=utf-8")
            )
            return@get
        }

        // Collect all metrics in Prometheus text format
        val sb = StringBuilder()

        // Helper to output Prometheus metric line
        fun escapePrometheusLabelValue(value: String): String =
            value
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\"", "\\\"")

        fun addMetric(name: String, value: Double, tags: List<Tag> = emptyList()) {
            val tagString = if (tags.isNotEmpty()) {
                tags.joinToString(",", "{", "}") { tag ->
                    "${tag.key}=\"${escapePrometheusLabelValue(tag.value)}\""
                }
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
