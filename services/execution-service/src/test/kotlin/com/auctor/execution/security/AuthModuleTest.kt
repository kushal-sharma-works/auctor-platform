package com.auctor.execution.security

import com.auctor.execution.util.TestTokenGenerator
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.server.config.MapApplicationConfig
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class AuthModuleTest {

    private val testSecret = "dev-secret-change-later-dev-secret-change-later"
    private val testIssuer = "auctor-auth"
    private val testAudience = "execution-service"

    @Test
    fun `auth-jwt accepts valid token`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "ktor.jwt.secret" to testSecret,
                "ktor.jwt.issuer" to testIssuer,
                "ktor.jwt.audience" to testAudience
            )
        }
        application { configureAuth(); routing { authenticate("auth-jwt") { get("/secure") { call.respondText("ok") } } } }

        val token = TestTokenGenerator.generateToken(
            roles = listOf("VIEWER"),
            issuer = testIssuer,
            audience = testAudience,
            secret = testSecret
        )
        val response = client.get("/secure") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `auth-viewer rejects token without viewer role`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "ktor.jwt.secret" to testSecret,
                "ktor.jwt.issuer" to testIssuer,
                "ktor.jwt.audience" to testAudience
            )
        }
        application { configureAuth(); routing { authenticate("auth-viewer") { get("/viewer") { call.respondText("ok") } } } }

        val token = TestTokenGenerator.generateToken(
            roles = listOf("EXECUTOR"),
            issuer = testIssuer,
            audience = testAudience,
            secret = testSecret
        )
        val response = client.get("/viewer") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `auth-executor accepts executor role`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "ktor.jwt.secret" to testSecret,
                "ktor.jwt.issuer" to testIssuer,
                "ktor.jwt.audience" to testAudience
            )
        }
        application { configureAuth(); routing { authenticate("auth-executor") { get("/exec") { call.respondText("ok") } } } }

        val token = TestTokenGenerator.generateToken(
            roles = listOf("EXECUTOR"),
            issuer = testIssuer,
            audience = testAudience,
            secret = testSecret
        )
        val response = client.get("/exec") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `auth-executor rejects viewer role`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "ktor.jwt.secret" to testSecret,
                "ktor.jwt.issuer" to testIssuer,
                "ktor.jwt.audience" to testAudience
            )
        }
        application { configureAuth(); routing { authenticate("auth-executor") { get("/exec") { call.respondText("ok") } } } }

        val token = TestTokenGenerator.generateToken(
            roles = listOf("VIEWER"),
            issuer = testIssuer,
            audience = testAudience,
            secret = testSecret
        )
        val response = client.get("/exec") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `invalid token is unauthorized`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "ktor.jwt.secret" to testSecret,
                "ktor.jwt.issuer" to testIssuer,
                "ktor.jwt.audience" to testAudience
            )
        }
        application { configureAuth(); routing { authenticate("auth-jwt") { get("/secure") { call.respondText("ok") } } } }

        val response = client.get("/secure") {
            header(HttpHeaders.Authorization, "Bearer invalid.token.value")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `expired token is unauthorized`() = testApplication {
        environment {
            config = MapApplicationConfig(
                "ktor.jwt.secret" to testSecret,
                "ktor.jwt.issuer" to testIssuer,
                "ktor.jwt.audience" to testAudience
            )
        }
        application { configureAuth(); routing { authenticate("auth-jwt") { get("/secure") { call.respondText("ok") } } } }

        val expired = TestTokenGenerator.generateToken(
            roles = listOf("VIEWER"),
            expiresAt = Instant.now().minusSeconds(10),
            issuer = testIssuer,
            audience = testAudience,
            secret = testSecret
        )

        val response = client.get("/secure") {
            header(HttpHeaders.Authorization, "Bearer $expired")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
