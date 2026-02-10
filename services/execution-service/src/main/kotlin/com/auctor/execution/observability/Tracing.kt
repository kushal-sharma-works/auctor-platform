package com.auctor.execution.observability

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.semconv.ResourceAttributes
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator

fun initTracing(): OpenTelemetry {
    val endpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT")
    val exporter = OtlpGrpcSpanExporter.builder()
        .setEndpoint(endpoint ?: "http://localhost:4317")
        .build()

    val tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
            .setResource(
                Resource.getDefault().merge(
                    Resource.create(
                        io.opentelemetry.api.common.Attributes.of(
                            ResourceAttributes.SERVICE_NAME,
                            "execution-service"
                        )
                    )
                )
            )
            .build()

    val openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .build()

    GlobalOpenTelemetry.set(openTelemetry)
    return openTelemetry
}
