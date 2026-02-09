package com.auctor.execution.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.semconv.ResourceAttributes

fun initTracing(): OpenTelemetry {
    val exporter = OtlpGrpcSpanExporter.builder().build()

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

    return OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .build()
}
