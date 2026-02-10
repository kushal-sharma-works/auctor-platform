package com.auctor.execution.observability

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Route.installMetricsRoute(meterRegistry: PrometheusMeterRegistry) {
    get("/metrics") {
        call.respondText(
            meterRegistry.scrape(),
            ContentType.parse("text/plain; version=0.0.4; charset=utf-8")
        )
    }
}
