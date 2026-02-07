package com.auctor.execution.cache

import com.auctor.execution.grpc.DefinitionGrpcClient
import com.auctor.execution.grpc.DefinitionDto
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import io.lettuce.core.RedisClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.time.Duration

/**
 * CacheService implements two-level caching:
 *  - L1: Caffeine in-process cache (fast, per-instance)
 *  - L2: Redis (shared across instances)
 *
 * getOrLoad will:
 *  1) try L1
 *  2) if miss, try L2
 *  3) if miss, call DefinitionGrpcClient (passing auth token if available)
 *  4) populate L2 and L1 before returning
 *
 * TTLs: configurable constants below.
 */
class CacheService(
    private val grpcClient: DefinitionGrpcClient,
    redisUrl: String = "redis://localhost:6379"
) : AutoCloseable {

    private val l1Cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build<String, DefinitionDto?>()

    private val redisClient = RedisClient.create(redisUrl)
    private val redisConn = redisClient.connect()
    private val mapper = jacksonObjectMapper()
    private val redisCommands = redisConn.async()

    private val L2_TTL_SECONDS = 300L // 5 minutes

    suspend fun getOrLoad(id: String, authHeader: String? = null): DefinitionDto? {
        // 1) L1
        l1Cache.getIfPresent(id)?.let { return it }

        // 2) L2 (Redis)
        val fromL2 = withContext(Dispatchers.IO) {
            try {
                val stored = redisCommands.get(id).await() // returns String?
                stored?.let {
                    mapper.readValue(it, DefinitionDto::class.java)
                }
            } catch (e: Exception) {
                null
            }
        }
        if (fromL2 != null) {
            l1Cache.put(id, fromL2)
            return fromL2
        }

        // 3) Load from gRPC
        val loaded = grpcClient.getDefinition(id, authHeader)
        if (loaded != null) {
            // store in L2
            try {
                val json = mapper.writeValueAsString(loaded)
                // set with TTL
                redisCommands.setex(id, L2_TTL_SECONDS, json).await()
            } catch (e: Exception) {
                // log and continue (don't fail on Redis set)
            }
            // store in L1
            l1Cache.put(id, loaded)
        }

        return loaded
    }

    override fun close() {
        try {
            redisClient.shutdown()
        } catch (_: Exception) { }
    }
}
