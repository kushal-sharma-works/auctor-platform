import { WebTracerProvider } from '@opentelemetry/sdk-trace-web'
import { BatchSpanProcessor } from '@opentelemetry/sdk-trace-base'
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http'

export function initTracing() {
  if (typeof window === 'undefined') return

  const endpoint = process.env.NEXT_PUBLIC_OTEL_EXPORTER_ENDPOINT
  if (!endpoint) return

  const provider = new WebTracerProvider({
    spanProcessors: [
      new BatchSpanProcessor(
        new OTLPTraceExporter({
          url: endpoint
        })
      )
    ]
  })

  provider.register()
}
