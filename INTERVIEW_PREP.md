# Auctor Platform — Recruiter & Technical Interview Preparation Guide

This document prepares you for every question a European or Indian recruiter — or the senior engineers behind them — might ask about this project. It assumes a candidate profile of **5–6 years of experience** targeting a **Senior Software Engineer** role. Questions are grouped from softest ("tell me about yourself") to hardest ("walk me through thread safety in your cache strategy"). Each question is followed by a ready-to-deliver answer grounded entirely in the actual code.

---

## Table of Contents

1. [Name & Concept](#1-name--concept)
2. [Non-Technical / Background Questions](#2-non-technical--background-questions)
3. [Architecture & System Design](#3-architecture--system-design)
4. [Service Deep-Dive — definition-service (Java / Spring Boot)](#4-service-deep-dive--definition-service-java--spring-boot)
5. [Service Deep-Dive — execution-service (Kotlin / Ktor)](#5-service-deep-dive--execution-service-kotlin--ktor)
6. [Frontend Deep-Dive — web (Next.js / React)](#6-frontend-deep-dive--web-nextjs--react)
7. [Data Layer](#7-data-layer)
8. [Authentication & Security](#8-authentication--security)
9. [Observability](#9-observability)
10. [Infrastructure & DevOps](#10-infrastructure--devops)
11. [Code Style & Engineering Patterns](#11-code-style--engineering-patterns)
12. [Trade-offs, Failures & What I Would Do Differently](#12-trade-offs-failures--what-i-would-do-differently)
13. [File-by-File Reference Index](#13-file-by-file-reference-index)

---

## 1. Name & Concept

**Q: Why did you call it "Auctor"?**

*Auctor* is Latin for "author" or "originator" — the one who causes something to exist. The name was chosen deliberately because the platform is centred on **authoring workflow and policy definitions** that then govern how executions happen. Every execution is traceable back to a versioned definition that someone *authored*. The Latin root also signals a certain seriousness: this is a governance tool, not a general-purpose task runner.

**Q: In one sentence — what does Auctor do?**

Auctor lets you define named workflow state machines with policy-gated transitions, then execute those workflows in a separate runtime service while maintaining a tamper-evident audit trail of every state change and policy evaluation.

---

## 2. Non-Technical / Background Questions

**Q: Walk me through this project from beginning to end.**

I built Auctor as a portfolio platform that demonstrates a production-grade, polyglot microservices architecture in a single monorepo. The problem I set out to solve is real: organisations running multi-step approval or processing workflows need strict governance (immutable definitions, policy-gated transitions) combined with a high-throughput runtime and a full audit trail. I split those concerns across two backend services, connected them internally via gRPC, and built a Next.js UI on top. The whole stack is containerised and can spin up locally with one Docker Compose command.

**Q: Why a monorepo?**

Cross-service changes — for example, updating a shared protobuf contract — need to land atomically. With a polyrepo, every such change fans out to multiple PRs, multiple CI pipelines, and potential version drift. A monorepo keeps the release cadence aligned. The downside is a slightly larger CI scope per commit, which I mitigate with per-service CI jobs (`.github/workflows/ci-definition-service.yml`, `ci-execution-service.yml`, `ci-web.yml`) that only trigger on changes to their respective paths.

**Q: Why did you pick two different backend languages?**

Intentional. The **definition-service** manages long-lived, transactional domain objects (workflow and policy definitions) where a rich Spring ecosystem — JPA, Flyway, Spring Security, Spring GraphQL — handles heavy lifting quickly and reliably. Java 21 with virtual threads means the IO-bound Spring stack can handle high concurrency without a complex reactive chain. The **execution-service** is a lightweight, coroutine-native Ktor service optimised for throughput; Kotlin's coroutines and structured concurrency map naturally to async database operations and parallel gRPC calls without callback hell. Using two languages is a deliberate trade-off, not a mistake — each service plays to its language's strengths.

**Q: How long did it take to build and what were the hardest parts?**

The hardest parts were: (1) the two-level cache in `CacheService.kt` — getting Redis and Caffeine to coexist correctly across coroutine context switches required careful use of `Dispatchers.IO`; (2) the gRPC circuit breaker in `DefinitionGrpcClient.kt` — handling `CancellationException` without swallowing it (structured concurrency requires you propagate cancellations, never catch them silently); and (3) the authentication bridge — verifying a Google ID token server-side and then minting a platform JWT that both backend services trust, with roles extracted on each side.

**Q: Who is the target user?**

Operations or platform teams that need auditable, policy-driven workflows — think loan approval pipelines, compliance sign-off flows, content review processes.

**Q: Why not just use an existing workflow engine like Camunda or Temporal?**

Both are excellent. Camunda introduces BPMN complexity that many teams don't need; Temporal adds a significant operational footprint (separate server cluster, specific SDK patterns). For a deterministic, state-machine-style workflow with policy conditions and a strict audit trail, a focused custom engine demonstrates the underlying mechanics more clearly and avoids over-engineering for the scope. I documented this choice explicitly as a "Deliberately Not Included" item in the README.

---

## 3. Architecture & System Design

**Q: Describe the overall architecture.**

```
Browser
  │
  ▼
Next.js web (port 3000)
  │   /api/definition-graphql  ──────────────────► definition-service (8081)
  │   /api/execution-graphql   ──────────────────► execution-service (8082)
  │                                                      │
  │                                                      │ gRPC (9090)
  │                                                      ▼
  │                                               definition-service
  │
  └─ Both services ──► PostgreSQL (5432)
     execution-service ──► Redis (6379)
```

The browser never calls backend services directly. Every request goes through Next.js API routes, which forward the bearer token from the cookie. This means the backend services are never exposed to raw Google tokens — only to the platform JWT the web layer issues.

**Q: Why GraphQL for the UI-facing layer and gRPC for service-to-service?**

GraphQL suits the UI because it lets the frontend request exactly the fields it needs (avoids over-fetching), supports pagination natively, and makes schema changes visible and type-safe via the SDL. The Next.js app uses `graphql-request` to keep bundle size minimal.

gRPC suits service-to-service for the opposite reasons: the contract is a `.proto` file that both sides compile against, giving compile-time guarantees. Serialisation via Protocol Buffers is 5–10× more compact than JSON, which matters for the hot-path lookup of workflow definitions on every `advanceExecution` call. Additionally, gRPC gives free deadline propagation and cancellation — I use `withDeadlineAfter` in `DefinitionGrpcClient.kt`.

**Q: How does a "start execution" request flow through the system?**

1. The user clicks "Start" in the UI.
2. The Next.js API route at `/api/execution-graphql` proxies the GraphQL mutation, attaching the `auctor.auth.token` cookie as an `Authorization: Bearer` header.
3. `execution-service` receives the mutation, resolves it in `GraphQLProvider.kt`, and calls `ExecutionEngine.startExecution()`.
4. `ExecutionEngine` calls `grpcClient.getWorkflow()` with a 5-second timeout and validates that the workflow is `PUBLISHED`.
5. An `Execution` record (status=`RUNNING`, state=`<initialState>`) is created, plus an `EXECUTION_STARTED` audit event.
6. Both are persisted atomically via `ExposedExecutionRepository.saveWithAudit()` inside a single suspended transaction.
7. The execution object is returned up the chain to the GraphQL response.

**Q: How do you handle a policy-gated transition?**

In `ExecutionEngine.advanceExecution()`, the engine iterates through valid transitions from the current state. If a transition has a `policyRef`, it calls `grpcClient.evaluatePolicy()` with the execution's `input` map as context. The definition-service evaluates all policy conditions (AND logic) in `PolicyEvaluator.evaluate()` and returns `allowed` + an explanation string. A `POLICY_EVALUATED` audit event is written regardless of outcome. The first transition whose policy returns `allowed=true` (or which has no policy at all) is selected. If no transition is allowed, an `IllegalStateException` is thrown with all policy denial explanations concatenated, and execution remains in its current state.

**Q: How do you ensure an execution cannot be advanced after it is complete?**

`ExecutionEngine.advanceExecution()` checks:
```kotlin
if (execution.status is ExecutionStatus.Completed || execution.status is ExecutionStatus.Failed) {
    throw IllegalStateException("Execution ${execution.id} is already in terminal state: ${execution.status}")
}
```
This is a domain-level guard before any database write. The `ExecutionStatus` sealed class means the compiler forces exhaustive handling everywhere that status is inspected.

**Q: How does the system detect terminal states?**

After a successful state transition, the engine checks whether the new state has any outgoing transitions in the workflow definition:
```kotlin
val outgoingTransitions = workflow.transitions.filter { it.fromState == newState }
```
If the list is empty, the execution is marked `Completed` and an `EXECUTION_COMPLETED` audit event is appended. This means terminal states are emergent from the workflow graph rather than hard-coded.

**Q: Why split definition-service and execution-service at all?**

- **Release independence**: definitions change infrequently and need strict review; the execution runtime needs to be deployable independently for bug fixes.
- **Load isolation**: execution can be scaled horizontally without touching the definition store.
- **Access control clarity**: only definition-service writes definitions; execution-service is read-only on definitions (via gRPC).
- **Compliance**: immutable definitions form an audit anchor — a workflow version used in an execution three years ago can be re-read exactly as it was.

---

## 4. Service Deep-Dive — definition-service (Java / Spring Boot)

**Q: Why Java 21 and Spring Boot 3.4?**

Java 21 is the current LTS with virtual threads (Project Loom) available via `spring.threads.virtual.enabled: true` in `application.yml`. This transforms Spring's traditional thread-per-request model into a virtual-thread-per-request model with almost no code change, improving concurrency for IO-bound workloads (database calls, gRPC serving) without the complexity of reactive programming. Spring Boot 3.4 brings its native image support, AOT compilation, and Jakarta EE 10 alignment.

**Q: Explain the Hexagonal Architecture pattern used here.**

The definition-service is structured into four layers:

| Package | Role |
|---|---|
| `domain.model` | Pure domain objects (`WorkflowDefinition`, `PolicyDefinition`, etc.) — no framework annotations |
| `domain.service` | Domain logic (`WorkflowService`, `PolicyService`, `PolicyEvaluator`) — depends only on ports |
| `domain.port` | Interface contracts (`WorkflowCommandPort`, `WorkflowQueryPort`, etc.) |
| `infra.adapter` | JPA implementations of ports (`JpaWorkflowCommandAdapter`, etc.) |
| `api.*` | Inbound adapters: GraphQL controllers, REST DTOs, gRPC service |

The domain never imports Spring or JPA. Tests can exercise domain logic with mock ports, no database needed. This pattern is sometimes called Ports & Adapters or Clean Architecture.

**Q: Why are WorkflowDefinition and PolicyDefinition immutable?**

Mutability in a domain object is a common source of bugs — a shared reference mutated in one place breaks invariants everywhere else. Java 21 records (like `PolicyId`, `WorkflowId`) and hand-rolled immutable classes (like `WorkflowDefinition`) make this explicit. The `withStatus()` and `withVersion()` methods on `WorkflowDefinition` return new instances rather than mutating in place, preserving thread safety. `List.copyOf()` in the constructor ensures the transition and state lists cannot be mutated by the caller after construction.

**Q: How does `WorkflowStatus` as a sealed interface work in Java 21?**

```java
public sealed interface WorkflowStatus permits WorkflowStatus.Draft, WorkflowStatus.Published {
    String label();
    record Draft() implements WorkflowStatus { ... }
    record Published() implements WorkflowStatus { ... }
}
```
The `sealed` keyword restricts which classes can implement the interface. This means any `switch` on `WorkflowStatus` can be compiler-checked for exhaustiveness — if you add a third state (`Deprecated`) and forget to handle it in a switch, the compiler flags it. This is the same motivation as Kotlin/Scala sealed classes, now available in Java.

**Q: Explain the GraphQL schema and controller design.**

The schema in `schema.graphqls` defines two `Query` operations (`workflow`, `workflows`, `policy`, `policies`) and four `Mutation` operations. Spring GraphQL uses `@QueryMapping` and `@MutationMapping` annotations in `WorkflowGraphQLController` and `PolicyGraphQLController` to wire resolver methods. Input types (`CreateWorkflowInput`) are separate from domain objects to decouple GraphQL API surface from internal model changes. A `GraphQLExceptionHandler` translates domain exceptions (`EntityNotFoundException`) into GraphQL partial responses rather than HTTP 500s.

**Q: How does the JPA persistence layer work?**

`WorkflowJpaEntity` is a `@Entity` with a composite key (`WorkflowDefinitionId` = id + version). The `transitions` field is stored as JSON using `TransitionListConverter` (a JPA `AttributeConverter`), which serialises `List<TransitionDto>` to a `TEXT` column via Jackson. This avoids a separate `transitions` table for what is effectively an embedded value list. Flyway manages schema migrations from `src/main/resources/db/migration`.

**Q: Why is there both a REST and a GraphQL API in definition-service?**

The REST DTOs (`CreateWorkflowRequest`, `WorkflowResponse`, etc.) exist as an alternative inbound adapter, useful for non-browser clients or testing. The GraphQL adapter is the primary path for the UI. Having both follows the Ports & Adapters design — multiple inbound adapters, same domain.

**Q: What does `PolicyEvaluator.evaluate()` do?**

It iterates over all `PolicyCondition`s in a policy and evaluates each using a Java 21 switch expression over the `Operator` enum. The operators are `EQ`, `NEQ`, `GT`, `LT`, `GTE`, `LTE`, `IN`, `NOT_IN`. String comparisons use `equalsIgnoreCase` and `Locale.ROOT` to avoid locale-dependent case folding (e.g., Turkish dotted-I problem). Numeric comparisons parse both sides as `double`, which covers integer and decimal cases. If *any* condition fails, evaluation short-circuits and returns `allowed=false` with an explanation. All conditions must pass — AND semantics.

**Q: Why Micrometer for metrics rather than raw Prometheus client?**

Micrometer is a metrics facade (like SLF4J for logging), vendor-neutral. The `MeterRegistry` dependency in `WorkflowService` and `PolicyEvaluator` means metrics can be published to Prometheus, Datadog, CloudWatch, or any other backend by changing the registry implementation, with zero code change. The `micrometer-registry-prometheus` dependency wires it to Prometheus; the `/actuator/prometheus` endpoint exposes the scrape target.

**Q: How does gRPC authentication work in definition-service?**

`JwtGrpcInterceptor` implements `ServerInterceptor`. On every incoming gRPC call it reads the `Authorization` metadata header, validates the JWT (same secret/issuer/audience as the HTTP path), and sets a `SecurityContext` so the downstream gRPC service method runs with an authenticated principal. `GrpcMetricsInterceptor` and `GrpcTracingInterceptor` add Micrometer counters and OTel span propagation respectively to every gRPC call.

---

## 5. Service Deep-Dive — execution-service (Kotlin / Ktor)

**Q: Why Ktor instead of Spring Boot for the execution service?**

Ktor is a lightweight, coroutine-first framework with no annotation magic. The execution service is mostly: receive a GraphQL payload → call gRPC → write to DB → return result. Ktor's plugin system (`install(Authentication)`, `install(ContentNegotiation)`) is explicit and composable. The total JAR is significantly smaller than a Spring Boot fat JAR. Coroutines (rather than thread pools) align with the async gRPC client calls and suspended Exposed transactions, avoiding the need for reactive wrapper APIs.

**Q: What is structured concurrency and how is it used here?**

Kotlin's structured concurrency means coroutines are always launched within a parent scope. When `ExecutionEngine.startExecution()` is called inside `coroutineScope { }`, any child coroutine failure cancels the parent scope. `CancellationException` is special: it must never be swallowed. In `DefinitionGrpcClient.retryWithBackoff()`:
```kotlin
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e  // must re-throw to honour cancellation
}
```
If `withTimeout(5000)` fires, it throws `TimeoutCancellationException` (a subclass of `CancellationException`) — catching and swallowing it would break the contract.

**Q: Explain the circuit breaker in `DefinitionGrpcClient`.**

Two `AtomicLong`/`AtomicInteger` counters track consecutive failures and when the circuit should reopen. If `consecutiveFailures >= 5`, the circuit opens for 30 seconds (`circuitOpenUntil` is set to `now + 30_000ms`). Subsequent calls throw immediately without hitting gRPC. On the first success, both counters reset. This pattern is a manual implementation of the Circuit Breaker resilience pattern (as described in Release It! by Michael Nygard). An atomic compare-and-swap would be preferable in very high-concurrency scenarios, but the current implementation uses `AtomicInteger.incrementAndGet()` and `AtomicLong.set()` which are thread-safe.

**Q: How does the two-level cache work?**

`CacheService.getWorkflowCached()` implements a standard read-through, write-through L1/L2 cache:

1. **L1 (Caffeine)**: in-process, max 10,000 entries, 5-minute TTL. Sub-microsecond lookup, no network IO.
2. **L2 (Redis)**: shared across all instances of execution-service, 5-minute TTL. Used for warm-up after JVM restarts.
3. **L3 (gRPC)**: actual definition-service call if both caches miss.

On a cache miss, the loaded value is written to Redis (`SETEX` with TTL), then to Caffeine. Redis operations use `Dispatchers.IO` to avoid blocking the coroutine dispatcher. Redis failures are caught and logged as warnings rather than propagated — the cache is a performance optimisation, not a correctness requirement.

**Q: Why is the GraphQL server in execution-service implemented with graphql-java directly rather than a framework?**

The Ktor ecosystem does not have a mature opinionated GraphQL plugin comparable to Spring GraphQL. Using `graphql-java` directly (`GraphQLProvider.kt`, `GraphQLRoutes.kt`) is intentional — it keeps the schema definition explicit (loaded from a `.graphqls` file), the resolver wiring transparent, and adds no magic. It also avoids adding a second heavy framework.

**Q: How does the execution-service authenticate requests?**

`AuthModule.kt` installs three `jwt()` authentication configurations on Ktor: `auth-jwt` (any valid JWT), `auth-viewer` (ADMIN or VIEWER roles), `auth-executor` (ADMIN or EXECUTOR roles). Routes are wrapped with `authenticate("auth-executor")` or `authenticate("auth-viewer")` as appropriate. The JWT validator uses the same secret/issuer/audience configuration as definition-service, so both services accept the same platform JWT minted by the web layer.

`AuthContextPlugin.kt` extracts claims from the validated JWT and stores them in `call.attributes` so route handlers can call `call.authContextOrNull()` to get the current actor's email and roles without re-parsing the token.

**Q: How does the audit trail work?**

Every significant event writes an `AuditEvent` record to the `audit_events` table via `ExposedAuditRepository`. Events are append-only (there is no `update` or `delete` on audit records). The event types are `EXECUTION_STARTED`, `STATE_TRANSITION`, `POLICY_EVALUATED`, `EXECUTION_COMPLETED`. Each event carries a `correlationId` (a UUID generated per web request), the `actor` (the user's email from the JWT), timestamps, and optional policy evaluation results and explanations. This provides a deterministic replay record.

**Q: Why Exposed and not JPA for execution-service?**

Exposed is a Kotlin-first SQL DSL and DAO library from JetBrains. It integrates naturally with coroutines via `newSuspendedTransaction(Dispatchers.IO) { }`. JPA's session scoping (EntityManager lifecycle) does not map cleanly to coroutine context; using Exposed avoids subtle bugs around session/transaction boundaries when coroutines switch threads. Exposed also gives finer-grained control over SQL generation, which matters for audit trail writes that need predictable behaviour under load.

---

## 6. Frontend Deep-Dive — web (Next.js / React)

**Q: Why Next.js 14 with the App Router?**

The App Router enables React Server Components, which means the dashboard page (`app/page.tsx`) renders its layout server-side (no client JS for the outer shell) while delegating the dynamic data sections to `DashboardClient.tsx` (a client component with `"use client"`). This gives a fast first-contentful-paint and good Core Web Vitals out of the box. The `/api/*` routes are serverless functions that handle auth token exchange without a separate backend-for-frontend service.

**Q: How does the authentication flow work on the frontend?**

1. User lands on `/login`, clicks "Sign in with Google".
2. Google's JS SDK returns a Google ID token.
3. The frontend `POST`s the ID token to `/api/auth/google/route.ts`.
4. The API route verifies the ID token against Google's JWKS endpoint using `jose`.
5. A platform JWT is signed (HMAC-SHA256 with `AUCTOR_JWT_SECRET`) containing `sub`, `email`, `roles`, `iss`, `aud`, `exp`.
6. The JWT is set as a cookie (`auctor.auth.token`) and also returned in the response body.
7. The middleware (`middleware.ts`) checks for this cookie on every non-public `GET` request; unauthenticated users are redirected to `/login`.

**Q: How does the Next.js app proxy GraphQL calls?**

There are two proxy routes: `/api/definition-graphql` and `/api/execution-graphql`. Each route reads the `auctor.auth.token` cookie, forwards the GraphQL payload to the appropriate backend service with `Authorization: Bearer <token>`, and streams the response back. This keeps CORS simple (same origin from the browser's perspective) and ensures the token is never exposed in client-side code.

**Q: What state management approach is used?**

The project uses a combination of:
- **`@tanstack/react-query`**: for server state (fetching, caching, invalidating GraphQL responses). Keeps server state out of Redux.
- **`@reduxjs/toolkit`**: for client-side UI state (currently minimal — auth slice, loading indicators).
- **`react-hook-form` + `zod`**: for form validation on the workflow/policy creation modals.

This layered approach separates concerns: `react-query` owns "what does the server say?", Redux owns "what has the user done locally?".

**Q: What is the `ProtectedRoute` component?**

`ProtectedRoute.tsx` is a client-side guard that reads the JWT from a cookie (or Redux store), decodes it with `jose`, and checks expiry. If the token is missing or expired, it redirects to `/login`. This works in tandem with the `middleware.ts` server-side guard for defence in depth.

**Q: Why Chakra UI over Tailwind-only components?**

Chakra UI provides accessible, theme-consistent components (modals, tables, form controls) out of the box with proper `aria-*` attributes, keyboard navigation, and focus management. Tailwind is still used for layout and utility classes on custom elements. Using both means not reinventing modal accessibility while retaining flexibility for custom layouts.

---

## 7. Data Layer

**Q: Why PostgreSQL instead of a NoSQL database?**

Workflow definitions and execution states have relational integrity requirements: a transition must reference valid states within the same workflow, a policy must exist before being referenced by a workflow, an execution must reference an existing workflow version. PostgreSQL's ACID guarantees, foreign key constraints (via Flyway-managed schema), and support for JSONB (used for `conditions` and `transitions` column storage) cover all these needs without the operational complexity of a separate document store.

**Q: Why store transitions and conditions as JSON columns instead of separate tables?**

Transitions are always read and written as a complete list alongside their parent workflow definition. They have no independent query or mutation lifecycle. Normalising them into a separate table would add a join on every read with no benefit. The JSON column approach (via `TransitionListConverter` in definition-service) keeps the data co-located and the schema simple. The converter uses Jackson serialisation, which is tested.

**Q: How does schema migration work?**

Both services use Flyway. Migration scripts live in `src/main/resources/db/migration/` (definition-service) and the equivalent path in execution-service. Flyway runs on startup, compares the `flyway_schema_history` table to the classpath scripts, and applies any pending migrations in version order. The `ddl-auto: validate` setting in definition-service ensures Hibernate validates the schema at startup without making changes — Flyway owns all DDL.

**Q: What is the purpose of Redis beyond caching?**

Currently Redis serves only as the L2 cache for workflow definition lookups in execution-service. In a production scale-up, it could also serve as a distributed lock (Redlock) for concurrent execution advancement on the same execution ID, or as a pub/sub channel for execution state change events. These extensions are architecturally prepared for but not yet implemented.

---

## 8. Authentication & Security

**Q: Why not just validate the Google ID token in the backend services directly?**

The backend services use a platform JWT with internal claims (`roles`, `aud=definition-service,execution-service`, `iss=auctor-auth`). Google tokens carry Google's claims (which can change) and are signed by Google's private keys (which rotate and require JWKS lookups). Bridging at the web layer means: (1) backend services only need to trust one issuer; (2) role assignment (`ADMIN`, `EXECUTOR`) lives in one place (`lib/admins.ts`); (3) the same JWT works for both services with the same secret; (4) if Google login is ever replaced (e.g., with SSO/SAML), only the web auth route changes.

**Q: How are roles assigned?**

In `web/app/api/auth/google/route.ts`, every authenticated user receives the `EXECUTOR` role. If the email matches the admin list in `lib/admins.ts`, they also receive `ADMIN`. The roles array is embedded in the JWT. Backend services extract roles from the `roles` claim and enforce them:
- definition-service: `SecurityConfig.java` — GraphQL endpoint requires `VIEWER`, `ADMIN`, or `EXECUTOR`.
- execution-service: `AuthModule.kt` — routes are tagged with `auth-viewer` or `auth-executor` authentication configurations.

**Q: Is there an optional dev login? How is it secured?**

Yes. `ENABLE_DEV_LOGIN=true` enables a dev login path that bypasses Google verification. This is gated behind an environment variable and is never enabled in non-local profiles. It exists solely to reduce friction for local demo sessions where Google credentials are unavailable. The security documentation in `docs/architecture.md` and `docs/local-run.md` explicitly calls out that these local defaults must be hardened for production.

**Q: How is CORS configured?**

`CorsConfig.java` in definition-service uses Spring's CORS support to allow requests from the web layer origin. The Ktor execution-service installs the `CORS` plugin with matching configuration. Both restrict allowed methods and headers. In local mode the allowed origin is set via environment variable.

**Q: What would you harden for a production deployment?**

- Rotate `AUCTOR_JWT_SECRET` to a high-entropy secret managed by Azure Key Vault (already provisioned in `infra/terraform/azure/main.tf`).
- Move from symmetric HMAC JWT signing to asymmetric RS256 (public key on backends, private key only in web layer).
- Restrict CORS to the specific production domain.
- Enable PostgreSQL SSL and Redis TLS (Terraform already sets `minimum_tls_version = "1.2"` for Redis).
- Add network policies in Kubernetes to restrict traffic between pods.

---

## 9. Observability

**Q: What observability stack is used?**

| Component | Purpose |
|---|---|
| OpenTelemetry (OTel) SDK | Instrumentation and trace export |
| Jaeger | Distributed trace storage and UI |
| Prometheus | Metrics scraping |
| Grafana | Dashboards |
| Logback + logstash-logback-encoder | Structured JSON logging |

**Q: How is trace context propagated across services?**

Both services initialise OTel with `W3CTraceContextPropagator` — the standard `traceparent` / `tracestate` HTTP headers. The OTel gRPC interceptor (`opentelemetry-grpc-1.6`) in both definition-service and execution-service propagates trace context over gRPC metadata automatically. This means a single user request generates a single trace tree visible in Jaeger, spanning the web proxy → execution-service → gRPC → definition-service hops.

**Q: What is the `CorrelationIdFilter` and why is it different from the trace ID?**

`CorrelationIdFilter.java` in definition-service (and `CorrelationIdPlugin.kt` in execution-service) manages a business-level correlation ID. The trace ID is an OTel concern used for distributed tracing infrastructure. The correlation ID is an application-level concept passed in the `X-Correlation-ID` header that can be used to correlate log entries across services even if tracing infrastructure is absent. If an OTel trace is active, the filter uses the trace ID as the correlation ID for consistency; otherwise it generates a UUID.

**Q: What metrics are exported?**

In definition-service:
- `workflow.created.total` (counter)
- `workflow.published.total` (counter)
- `policy.evaluation.total` tagged with `result=allowed|denied`
- `policy.evaluation.duration` (timer)

In execution-service:
- `execution.started.total`, `execution.completed.total`, `execution.failed.total` (counters)
- `execution.duration` (timer, measures wall-clock time from start to completion)
- `execution.state_transition.total` tagged with `from_state`, `to_state`
- `grpc.client.request.duration` tagged with `method`, `status`
- `cache.hit.total`, `cache.miss.total` (counters)

These feed into the Grafana dashboard defined under `infra/monitoring/grafana/`.

---

## 10. Infrastructure & DevOps

**Q: Walk me through the Terraform configuration.**

`infra/terraform/azure/main.tf` provisions on Azure:
- **Resource Group** (`azurerm_resource_group`)
- **AKS cluster** with system-assigned managed identity
- **ACR** (Azure Container Registry) with `AcrPull` role assigned to the AKS kubelet identity — pods can pull images without explicit credentials
- **PostgreSQL Flexible Server** with two databases (`definition`, `execution`)
- **Redis Cache** (minimum TLS 1.2)
- **Key Vault** with RBAC authorisation and purge protection

The `random_string` suffix prevents name collisions across environments. Variables are in `variables.tf`; example values in `terraform.tfvars.example`. Remote state is configured in `backend.tf`.

**Q: What does the Helm chart do?**

`infra/helm/` is a single Helm chart (`auctor`) with three deployments: definition-service, execution-service, and web. Values files per environment (`values-dev.yaml`, `values-sit.yaml`) override image tags, replica counts, resource limits, and environment-specific secrets (referenced from Kubernetes secrets, not hardcoded). The chart uses a unified deployment template in `templates/`.

**Q: How does GitOps work with Argo CD?**

`infra/argocd/project.yaml` defines the Argo CD project with `sourceRepos` pointing to this repo and `destinations` pointing to the AKS namespaces. `application-dev.yaml` and `application-sit.yaml` are Argo CD Application manifests that point to the Helm chart path in this repo. When a CI pipeline builds and pushes a new image, it updates the image tag in the values file, commits it back to the repo, and Argo CD syncs the cluster to match the desired state — no manual `kubectl apply` needed.

**Q: What CI workflows exist?**

| Workflow | Trigger | What it does |
|---|---|---|
| `ci-definition-service.yml` | push to paths under `services/definition-service/**` | Maven build + unit tests |
| `ci-execution-service.yml` | push to paths under `services/execution-service/**` | Gradle build + unit tests |
| `ci-web.yml` | push to paths under `web/**` | npm install, lint, Jest tests, Next.js build |
| `docker.yml` | configured branches | Docker build + push to ACR for all services |
| `deploy-sit.yml` | manual / tagged release | Helm upgrade to SIT |
| `security-scan.yml` | scheduled / PR | Dependency vulnerability scanning |
| `dependency-review.yml` | PR | GitHub Dependency Review Action |
| `ci-infra.yml` | push to paths under `infra/**` | `terraform validate` |
| `workflow-lint.yml` | push to `.github/workflows/**` | Lint CI YAML files |

---

## 11. Code Style & Engineering Patterns

**Q: Why plain Java accessor methods (`id()`, `name()`) instead of Lombok or records in WorkflowDefinition?**

`WorkflowDefinition` is a hand-rolled immutable class rather than a `record` because it has non-trivial constructor validation logic (checking that all transition states exist in the states list). Java records generate a compact canonical constructor but validation is easier to read as explicit imperative code. Lombok was intentionally excluded — it hides code generation, makes the bytecode harder to reason about, and adds a build-time annotation processor dependency. Explicit accessors are more readable in a team context.

**Q: Why are the domain ports interfaces rather than abstract classes?**

Interfaces enable multiple implementations (JPA adapter for production, in-memory adapter for tests) with no inheritance coupling. The domain service depends on the port interface; the Spring DI container wires the correct adapter at runtime. In tests, a simple `HashMap`-backed implementation can stub the port without Mockito.

**Q: How are DTOs mapped between layers?**

- In definition-service: `DomainMapper.java` handles JPA entity ↔ domain model. `DtoMapper.java` handles domain model ↔ REST response. `GraphQL*Dto` classes are the GraphQL layer's view.
- In execution-service: `GrpcDtos.kt` holds data classes that mirror the proto-generated types but live in the Kotlin domain, avoiding tight coupling to generated code.

**Q: Why is there a `noop()` factory on `ExecutionMetrics`?**

```kotlin
companion object {
    fun noop(): ExecutionMetrics {
        return ExecutionMetrics(SimpleMeterRegistry())
    }
}
```
This provides a no-op implementation for tests that do not care about metrics, without requiring Mockito mocking. It avoids `NullPointerException` if a metrics instance is accidentally absent. This is the Null Object pattern.

**Q: Why are Kotlin coroutine-based database calls wrapped in `newSuspendedTransaction(Dispatchers.IO)`?**

JDBC calls are blocking by nature. `Dispatchers.IO` is a thread pool optimised for blocking operations. By wrapping database calls in `newSuspendedTransaction(Dispatchers.IO)`, the coroutine suspends on the coroutine dispatcher, runs the blocking JDBC work on the IO pool, and resumes on the original dispatcher when done. This prevents blocking the main coroutine thread pool (which handles request routing).

**Q: Why does `retryWithBackoff` use `1 shl (attempt - 1)` for delays?**

`1 shl n` is Kotlin's bitwise left-shift, equivalent to `2^n`. For `attempt = 1, 2, 3`: delays are `100 * 1 = 100ms`, `100 * 2 = 200ms`, `100 * 4 = 400ms`. This is exponential backoff with a base delay of 100ms. The pattern reduces thundering herd when definition-service restarts and all execution-service instances retry simultaneously.

**Q: Why is `httpOnly: false` on the auth cookie?**

This is intentional for the demo/local profile. The frontend JavaScript needs to read the token to attach it as a header for GraphQL calls. A `httpOnly: true` cookie cannot be read by JS.

> ⚠️ **Security note**: `httpOnly: false` exposes the token to JavaScript and therefore to XSS attacks. This must **never** be used in production. In a production setup, the pattern shifts to the Next.js API route always reading the cookie server-side and injecting the `Authorization` header before proxying to backend services — the browser JS never sees the raw token, and the cookie is set with `httpOnly: true`. The README and `docs/local-run.md` explicitly call out that local defaults must be hardened for production.

---

## 12. Trade-offs, Failures & What I Would Do Differently

**Q: What would you add first if this went to production?**

1. **Distributed locking** for concurrent `advanceExecution` on the same execution ID — currently two concurrent requests could both read the same state and both apply a transition. A Redis Redlock or database advisory lock would prevent this.
2. **Idempotency keys** on mutations, so retried requests from the UI don't create duplicate executions.
3. **Asymmetric JWT signing** (RS256) to separate signing authority from verification.
4. **Rate limiting** at the Next.js API route layer.
5. **Testcontainers integration tests** in CI — they exist in the codebase but are excluded from the default CI run via `skipITs=true`.

**Q: What is the hardest bug you can imagine in this codebase?**

A race condition in `advanceExecution`: two concurrent requests for the same execution ID both read `status=RUNNING` from the database, both evaluate policy, both attempt to write the new state. The second write would overwrite the first, potentially skipping a state and losing an audit event. The `updateWithAudit` call is not atomic with the initial `findById`. Fixing this requires either optimistic locking (a `version` column with a CAS update) or a `SELECT FOR UPDATE` in the database transaction.

**Q: Why not use Kafka for execution events?**

Kafka adds significant operational overhead: schema registry, consumer group management, at-least-once delivery semantics, replay logic. For the current scope (single execution engine, synchronous API), Kafka would be premature. The append-only audit table already provides replay capability. If execution-service needed to fan out events to multiple downstream consumers asynchronously, Kafka would become the right choice.

**Q: What would you monitor most closely in production?**

- `execution.failed.total` rate — a spike indicates workflow or policy configuration errors.
- `grpc.client.request.duration{method="DefinitionService/GetWorkflow"}` P99 — a rise indicates definition-service latency degrading execution throughput.
- Circuit breaker state via `consecutiveFailures` (currently not exported as a metric — would add this).
- Redis cache hit ratio (`cache.hit.total / (cache.hit.total + cache.miss.total)`) — a drop means definition-service load is higher than expected.

---

## 13. File-by-File Reference Index

This section maps every file to a one-sentence description. Use it when an interviewer asks "what does *X* file do?"

### Root
| File | Purpose |
|---|---|
| `README.md` | Project overview, stack table, local run commands, design decisions summary |
| `docker-compose.yml` | Local stack definition: definition-service, execution-service, web, PostgreSQL, Redis, OTel collector, Jaeger, Prometheus, Grafana |
| `package.json` | Root-level npm workspace config (minimal — workspaces are managed per-service) |
| `INTERVIEW_PREP.md` | This file |

### `docs/`
| File | Purpose |
|---|---|
| `architecture.md` | Mermaid architecture diagram, service responsibility table, protocol choices, key data flows |
| `decisions.md` | Architecture Decision Records (ADR-001 to ADR-008) |
| `local-run.md` | Step-by-step local demo runbook for recruiters |
| `project-operating-manual.md` | Comprehensive reference: what the platform is, how it runs, rationale, runbooks, risks |

### `services/definition-service/`
| File | Purpose |
|---|---|
| `pom.xml` | Maven build: Spring Boot 3.4.1, Java 21, gRPC, Protobuf, Flyway, Testcontainers, Micrometer, OTel |
| `src/main/proto/` | `.proto` files defining the gRPC contract between execution-service (client) and definition-service (server) |
| `DefinitionServiceApplication.java` | Spring Boot entry point with `@SpringBootApplication` |
| `domain/model/WorkflowDefinition.java` | Immutable domain object: id, name, version, status, states, transitions. Validates invariants in constructor. |
| `domain/model/WorkflowStatus.java` | Sealed interface with `Draft` and `Published` record implementations |
| `domain/model/PolicyDefinition.java` | Immutable domain object for policy: id, name, version, status, conditions |
| `domain/model/PolicyStatus.java` | Sealed interface: `Draft`, `Published` |
| `domain/model/PolicyCondition.java` | Value object: field, operator, value |
| `domain/model/Operator.java` | Enum: `EQ`, `NEQ`, `GT`, `LT`, `GTE`, `LTE`, `IN`, `NOT_IN` |
| `domain/model/EvaluationResult.java` | Value object: `allowed` boolean + `explanation` string |
| `domain/model/WorkflowId.java` | Typed value object wrapping a UUID string — avoids primitive obsession |
| `domain/model/PolicyId.java` | Same pattern for policy identifiers |
| `domain/model/Transition.java` | Value object: `fromState`, `toState`, optional `policyRef` |
| `domain/port/WorkflowCommandPort.java` | Outbound port interface: `save(WorkflowDefinition)` |
| `domain/port/WorkflowQueryPort.java` | Outbound port interface: `findById`, `findByIdAndVersion`, `findAll` |
| `domain/port/PolicyCommandPort.java` | Outbound port interface for policy writes |
| `domain/port/PolicyQueryPort.java` | Outbound port interface for policy reads |
| `domain/service/WorkflowService.java` | Domain service: create and publish workflows, emit Micrometer counters |
| `domain/service/PolicyService.java` | Domain service: create and publish policies |
| `domain/service/PolicyEvaluator.java` | Evaluates all policy conditions against a context map; AND semantics; case-insensitive strings |
| `domain/exception/EntityNotFoundException.java` | Domain exception mapped to 404 in both GraphQL and REST handlers |
| `api/graphql/WorkflowGraphQLController.java` | Spring GraphQL controller: `@QueryMapping`, `@MutationMapping` for workflow operations |
| `api/graphql/PolicyGraphQLController.java` | Spring GraphQL controller for policy operations |
| `api/graphql/GraphQLExceptionHandler.java` | Translates domain exceptions to GraphQL errors |
| `api/graphql/dto/*` | GraphQL response DTOs (separate from domain objects) |
| `api/graphql/input/*` | GraphQL input types for mutations |
| `api/rest/dto/*` | REST request/response DTOs |
| `api/rest/exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` mapping domain exceptions to HTTP status codes |
| `api/rest/mapper/DtoMapper.java` | Converts domain ↔ REST DTOs |
| `grpc/v1/DefinitionGrpcService.java` | gRPC server implementation: `getWorkflow`, `getPolicy`, `evaluatePolicy` |
| `grpc/JwtGrpcInterceptor.java` | Validates JWT on every incoming gRPC call |
| `infra/adapter/JpaWorkflowCommandAdapter.java` | Port implementation: maps domain → JPA entity, calls repository |
| `infra/adapter/JpaWorkflowQueryAdapter.java` | Port implementation: queries JPA repository, maps entity → domain |
| `infra/adapter/JpaPolicyCommandAdapter.java` | Same pattern for policy writes |
| `infra/adapter/JpaPolicyQueryAdapter.java` | Same pattern for policy reads |
| `infra/adapter/DomainMapper.java` | Converts between JPA entities and domain objects |
| `infra/jpa/WorkflowJpaEntity.java` | `@Entity` with composite key (id + version), JSON column for transitions |
| `infra/jpa/PolicyJpaEntity.java` | `@Entity` for policy definition |
| `infra/jpa/WorkflowJpaRepository.java` | Spring Data JPA repository |
| `infra/jpa/PolicyJpaRepository.java` | Spring Data JPA repository |
| `infra/jpa/converter/TransitionListConverter.java` | JPA `AttributeConverter`: serialises `List<TransitionDto>` to JSON string |
| `infra/jpa/converter/StringListConverter.java` | Converts `List<String>` to comma-separated column value |
| `config/SecurityConfig.java` | Spring Security: stateless JWT resource server, role-based endpoint access |
| `config/JwtConfig.java` | JWT decoder bean configured from `application.yml` |
| `config/AudienceValidator.java` | Custom OAuth2 token validator checking the `aud` claim |
| `config/CorsConfig.java` | CORS configuration bean |
| `config/ServiceConfiguration.java` | Spring `@Configuration` wiring domain services with their ports |
| `observability/CorrelationIdFilter.java` | Servlet filter: reads/generates `X-Correlation-ID`, writes to MDC |
| `observability/GrpcMetricsInterceptor.java` | Adds Micrometer counters to every gRPC server call |
| `observability/GrpcTracingInterceptor.java` | Propagates OTel trace context through gRPC metadata |

### `services/execution-service/`
| File | Purpose |
|---|---|
| `build.gradle.kts` | Gradle build: Kotlin 2.2.20, Ktor 3.0.0, Exposed, gRPC client, Caffeine, Lettuce, Micrometer, OTel |
| `Application.kt` | Ktor entry point: installs plugins, configures routing, initialises OTel, starts Netty server |
| `domain/Execution.kt` | Kotlin data class: id, workflowId, workflowVersion, currentState, status, input map, timestamps |
| `domain/ExecutionId.kt` | Typed value wrapper for execution identifier |
| `domain/ExecutionStatus.kt` | Sealed class: `Running`, `Completed`, `Failed` with `toStorageString` / `fromStorageString` |
| `domain/AuditEvent.kt` | Data class representing a single audit record |
| `domain/AuditEventType.kt` | Enum: `EXECUTION_STARTED`, `STATE_TRANSITION`, `POLICY_EVALUATED`, `EXECUTION_COMPLETED` |
| `domain/ExecutionRepository.kt` | Repository interface: `save`, `saveWithAudit`, `findById`, `findAll`, `update`, `updateWithAudit` |
| `domain/AuditRepository.kt` | Repository interface: `append(AuditEvent)`, `findByExecutionId` |
| `domain/ExecutionEngine.kt` | Core domain service: start/advance executions, gRPC calls, audit event emission, OTel spans |
| `domain/ExecutionNotFoundException.kt` | Domain exception for missing execution IDs |
| `domain/StateTransitionRequest.kt` | Value object: executionId, targetState, actor, correlationId |
| `domain/PolicyEvaluationResult.kt` | Value object: allowed boolean + explanation |
| `cache/CacheService.kt` | Two-level cache: Caffeine (L1) + Redis (L2) for workflow definition lookups |
| `grpc/DefinitionGrpcClient.kt` | gRPC client with retry, circuit breaker, OTel tracing, auth header propagation |
| `grpc/DefinitionGrpcClientFactory.kt` | Factory creating `DefinitionGrpcClient` instances from auth context |
| `grpc/GrpcDtos.kt` | Kotlin data classes mirroring proto types: `WorkflowDto`, `PolicyDto`, `TransitionDto`, `PolicyConditionDto`, `PolicyEvaluationResultDto` |
| `grpc/AuthGrpcClientInterceptor.kt` | Attaches `Authorization` header to outbound gRPC calls |
| `grpc/GrpcJwtClientInterceptor.kt` | Alternative JWT propagation interceptor for gRPC metadata |
| `http/GraphQLRoutes.kt` | Ktor routing: accepts GraphQL POST, delegates to `GraphQLProvider` |
| `http/ExecuteRoutes.kt` | Ktor routing: `GET /execute/{id}` — fetches workflow definition via gRPC and returns summary |
| `graphql/GraphQLProvider.kt` | Builds the `graphql-java` schema and data fetchers; resolves execution queries and mutations |
| `graphql/GraphQLContext.kt` | Carries auth context + authHeader into GraphQL resolver calls |
| `infra/db/Executions.kt` | Exposed Table DSL: column definitions for `executions` table |
| `infra/db/AuditEvents.kt` | Exposed Table DSL: column definitions for `audit_events` table |
| `infra/db/ExposedExecutionRepository.kt` | Exposed implementation of `ExecutionRepository` using suspended transactions |
| `infra/db/ExposedAuditRepository.kt` | Exposed implementation of `AuditRepository` |
| `security/AuthModule.kt` | Installs three Ktor JWT auth configurations: `auth-jwt`, `auth-viewer`, `auth-executor` |
| `security/JwtConfig.kt` | Builds JWT verifier from application config or dev defaults |
| `security/AuthContext.kt` | Data class holding extracted JWT claims: subject, email, roles |
| `security/AuthContextPlugin.kt` | Ktor plugin extracting `AuthContext` from validated JWT and storing in call attributes |
| `security/JwtPrincipalExtensions.kt` | Extension functions on `JWTPrincipal` for clean claim extraction |
| `observability/Tracing.kt` | Initialises OTel SDK with OTLP gRPC exporter and W3C trace context propagator |
| `observability/ExecutionMetrics.kt` | Micrometer counters and timers for execution lifecycle and cache metrics |
| `observability/HealthService.kt` | Checks database and gRPC channel connectivity for readiness probe |
| `observability/MetricsRoutes.kt` | Ktor routes exposing `/health`, `/metrics` (Prometheus scrape format) |
| `observability/HttpTracingPlugin.kt` | Ktor plugin creating OTel spans for every HTTP request |
| `observability/CorrelationIdPlugin.kt` | Ktor plugin managing `X-Correlation-ID` header and MDC context |

### `web/`
| File | Purpose |
|---|---|
| `app/layout.tsx` | Root React layout: installs Chakra UI provider, font, global styles |
| `app/page.tsx` | Dashboard page: server component shell + `DashboardClient` for dynamic data |
| `app/login/` | Login page with Google Sign-In button and optional dev login form |
| `app/workflows/page.tsx` | Workflow list page |
| `app/workflows/new/` | Create workflow form page |
| `app/workflows/[id]/` | Workflow detail/execution page |
| `app/policies/` | Policy list and create pages |
| `app/executions/` | Execution list and detail pages |
| `app/api/auth/google/route.ts` | Verifies Google ID token, mints platform JWT, sets cookie |
| `app/api/auth/token/route.ts` | Returns current token from cookie (used by client components) |
| `app/api/auth/refresh/route.ts` | Refreshes the platform JWT before expiry |
| `app/api/auth/logout/route.ts` | Clears the auth cookie |
| `app/api/definition-graphql/route.ts` | Proxy: forwards GraphQL to definition-service with bearer token |
| `app/api/execution-graphql/route.ts` | Proxy: forwards GraphQL to execution-service with bearer token |
| `app/globals.css` | Global CSS baseline |
| `components/Layout.tsx` | Shell layout: Navigation + main content area |
| `components/Navigation.tsx` | Sidebar navigation with links to workflows, policies, executions |
| `components/DashboardClient.tsx` | Client component: fetches and displays workflow/execution summary |
| `components/DefinitionCard.tsx` | Reusable card component for workflow/policy list items |
| `components/StartExecutionModal.tsx` | Modal form for providing execution input and starting a workflow |
| `components/ProtectedRoute.tsx` | Client-side JWT check; redirects to login if token missing or expired |
| `components/Providers.tsx` | Wraps children with `QueryClientProvider` (react-query) and Redux `Provider` |
| `components/ErrorBoundary.tsx` | React error boundary catching render-time exceptions |
| `components/TokenSetter.tsx` | Client component that reads JWT from cookie and stores in Redux on mount |
| `components/UI.tsx` | Shared micro-components (buttons, badges, spinners) built on Chakra primitives |
| `components/ui/` | Additional Chakra-based UI component variants |
| `middleware.ts` | Next.js edge middleware: checks `auctor.auth.token` cookie, redirects unauthenticated GETs |
| `graphql/` | GraphQL query/mutation string definitions used by `graphql-request` |
| `hooks/` | Custom React hooks (e.g., `useExecutions`, `useWorkflows`) wrapping react-query calls |
| `lib/jwt-server.ts` | Server-side JWT signing utility using `jose` |
| `lib/admins.ts` | Email allowlist for ADMIN role assignment |
| `store/` | Redux Toolkit slices and store configuration |
| `resources/` | Static assets (icons, images) |
| `next.config.mjs` | Next.js config: rewrites, environment variable exposure |
| `tailwind.config.js` | Tailwind CSS configuration |
| `.env.example` | Template for local environment variables (never committed with real values) |
| `jest.config.mjs` | Jest configuration: jsdom environment, module aliases |
| `jest.setup.ts` | Jest global setup: `@testing-library/jest-dom` matchers |
| `test/` | Jest unit tests for components and API routes |

### `infra/`
| File | Purpose |
|---|---|
| `terraform/azure/main.tf` | Azure resources: AKS, ACR, PostgreSQL Flexible Server, Redis Cache, Key Vault |
| `terraform/azure/variables.tf` | Input variable declarations with descriptions and defaults |
| `terraform/azure/outputs.tf` | Output values: AKS credentials, ACR login server, Key Vault URI |
| `terraform/azure/backend.tf` | Remote state backend configuration (Azure Storage) |
| `terraform/azure/terraform.tfvars.example` | Example variable values for bootstrapping a new environment |
| `helm/Chart.yaml` | Helm chart metadata: name `auctor`, version 1.0.0 |
| `helm/values.yaml` | Default Helm values |
| `helm/values-dev.yaml` | Dev environment overrides |
| `helm/values-sit.yaml` | SIT environment overrides |
| `helm/templates/` | Kubernetes manifest templates: Deployment, Service, Ingress, ConfigMap, Secret |
| `argocd/project.yaml` | Argo CD Project: source repos, destination namespaces, allowed resource kinds |
| `argocd/application-dev.yaml` | Argo CD Application manifest for dev namespace |
| `argocd/application-sit.yaml` | Argo CD Application manifest for SIT namespace |
| `monitoring/prometheus.yml` | Prometheus scrape configuration targeting both services' `/actuator/prometheus` and `/metrics` |
| `monitoring/prometheus-alerts.yml` | Alerting rules: high execution failure rate, service down |
| `monitoring/otel-collector-config.yaml` | OTel collector pipeline: OTLP receiver → Jaeger exporter |
| `monitoring/grafana/` | Grafana dashboard JSON provisioning for execution metrics and service health |
| `docker/` | Service-specific Dockerfile helpers and build configs |

### `.github/workflows/`
| File | Purpose |
|---|---|
| `ci-definition-service.yml` | Maven build + unit test on changes to definition-service |
| `ci-execution-service.yml` | Gradle build + unit test on changes to execution-service |
| `ci-web.yml` | npm install + lint + Jest + Next.js build on web changes |
| `backend-java.yml` | Shared reusable Java CI job |
| `backend-kotlin.yml` | Shared reusable Kotlin/Gradle CI job |
| `frontend.yml` | Shared reusable Next.js CI job |
| `docker.yml` | Docker build + ACR push workflow |
| `deploy-sit.yml` | Helm upgrade to SIT environment |
| `ci-infra.yml` | `terraform validate` on infra changes |
| `security-scan.yml` | Dependency vulnerability scanning |
| `dependency-review.yml` | GitHub Dependency Review Action on PRs |
| `workflow-lint.yml` | Lints CI YAML files for syntax errors |

---

*Last updated: generated from repository source — always verify answers against actual code before interviews.*
