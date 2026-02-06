package com.auctor.execution

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auctor.definition.grpc.v1.DefinitionServiceGrpcKt
import com.auctor.definition.grpc.v1.GetDefinitionRequest
import com.auctor.definition.grpc.v1.GetDefinitionResponse
import com.auctor.execution.grpc.DefinitionGrpcClient
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*
import java.util.UUID

class AuthIntegrationTest {

    private fun token(): String =
        JWT.create()
            .withIssuer("auctor-auth")
            .withAudience("execution-service")
            .withSubject("test-user")
            .withClaim("roles", listOf("EXECUTOR"))
            .sign(Algorithm.HMAC256("dev-secret-change-later"))

    @Test
    fun `execute endpoint allows valid token`() = testApplication {
        val serverName = "auth-test-${UUID.randomUUID()}"
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
            application { module(definitionClient) }

            val response = client.get("/execute/123") {
                header(HttpHeaders.Authorization, "Bearer ${token()}")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        } finally {
            definitionClient.close()
            server.shutdownNow()
        }
    }
}
