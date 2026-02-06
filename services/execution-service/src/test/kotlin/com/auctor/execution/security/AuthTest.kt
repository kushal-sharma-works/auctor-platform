package com.auctor.execution

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class AuthTest {

    @Test
    fun `execute endpoint rejects unauthenticated access`() = testApplication {
        application { module() }

        val response = client.get("/execute/123")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
