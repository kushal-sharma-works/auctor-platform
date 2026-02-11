package com.auctor.execution.observability

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CorrelationIdPluginTest {

    @Test
    fun `correlation id is propagated when provided`() = testApplication {
        application {
            install(HttpTracingPlugin)
            install(CorrelationIdPlugin)
            routing {
                get("/health") { call.respondText("ok") }
            }
        }

        val response = client.get("/health") {
            headers.append("X-Correlation-ID", "corr-123")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("corr-123", response.headers["X-Correlation-ID"])
    }

    @Test
    fun `correlation id is generated when missing`() = testApplication {
        application {
            install(HttpTracingPlugin)
            install(CorrelationIdPlugin)
            routing {
                get("/health") { call.respondText("ok") }
            }
        }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(response.headers["X-Correlation-ID"])
    }
}
