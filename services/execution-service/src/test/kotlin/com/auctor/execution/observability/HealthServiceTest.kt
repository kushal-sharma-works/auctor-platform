package com.auctor.execution.observability

import com.auctor.definition.grpc.v1.DefinitionServiceGrpc
import com.auctor.execution.grpc.DefinitionGrpcClient
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.sql.Connection
import javax.sql.DataSource
import kotlin.test.assertEquals

class HealthServiceTest {

    @Test
    fun `readiness reports UP when db and grpc are up`() {
        val dataSource = mockDataSource(dbUp = true)
        val (grpcClient, cleanup) = readyGrpcClient()

        try {
            val service = HealthService(dataSource, grpcClient)
            val readiness = service.readiness()
            val checks = readiness["checks"] as Map<*, *>

            assertEquals("UP", readiness["status"])
            assertEquals("UP", (checks["db"] as Map<*, *>)["status"])
            assertEquals("UP", (checks["grpc"] as Map<*, *>)["status"])
        } finally {
            cleanup()
        }
    }

    @Test
    fun `readiness reports DOWN when db is down and grpc is up`() {
        val dataSource = mockDataSource(dbUp = false)
        val (grpcClient, cleanup) = readyGrpcClient()

        try {
            val service = HealthService(dataSource, grpcClient)
            val readiness = service.readiness()
            val checks = readiness["checks"] as Map<*, *>

            assertEquals("DOWN", readiness["status"])
            assertEquals("DOWN", (checks["db"] as Map<*, *>)["status"])
            assertEquals("UP", (checks["grpc"] as Map<*, *>)["status"])
        } finally {
            cleanup()
        }
    }

    @Test
    fun `readiness reports DOWN when db is up and grpc is down`() {
        val dataSource = mockDataSource(dbUp = true)
        val (grpcClient, cleanup) = unreadyGrpcClient()

        try {
            val service = HealthService(dataSource, grpcClient)
            val readiness = service.readiness()
            val checks = readiness["checks"] as Map<*, *>

            assertEquals("DOWN", readiness["status"])
            assertEquals("UP", (checks["db"] as Map<*, *>)["status"])
            assertEquals("DOWN", (checks["grpc"] as Map<*, *>)["status"])
        } finally {
            cleanup()
        }
    }

    @Test
    fun `readiness reports DOWN when db and grpc are down`() {
        val dataSource = mockDataSource(dbUp = false)
        val (grpcClient, cleanup) = unreadyGrpcClient()

        try {
            val service = HealthService(dataSource, grpcClient)
            val readiness = service.readiness()
            val checks = readiness["checks"] as Map<*, *>

            assertEquals("DOWN", readiness["status"])
            assertEquals("DOWN", (checks["db"] as Map<*, *>)["status"])
            assertEquals("DOWN", (checks["grpc"] as Map<*, *>)["status"])
        } finally {
            cleanup()
        }
    }

    private fun mockDataSource(dbUp: Boolean): DataSource {
        val dataSource = mock<DataSource>()
        val connection = mock<Connection>()
        whenever(dataSource.connection).thenReturn(connection)
        whenever(connection.isValid(2)).thenReturn(dbUp)
        return dataSource
    }

    private fun readyGrpcClient(): Pair<DefinitionGrpcClient, () -> Unit> {
        val serverName = InProcessServerBuilder.generateName()
        val server = InProcessServerBuilder
            .forName(serverName)
            .directExecutor()
            .addService(object : DefinitionServiceGrpc.DefinitionServiceImplBase() {})
            .build()
            .start()

        val channel = InProcessChannelBuilder
            .forName(serverName)
            .directExecutor()
            .build()

        waitForReady(channel)
        val client = DefinitionGrpcClient(channel)
        return client to {
            client.close()
            server.shutdownNow()
        }
    }

    private fun unreadyGrpcClient(): Pair<DefinitionGrpcClient, () -> Unit> {
        val channel = ManagedChannelBuilder
            .forAddress("127.0.0.1", 65534)
            .usePlaintext()
            .build()

        val client = DefinitionGrpcClient(channel)
        return client to { client.close() }
    }

    private fun waitForReady(channel: ManagedChannel) {
        repeat(20) {
            if (channel.getState(true) == ConnectivityState.READY) {
                return
            }
            Thread.sleep(50)
        }
    }
}
