package com.auctor.execution.observability

import io.ktor.server.application.*
import io.ktor.server.response.*
import org.slf4j.MDC
import java.util.UUID

val CorrelationIdPlugin = createApplicationPlugin("CorrelationIdPlugin") {
    onCall { call ->
        val incoming = call.request.headers["X-Correlation-ID"]
        val correlationId = if (!incoming.isNullOrBlank()) incoming else UUID.randomUUID().toString()
        val traceId = io.opentelemetry.api.trace.Span.current().spanContext.traceId
        val traceValid = io.opentelemetry.api.trace.Span.current().spanContext.isValid

        MDC.put("correlation_id", correlationId)
        MDC.put("trace_id", if (traceValid) traceId else "")
        call.response.headers.append("X-Correlation-ID", correlationId)
    }

    onCallRespond { _ ->
        MDC.remove("correlation_id")
        MDC.remove("trace_id")
    }
}
