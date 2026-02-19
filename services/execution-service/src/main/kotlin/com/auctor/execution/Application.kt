package com.auctor.execution

import com.auctor.execution.cache.CacheService
import com.auctor.execution.domain.ExecutionEngine
import com.auctor.execution.domain.ExecutionNotFoundException
import com.auctor.execution.grpc.DefinitionGrpcClient
import com.auctor.execution.http.installGraphQlRoutes
import com.auctor.execution.infra.db.ExposedAuditRepository
import com.auctor.execution.infra.db.ExposedExecutionRepository
import com.auctor.execution.observability.CorrelationIdPlugin
import com.auctor.execution.observability.ExecutionMetrics
import com.auctor.execution.observability.HealthService
import com.auctor.execution.observability.HttpTracingPlugin
import com.auctor.execution.observability.installMetricsRoute
import com.auctor.execution.observability.initTracing
import com.auctor.execution.security.AuthContextPlugin
import com.auctor.execution.security.configureAuth
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

fun main(args: Array<String>) {
    // Delegate to Ktor EngineMain to load application.conf without extra config dependencies.
    EngineMain.main(args)
}

fun Application.module(
    grpcClient: DefinitionGrpcClient? = null,
    cacheService: CacheService? = null,
    enableCache: Boolean = true,
    dataSource: HikariDataSource? = null,
    executionEngine: ExecutionEngine? = null
) {
    // Database configuration
    val actualDataSource = dataSource ?: createDataSource()
    
    // Run Flyway migrations (only if we created datasource)
    if (dataSource == null) {
        runMigrations(actualDataSource)
    }
    
    // Initialize Exposed database connection
    Database.connect(actualDataSource)
    
    // Content negotiation with custom JSON config
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // Status pages for error handling
    install(StatusPages) {
        exception<ExecutionNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "NOT_FOUND", "message" to (cause.message ?: "Resource not found"))
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "BAD_REQUEST", "message" to (cause.message ?: "Invalid request"))
            )
        }

        exception<IllegalStateException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                mapOf("error" to "CONFLICT", "message" to (cause.message ?: "Invalid state transition"))
            )
        }

        exception<Throwable> { call, cause ->
            if (cause is kotlinx.coroutines.CancellationException) {
                throw cause
            }
            logger.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "INTERNAL_ERROR", "message" to (cause.message ?: "An unexpected error occurred"))
            )
        }
    }

    // Authentication and auth context extraction
    configureAuth()
    install(AuthContextPlugin)

    // Initialize OpenTelemetry tracing
    initTracing()
    install(HttpTracingPlugin)
    install(CorrelationIdPlugin)

    // Metrics registry and standard JVM binders
    val meterRegistry = SimpleMeterRegistry()
    ClassLoaderMetrics().bindTo(meterRegistry)
    JvmMemoryMetrics().bindTo(meterRegistry)
    JvmGcMetrics().bindTo(meterRegistry)
    JvmThreadMetrics().bindTo(meterRegistry)
    ProcessorMetrics().bindTo(meterRegistry)
    val executionMetrics = ExecutionMetrics(meterRegistry)

    // Create gRPC client (shared) - use provided one for testing or create default
    val actualGrpcClient = grpcClient ?: DefinitionGrpcClient(
        targetHost = System.getenv("EXECUTION_GRPC_HOST")
            ?: environment.config.propertyOrNull("ktor.grpc.definition-service-host")?.getString()
            ?: "localhost",
        targetPort = System.getenv("EXECUTION_GRPC_PORT")?.toIntOrNull()
            ?: environment.config.propertyOrNull("ktor.grpc.definition-service-port")?.getString()?.toInt()
            ?: 9090,
        callDeadlineMs = 5000,
        metrics = executionMetrics
    )

    // Create cache service (shared) - use provided one for testing or create default
    // If cacheService is explicitly passed, respect it; otherwise, honor enableCache
    val actualCacheService = if (cacheService != null) {
        cacheService
    } else if (!enableCache) {
        null
    } else {
        try {
            CacheService(
                actualGrpcClient,
                redisUrl = System.getenv("EXECUTION_REDIS_URL")
                    ?: environment.config.propertyOrNull("ktor.redis.url")?.getString()
                    ?: "redis://localhost:6379",
                metrics = executionMetrics
            )
        } catch (e: Exception) {
            logger.warn("Failed to connect to Redis, continuing without cache", e)
            null
        }
    }

    // Create repositories
    val executionRepository = ExposedExecutionRepository()
    val auditRepository = ExposedAuditRepository()

    // Create execution engine (shared) - use provided one for testing or create default
    val actualExecutionEngine = executionEngine ?: ExecutionEngine(
        executionRepository = executionRepository,
        auditRepository = auditRepository,
        grpcClient = actualGrpcClient,
        metrics = executionMetrics
    )

    val healthService = HealthService(actualDataSource, actualGrpcClient)

    // Configure routes
    routing {
        installMetricsRoute(meterRegistry)
        // GraphQL endpoints (includes health/ready)
        installGraphQlRoutes(
            cacheService = actualCacheService,
            executionEngine = actualExecutionEngine,
            executionRepository = executionRepository,
            auditRepository = auditRepository,
            healthService = healthService
        )
    }

    // Shutdown hook to clean resources - only if we created them
    environment.monitor.subscribe(ApplicationStopped) {
        logger.info("Application stopping, cleaning up resources...")
        if (cacheService == null && actualCacheService != null) actualCacheService.close()
        if (grpcClient == null) actualGrpcClient.close()
        if (dataSource == null) actualDataSource.close()
    }
}

/**
 * Create HikariCP datasource for PostgreSQL.
 */
private fun Application.createDataSource(): HikariDataSource {
    val config = HikariConfig().apply {
        jdbcUrl = System.getenv("EXECUTION_DB_URL")
            ?: environment.config.propertyOrNull("ktor.database.url")?.getString()
            ?: "jdbc:postgresql://localhost:5432/execution"
        username = System.getenv("EXECUTION_DB_USER")
            ?: environment.config.propertyOrNull("ktor.database.user")?.getString()
            ?: "execution"
        password = System.getenv("EXECUTION_DB_PASSWORD")
            ?: environment.config.propertyOrNull("ktor.database.password")?.getString()
            ?: "execution"
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        minimumIdle = 2
        idleTimeout = 600000
        connectionTimeout = 30000
        isAutoCommit = true
        transactionIsolation = "TRANSACTION_READ_COMMITTED"
        validate()
    }
    return HikariDataSource(config)
}

/**
 * Run Flyway database migrations.
 */
private fun runMigrations(dataSource: HikariDataSource) {
    logger.info("Running Flyway migrations...")
    val flyway = Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .load()
    
    flyway.migrate()
    logger.info("Flyway migrations completed successfully")
}

