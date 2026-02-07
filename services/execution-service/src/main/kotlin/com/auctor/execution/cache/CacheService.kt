package com.auctor.execution.cache

import com.auctor.execution.grpc.DefinitionGrpcClient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import io.lettuce.core.RedisClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.time.Duration

/**
 * CacheService implements two-level caching for gRPC-based definition lookups:
 *  - L1: Caffeine in-process cache (fast, per-instance)
 *  - L2: Redis (shared across instances)
 */
class CacheService(
    private val grpcClient: DefinitionGrpcClient,
    redisUrl: String = "redis://localhost:6379"
) : AutoCloseable {

    private val l1Cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build<String, Map<String, Any>?>()

    private val redisClient = RedisClient.create(redisUrl)
    private val redisConn = redisClient.connect()
    private val mapper = jacksonObjectMapper()
    private val redisCommands = redisConn.async()

    private val L2_TTL_SECONDS = 300L // 5 minutes

    suspend fun getWorkflowCached(id: String, version: Int, authHeader: String? = null): Map<String, Any>? {
        val cacheKey = "workflow:$id:$version"
        
        // 1) L1
        l1Cache.getIfPresent(cacheKey)?.let { return it }

        // 2) L2 (Redis)
        val fromL2 = withContext(Dispatchers.IO) {
            try {
                val stored = redisCommands.get(cacheKey).await()
                stored?.let {
                    @Suppress("UNCHECKED_CAST")
                    mapper.readValue(it, Map::class.java) as? Map<String, Any>
                }
            } catch (e: Exception) {
                null
            }
        }
        if (fromL2 != null) {
            l1Cache.put(cacheKey, fromL2)
            return fromL2
        }

        // 3) Load from gRPC
        val loaded = grpcClient.getWorkflow(id, version, authHeader)
        if (loaded != null) {
            val result = mapOf(
                "id" to loaded.id,
                "name" to loaded.name,
                "version" to loaded.version,
                "status" to loaded.status,
                "states" to loaded.states,
                "initialState" to loaded.initialState,
                "transitions" to loaded.transitions.map {
                    mapOf(
                        "fromState" to it.fromState,
                        "toState" to it.toState,
                        "policyRef" to it.policyRef
                    )
                }
            )
            
            // store in L2
            try {
                val json = mapper.writeValueAsString(result)
                redisCommands.setex(cacheKey, L2_TTL_SECONDS, json).await()
            } catch (e: Exception) {
                // log and continue
            }
            
            // store in L1
            l1Cache.put(cacheKey, result)
            
            return result
        }

        return null
    }

    override fun close() {
        try {
            redisClient.shutdown()
        } catch (_: Exception) { }
    }
}
