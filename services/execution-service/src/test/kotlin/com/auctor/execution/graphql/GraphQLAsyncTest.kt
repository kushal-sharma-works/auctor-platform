package com.auctor.execution.graphql

import com.auctor.execution.cache.CacheService
import com.auctor.execution.grpc.DefinitionDto
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GraphQLAsyncTest {

    @Test
    fun `async graphql resolver returns definition`() {
        val cache = mock<CacheService>()
        runBlocking {
            whenever(cache.getOrLoad("123", null))
                .thenReturn(DefinitionDto("123", "test", "desc"))
        }

        val provider = GraphQLProvider(cache)

        val result = provider.execute(
            query = """{ getDefinition(id: "123") { id name description } }""",
            variables = null,
            context = null
        )

        val data = result["data"] as? Map<*, *>
        assertNotNull(data, "Data should not be null")
        @Suppress("UNCHECKED_CAST")
        val def = (data as Map<String, *>)["getDefinition"] as? Map<*, *>
        assertNotNull(def, "getDefinition should not be null")

        @Suppress("UNCHECKED_CAST")
        val definition = def as Map<String, *>
        assertEquals("123", definition["id"])
        assertEquals("test", definition["name"])
    }
}
