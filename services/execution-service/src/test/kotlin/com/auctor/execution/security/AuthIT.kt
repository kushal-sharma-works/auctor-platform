package com.auctor.execution

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auctor.definition.grpc.v1.DefinitionServiceGrpcKt
import com.auctor.definition.grpc.v1.GetDefinitionRequest
import com.auctor.definition.grpc.v1.GetDefinitionResponse
import com.auctor.execution.cache.CacheService
import com.auctor.execution.grpc.DefinitionGrpcClient
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.util.UUID
import kotlin.test.*

class AuthIntegrationTest {

    companion object {
        private lateinit var redis: GenericContainer<*>
        private lateinit var redisUrl: String

        @JvmStatic
        @BeforeAll
        fun startRedis() {
            redis = GenericContainer(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
            redis.start()
            redisUrl = "redis://${redis.host}:${redis.getMappedPort(6379)}"
        }

        @JvmStatic
        @AfterAll
        fun stopRedis() {
            redis.stop()
        }
    }

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

        val grpcClient = DefinitionGrpcClient(channel)
        val cacheService = CacheService(grpcClient, redisUrl)

        try {
            application { 
                module(grpcClient = grpcClient, cacheService = cacheService) 
            }

            val response = client.get("/execute/123") {
                header(HttpHeaders.Authorization, "Bearer ${token()}")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("sample-definition"), "Response should contain definition name")
        } finally {
            cacheService.close()
            grpcClient.close()
            server.shutdownNow()
        }
    }
}
