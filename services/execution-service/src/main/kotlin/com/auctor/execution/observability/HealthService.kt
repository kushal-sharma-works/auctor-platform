package com.auctor.execution.observability

import com.auctor.execution.grpc.DefinitionGrpcClient
import javax.sql.DataSource

class HealthService(
    private val dataSource: DataSource,
    private val grpcClient: DefinitionGrpcClient
) {
    fun liveness(): Map<String, Any> {
        return mapOf("status" to "UP")
    }

    fun readiness(): Map<String, Any> {
        val dbUp = checkDatabase()
        val grpcUp = grpcClient.isChannelReady()
        val status = if (dbUp && grpcUp) "UP" else "DOWN"
        return mapOf(
            "status" to status,
            "checks" to mapOf(
                "db" to mapOf("status" to if (dbUp) "UP" else "DOWN"),
                "grpc" to mapOf("status" to if (grpcUp) "UP" else "DOWN")
            )
        )
    }

    private fun checkDatabase(): Boolean {
        return try {
            dataSource.connection.use { connection ->
                connection.isValid(2)
            }
        } catch (_: Exception) {
            false
        }
    }
}
