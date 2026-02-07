package com.auctor.execution.graphql

import com.auctor.execution.cache.CacheService
import com.auctor.execution.grpc.DefinitionDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class GraphQLResolverTest {

    @Test
    fun `graphql provider returns definition from cache service`() {
        // arrange: mock cache service
        val expected = DefinitionDto("123", "sample-definition", "desc")
        val cacheMock = mock<CacheService> {
            onBlocking { getOrLoad("123", "Bearer token") } doReturn expected
        }

        val provider = GraphQLProvider(cacheMock)

        // act
        val query = """{ getDefinition(id: "123") { id name description } }"""
        val result = provider.execute(query, null, mapOf("authorization" to "Bearer token"))

        // assert
        val data = result["data"] as? Map<*, *>
        assertNotNull(data, "Data should not be null")
        @Suppress("UNCHECKED_CAST")
        val def = (data as Map<String, *>)["getDefinition"] as? Map<*, *>
        assertNotNull(def, "getDefinition should not be null")
        @Suppress("UNCHECKED_CAST")
        val definition = def as Map<String, *>
        assertEquals("123", definition["id"])
        assertEquals("sample-definition", definition["name"])
        assertEquals("desc", definition["description"])
    }
}
