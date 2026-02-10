package com.auctor.execution.cache

import com.auctor.execution.grpc.DefinitionGrpcClient
import com.auctor.execution.observability.ExecutionMetrics
import com.auctor.execution.grpc.WorkflowDto
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import io.lettuce.core.RedisClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * CacheService implements two-level caching for gRPC-based definition lookups:
 *  - L1: Caffeine in-process cache (fast, per-instance)
 *  - L2: Redis (shared across instances)
 */
class CacheService(
    private val grpcClient: DefinitionGrpcClient,
    redisUrl: String = "redis://localhost:6379",
    private val metrics: ExecutionMetrics = ExecutionMetrics.noop()
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(CacheService::class.java)

    private val l1Cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build<String, WorkflowDto?>()

    private val redisClient = RedisClient.create(redisUrl)
    private val redisConn = redisClient.connect()
    private val mapper = jacksonObjectMapper()
    private val redisCommands = redisConn.async()

    private val L2_TTL_SECONDS = 300L // 5 minutes

    suspend fun getWorkflowCached(id: String, version: Int, authHeader: String? = null): WorkflowDto? {
        val cacheKey = "workflow:$id:$version"
        
        // 1) L1
        l1Cache.getIfPresent(cacheKey)?.let {
            metrics.recordCacheHit()
            return it
        }

        // 2) L2 (Redis)
        val fromL2 = withContext(Dispatchers.IO) {
            try {
                val stored = redisCommands.get(cacheKey).await()
                stored?.let { mapper.readValue(it, WorkflowDto::class.java) }
            } catch (e: Exception) {
                logger.warn("Redis cache read failed for $cacheKey", e)
                null
            }
        }
        if (fromL2 != null) {
            l1Cache.put(cacheKey, fromL2)
            metrics.recordCacheHit()
            return fromL2
        }

        // 3) Load from gRPC
        val loaded = grpcClient.getWorkflow(id, version, authHeader)
        if (loaded != null) {
            // store in L2
            try {
                val json = mapper.writeValueAsString(loaded)
                redisCommands.setex(cacheKey, L2_TTL_SECONDS, json).await()
            } catch (e: Exception) {
                logger.warn("Redis cache write failed for $cacheKey", e)
            }
            
            // store in L1
            l1Cache.put(cacheKey, loaded)

            metrics.recordCacheMiss()
            return loaded
        }

        metrics.recordCacheMiss()
        return null
    }

    override fun close() {
        try {
            redisClient.shutdown()
        } catch (_: Exception) { }
    }
}
