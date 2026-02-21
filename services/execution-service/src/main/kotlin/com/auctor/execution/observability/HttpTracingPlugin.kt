package com.auctor.execution.observability

import io.ktor.server.application.*
import io.ktor.server.application.hooks.CallFailed
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
        val span = tracer.spanBuilder("${call.request.local.method.value} ${call.request.local.uri}")
            .setParent(parentContext)
            .setSpanKind(SpanKind.SERVER)
            .startSpan()
        val scope = span.makeCurrent()
        call.attributes.put(spanKey, span)
        call.attributes.put(scopeKey, scope)
    }

    fun finishSpan(call: ApplicationCall, statusCode: Int? = null, cause: Throwable? = null) {
        val span = call.attributes.getOrNull(spanKey)
        val scope = call.attributes.getOrNull(scopeKey)

        if (span != null) {
            if (statusCode != null) {
                span.setAttribute("http.status_code", statusCode.toLong())
                if (statusCode >= 500) {
                    span.setStatus(StatusCode.ERROR)
                }
            }
            if (cause != null) {
                span.recordException(cause)
                span.setStatus(StatusCode.ERROR)
            }
            span.end()
            call.attributes.remove(spanKey)
        }

        if (scope != null) {
            scope.close()
            call.attributes.remove(scopeKey)
        }
    }

    onCallRespond { call ->
        val statusCode = call.response.status()?.value
        finishSpan(call, statusCode = statusCode)
    }

    on(CallFailed) { call, cause ->
        finishSpan(call, cause = cause)
    }
}

private object HeaderGetter : TextMapGetter<ApplicationCall> {
    override fun keys(carrier: ApplicationCall): Iterable<String> = carrier.request.headers.names()

    override fun get(carrier: ApplicationCall?, key: String): String? = carrier?.request?.headers?.get(key)
}
