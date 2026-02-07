package com.auctor.execution

import com.auctor.execution.cache.CacheService
import com.auctor.execution.grpc.DefinitionGrpcClient
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.mockito.kotlin.mock
import kotlin.test.*

class AuthTest {

    @Test
    fun `execute endpoint rejects unauthenticated access`() = testApplication {
        // Mock both to avoid real connections in unit test
        val mockClient = mock<DefinitionGrpcClient>()
        val mockCache = mock<CacheService>()
        
        application { 
            module(grpcClient = mockClient, cacheService = mockCache) 
        }

        val response = client.get("/execute/123")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
