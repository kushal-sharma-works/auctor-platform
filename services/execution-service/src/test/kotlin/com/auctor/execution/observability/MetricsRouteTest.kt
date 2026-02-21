package com.auctor.execution.observability

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
// import io.micrometer.prometheus.PrometheusConfig
// import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetricsRouteTest {

    @Test
    fun `metrics endpoint exposes prometheus format`() = testApplication {
        val registry = SimpleMeterRegistry()
        val metrics = ExecutionMetrics(registry)
        metrics.recordExecutionStarted()

        application {
            routing { installMetricsRoute(registry) }
        }

        val response = client.get("/metrics")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body.contains("# TYPE execution_started_total counter"))
        assertTrue(body.contains("execution_started_total"))
    }
}
