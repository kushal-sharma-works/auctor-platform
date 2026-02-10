package com.auctor.execution.observability

import io.ktor.server.application.*
import io.ktor.util.AttributeKey
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.context.propagation.TextMapGetter

private val spanKey = AttributeKey<Span>("otel-span")
private val scopeKey = AttributeKey<Scope>("otel-scope")

val HttpTracingPlugin = createApplicationPlugin("HttpTracingPlugin") {
    val openTelemetry = GlobalOpenTelemetry.get()
    val tracer = openTelemetry.getTracer("com.auctor.execution.http")
    val propagator = openTelemetry.propagators.textMapPropagator

    onCall { call ->
        val parentContext = propagator.extract(Context.current(), call, HeaderGetter)
        val span = tracer.spanBuilder("${call.request.httpMethod.value} ${call.request.path()}")
            .setParent(parentContext)
            .setSpanKind(SpanKind.SERVER)
            .startSpan()
        val scope = span.makeCurrent()
        call.attributes.put(spanKey, span)
        call.attributes.put(scopeKey, scope)
    }

    onCallRespond { call ->
        val span = call.attributes.getOrNull(spanKey)
        val scope = call.attributes.getOrNull(scopeKey)
        val statusCode = call.response.status()?.value
        if (span != null) {
            if (statusCode != null) {
                span.setAttribute("http.status_code", statusCode.toLong())
                if (statusCode >= 500) {
                    span.setStatus(StatusCode.ERROR)
                }
            }
            span.end()
        }
        scope?.close()
    }
}

private object HeaderGetter : TextMapGetter<ApplicationCall> {
    override fun keys(carrier: ApplicationCall): Iterable<String> = carrier.request.headers.names()

    override fun get(carrier: ApplicationCall?, key: String): String? = carrier?.request?.headers?.get(key)
}
