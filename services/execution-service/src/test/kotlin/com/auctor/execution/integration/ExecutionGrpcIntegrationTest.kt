package com.auctor.execution.integration

import com.auctor.definition.grpc.v1.DefinitionServiceGrpcKt
import com.auctor.definition.grpc.v1.GetDefinitionRequest
import com.auctor.definition.grpc.v1.GetDefinitionResponse
import com.auctor.execution.grpc.DefinitionGrpcClient
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auctor.execution.http.executionRoutes
import com.auctor.execution.security.configureAuth
import com.auctor.execution.service.ExecutionService
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertTrue

class ExecutionGrpcIntegrationTest {

    private fun token(): String =
        JWT.create()
            .withIssuer("auctor-auth")
            .withAudience("execution-service")
            .withSubject("test-user")
            .withClaim("roles", listOf("EXECUTOR"))
            .sign(Algorithm.HMAC256("dev-secret-change-later"))

    @Test
    fun `http call triggers real grpc`() {
        val serverName = "definition-test-${UUID.randomUUID()}"
        val definitionService = object : DefinitionServiceGrpcKt.DefinitionServiceCoroutineImplBase() {
            override suspend fun getDefinition(request: GetDefinitionRequest): GetDefinitionResponse {
                return GetDefinitionResponse.newBuilder()
                    .setId(request.id)
                    .setName("sample-definition")
                    .setDescription("sample-description")
                    .build()
            }
        }

        val server = InProcessServerBuilder
            .forName(serverName)
            .directExecutor()
            .addService(definitionService)
            .build()
            .start()

        val channel = InProcessChannelBuilder
            .forName(serverName)
            .directExecutor()
            .build()

        val definitionClient = DefinitionGrpcClient(channel)

        try {
            testApplication {
                application {
                    configureAuth()
                    val executionService = ExecutionService(definitionClient)
                    executionRoutes()
                }

                val response = client.get("/execute/123") {
                    header(HttpHeaders.Authorization, "Bearer ${token()}")
                }
                assertTrue(response.bodyAsText().contains("sample-definition"))
            }
        } finally {
            definitionClient.close()
            server.shutdownNow()
        }
    }
}
