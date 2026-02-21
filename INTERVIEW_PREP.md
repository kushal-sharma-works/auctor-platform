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
13. [Testing Strategy & Test Infrastructure](#13-testing-strategy--test-infrastructure)
14. [Versioning, Schema & Data Integrity](#14-versioning-schema--data-integrity)
15. [Runtime Behaviour & Edge Cases](#15-runtime-behaviour--edge-cases)
16. [File-by-File Reference Index](#16-file-by-file-reference-index)

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

> **Simplified:** One repo = one atomic commit for cross-service changes. Each service still has its own CI job so builds stay fast.

**Q: Why did you pick two different backend languages?**

Intentional. The **definition-service** manages long-lived, transactional domain objects (workflow and policy definitions) where a rich Spring ecosystem — JPA, Flyway, Spring Security, Spring GraphQL — handles heavy lifting quickly and reliably. Java 21 with virtual threads means the IO-bound Spring stack can handle high concurrency without a complex reactive chain. The **execution-service** is a lightweight, coroutine-native Ktor service optimised for throughput; Kotlin's coroutines and structured concurrency map naturally to async database operations and parallel gRPC calls without callback hell. Using two languages is a deliberate trade-off, not a mistake — each service plays to its language's strengths.

> **Simplified:** definition-service = lots of Spring ecosystem features → Java/Spring makes that easy. execution-service = lightweight, async runtime → Kotlin/Ktor is simpler and faster for that. Each language earns its place.

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

> **Simplified:** GraphQL = flexible, ask for only what you need (good for UI). gRPC = typed binary contract, fast binary serialisation (good for internal service calls where performance and strict contract matter).

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

> **Simplified:** Virtual threads = cheap threads that can block on IO without wasting OS threads. You get the concurrency benefit of reactive programming without rewriting everything in reactive style. Spring Boot 3.4 = latest stable LTS release with all modern Java 21 features.

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

> **Simplified:** The domain (business rules) sits in the centre and knows nothing about Spring, JPA, or HTTP. Everything outside the domain talks to it through interfaces ("ports"). Want to swap PostgreSQL for MongoDB? Write a new adapter. Domain code never changes. Tests are trivial because you just mock the port interfaces — no database needed.

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

> **Simplified:** Think of `sealed` as "only these two classes are allowed to implement this interface — nothing else, ever." That guarantee lets the compiler tell you when you have an unhandled case, like a compiler-enforced exhaustive switch. If you add `Archived` and forget a case somewhere, the build fails — not a runtime `NullPointerException` at 3am.

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

> **Simplified:** Micrometer is to metrics what SLF4J is to logging — an abstraction layer. Your code just calls `counter.increment()`. Whether that goes to Prometheus, Datadog or CloudWatch is a one-line config change. No code rewrites if you switch monitoring backends.

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

> **Simplified:** Imagine coroutines as a parent-child tree. If a child fails, the parent is notified and cancels other children. `CancellationException` is the signal that travels up this tree. If you accidentally swallow it in a `catch (e: Exception)` block, the parent never knows the child was cancelled — timeouts silently stop working. That's why `retryWithBackoff` explicitly re-throws it.

**Q: Explain the circuit breaker in `DefinitionGrpcClient`.**

Two `AtomicLong`/`AtomicInteger` counters track consecutive failures and when the circuit should reopen. If `consecutiveFailures >= 5`, the circuit opens for 30 seconds (`circuitOpenUntil` is set to `now + 30_000ms`). Subsequent calls throw immediately without hitting gRPC. On the first success, both counters reset. This pattern is a manual implementation of the Circuit Breaker resilience pattern (as described in Release It! by Michael Nygard). An atomic compare-and-swap would be preferable in very high-concurrency scenarios, but the current implementation uses `AtomicInteger.incrementAndGet()` and `AtomicLong.set()` which are thread-safe.

> **Simplified:** Like a real circuit breaker in your house — if too many failures happen (5 in a row), the circuit "trips" and all subsequent calls fail instantly for 30 seconds. This protects definition-service from being hammered with requests while it's recovering. After 30 seconds the circuit "resets" and tries again. `Atomic*` types ensure the counter and timer are read/written safely across multiple threads without locks.

**Q: How does the two-level cache work?**

`CacheService.getWorkflowCached()` implements a standard read-through, write-through L1/L2 cache:

1. **L1 (Caffeine)**: in-process, max 10,000 entries, 5-minute TTL. Sub-microsecond lookup, no network IO.
2. **L2 (Redis)**: shared across all instances of execution-service, 5-minute TTL. Used for warm-up after JVM restarts.
3. **L3 (gRPC)**: actual definition-service call if both caches miss.

On a cache miss, the loaded value is written to Redis (`SETEX` with TTL), then to Caffeine. Redis operations use `Dispatchers.IO` to avoid blocking the coroutine dispatcher. Redis failures are caught and logged as warnings rather than propagated — the cache is a performance optimisation, not a correctness requirement.

> **Simplified:** Three-tier lookup: (1) Check local memory first (Caffeine, nanoseconds). (2) Check shared Redis (milliseconds, shared between all running pods). (3) If both miss, call definition-service via gRPC (tens of milliseconds). On a cache miss, fill both caches on the way back. Redis failing degrades to "always hit gRPC" — correctness is never affected, only performance.

**Q: Why is the GraphQL server in execution-service implemented with graphql-java directly rather than a framework?**

The Ktor ecosystem does not have a mature opinionated GraphQL plugin comparable to Spring GraphQL. Using `graphql-java` directly (`GraphQLProvider.kt`, `GraphQLRoutes.kt`) is intentional — it keeps the schema definition explicit (loaded from a `.graphqls` file), the resolver wiring transparent, and adds no magic. It also avoids adding a second heavy framework.

**Q: How does the execution-service authenticate requests?**

`AuthModule.kt` installs three `jwt()` authentication configurations on Ktor: `auth-jwt` (any valid JWT), `auth-viewer` (ADMIN or VIEWER roles), `auth-executor` (ADMIN or EXECUTOR roles). Routes are wrapped with `authenticate("auth-executor")` or `authenticate("auth-viewer")` as appropriate. The JWT validator uses the same secret/issuer/audience configuration as definition-service, so both services accept the same platform JWT minted by the web layer.

`AuthContextPlugin.kt` extracts claims from the validated JWT and stores them in `call.attributes` so route handlers can call `call.authContextOrNull()` to get the current actor's email and roles without re-parsing the token.

**Q: How does the audit trail work?**

Every significant event writes an `AuditEvent` record to the `audit_events` table via `ExposedAuditRepository`. Events are append-only (there is no `update` or `delete` on audit records). The event types are `EXECUTION_STARTED`, `STATE_TRANSITION`, `POLICY_EVALUATED`, `EXECUTION_COMPLETED`. Each event carries a `correlationId` (a UUID generated per web request), the `actor` (the user's email from the JWT), timestamps, and optional policy evaluation results and explanations. This provides a deterministic replay record.

**Q: Why Exposed and not JPA for execution-service?**

Exposed is a Kotlin-first SQL DSL and DAO library from JetBrains. It integrates naturally with coroutines via `newSuspendedTransaction(Dispatchers.IO) { }`. JPA's session scoping (EntityManager lifecycle) does not map cleanly to coroutine context; using Exposed avoids subtle bugs around session/transaction boundaries when coroutines switch threads. Exposed also gives finer-grained control over SQL generation, which matters for audit trail writes that need predictable behaviour under load.

> **Simplified:** JPA tracks which objects you've loaded (the "session"), but coroutines can switch between threads mid-execution. JPA doesn't know about coroutines, so the session can get confused about which thread "owns" it. Exposed has no hidden session object — you write SQL DSL and it runs it, making it safe and predictable in a coroutine context.

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

> **Simplified:** When execution-service calls definition-service over gRPC, it automatically inserts a `traceparent` header containing the current trace ID. definition-service picks that up and creates a child span under the same trace ID. Jaeger then shows all hops (web → execution → definition) as one connected trace tree rather than three separate, unrelated traces.

**Q: What is the `CorrelationIdFilter` and why is it different from the trace ID?**

`CorrelationIdFilter.java` in definition-service (and `CorrelationIdPlugin.kt` in execution-service) manages a business-level correlation ID. The trace ID is an OTel concern used for distributed tracing infrastructure. The correlation ID is an application-level concept passed in the `X-Correlation-ID` header that can be used to correlate log entries across services even if tracing infrastructure is absent. If an OTel trace is active, the filter uses the trace ID as the correlation ID for consistency; otherwise it generates a UUID.

> **Simplified:** Trace ID lives in the OTel/Jaeger world — you need Jaeger running to use it. Correlation ID lives in logs — it's just a string in every log line so you can `grep` for it even in plain log files with no tracing stack. When OTel is active, the two are set to the same value so they always match. Think of correlation ID as the "poor man's" trace ID that works everywhere.

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

> **Simplified:** One `terraform apply` creates the whole Azure infrastructure: a Kubernetes cluster (AKS), a container registry (ACR), a managed PostgreSQL with two databases, a Redis cache, and a Key Vault for secrets. AKS is wired to pull images from ACR automatically via an IAM role assignment — no passwords stored anywhere.

**Q: What does the Helm chart do?**

`infra/helm/` is a single Helm chart (`auctor`) with three deployments: definition-service, execution-service, and web. Values files per environment (`values-dev.yaml`, `values-sit.yaml`) override image tags, replica counts, resource limits, and environment-specific secrets (referenced from Kubernetes secrets, not hardcoded). The chart uses a unified deployment template in `templates/`.

**Q: How does GitOps work with Argo CD?**

`infra/argocd/project.yaml` defines the Argo CD project with `sourceRepos` pointing to this repo and `destinations` pointing to the AKS namespaces. `application-dev.yaml` and `application-sit.yaml` are Argo CD Application manifests that point to the Helm chart path in this repo. When a CI pipeline builds and pushes a new image, it updates the image tag in the values file, commits it back to the repo, and Argo CD syncs the cluster to match the desired state — no manual `kubectl apply` needed.

> **Simplified:** Argo CD watches this Git repo. When you push a new Docker image, the CI pipeline updates the image tag in `values-dev.yaml` and commits it. Argo CD sees the git change and automatically updates Kubernetes to run the new image. No human runs `kubectl` commands — Git is the single source of truth for what's deployed.

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

> **Simplified:** Kotlin coroutines run on a small shared thread pool. JDBC blocks the thread it runs on. If you run JDBC directly on the coroutine pool, all requests queue up behind the blocked thread. By switching to `Dispatchers.IO` (a larger pool for blocking work), you isolate the blocking JDBC call so the coroutine pool stays free to handle new requests.

**Q: Why does `retryWithBackoff` use `1 shl (attempt - 1)` for delays?**

`1 shl n` is Kotlin's bitwise left-shift, equivalent to `2^n`. For `attempt = 1, 2, 3`: delays are `100 * 1 = 100ms`, `100 * 2 = 200ms`, `100 * 4 = 400ms`. This is exponential backoff with a base delay of 100ms. The pattern reduces thundering herd when definition-service restarts and all execution-service instances retry simultaneously.

> **Simplified:** Each retry waits twice as long as the previous one (100ms → 200ms → 400ms). This spreads out retries so all execution-service pods don't hammer definition-service at the exact same millisecond when it restarts. `1 shl n` is just a compact way to compute powers of 2 (`2^0=1`, `2^1=2`, `2^2=4`).

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

## 13. Testing Strategy & Test Infrastructure

**Q: What kinds of tests exist in this project?**

Three tiers:

| Tier | Where | Tool | Runs in CI? |
|---|---|---|---|
| Unit tests | definition-service, execution-service | JUnit 5 + Mockito / Kotlin test | Yes (default) |
| Integration tests (DB) | definition-service (`*IT.java`), execution-service (`ExposedRepositoryIT.kt`) | Testcontainers (PostgreSQL) / H2 in-memory | Skipped by default (`skipITs=true`) |
| Frontend tests | web | Jest + React Testing Library | Yes |

Unit tests exercise domain logic with mock ports, no database or network. Integration tests (`IntegrationTestBase.java` in definition-service) spin up a real PostgreSQL 15 container via Testcontainers. The execution-service IT uses H2 in PostgreSQL-compatibility mode — simpler to run without Docker.

> **Simplified:** Fast unit tests (no database, instant) run on every commit. Slow integration tests (real Postgres or H2) exist but are excluded from default CI to keep builds fast. You can run them explicitly with `mvn verify -DskipITs=false` or `./gradlew integrationTest`.

**Q: Why Testcontainers for definition-service integration tests but H2 for execution-service?**

definition-service uses JPA/Hibernate with PostgreSQL-specific JSONB columns and Flyway migrations. Testing against a real PostgreSQL via Testcontainers is the only way to verify that JSONB serialisation, Flyway DDL, and Hibernate dialect work correctly together. Execution-service uses Exposed with simpler column types that H2 handles in PostgreSQL-compatibility mode, making H2 sufficient and removing the Docker dependency for those tests.

> **Simplified:** JSONB columns only exist in PostgreSQL — H2 can't fully simulate them. For definition-service we need the real thing. Execution-service has simpler schema (no JSONB), so H2 is good enough and is faster.

**Q: How are tests isolated from the production security config?**

`SecurityConfig.java` carries `@Profile("!test")` — it is not loaded when the `test` Spring profile is active. The `IntegrationTestBase` class activates `@ActiveProfiles("test")` and imports `TestSecurityConfig.class`, which permits all requests without JWT validation. This ensures integration tests can exercise the full API layer without needing to generate valid JWTs.

> **Simplified:** The real JWT validator would reject every test request. So in the `test` profile, the security config is swapped out for one that permits all requests. It's like a "bypass mode" for tests only.

**Q: How is the gRPC client tested without a real server?**

`DefinitionGrpcClientTest.kt` uses gRPC's `InProcessServerBuilder` and `InProcessChannelBuilder` to spin up an in-process mock server (`MockDefinitionService`) that listens on a random name rather than a network port. The test client connects to it via the in-process channel. This avoids network IO, making gRPC tests fast, deterministic, and runnable without any external services. The mock server simulates success, `NOT_FOUND`, `UNAVAILABLE` (to test retry), and `DEADLINE_EXCEEDED` scenarios.

> **Simplified:** Instead of a real TCP connection, the client and server share the same JVM process memory. It looks like a real gRPC call to both sides but is completely in-memory. The mock server can be told to return errors to test the retry and circuit breaker logic.

**Q: How are domain-only tests written without a database?**

`WorkflowServiceTest.java` and `PolicyServiceTest.java` use `@ExtendWith(MockitoExtension.class)` to inject mocks for `WorkflowCommandPort` and `WorkflowQueryPort`. Because the domain service depends on port interfaces (not JPA repositories), Mockito can stub `commandPort.save()` to return a predefined result. No Spring context, no database, near-zero test startup time.

> **Simplified:** Domain services talk to interfaces. Tests give those interfaces a mock that returns canned data. The whole test takes milliseconds because there's no Spring startup or database involved.

---

## 14. Versioning, Schema & Data Integrity

**Q: How does workflow versioning actually work at the database level?**

The `workflow_definitions` table has a **composite primary key** of `(id, version)`. When you publish a workflow:
1. The existing `(id, 1)` row keeps its `DRAFT` status permanently.
2. A brand-new row `(id, 2)` is inserted with status `PUBLISHED`.

There is no `UPDATE` of the existing row — you always INSERT a new version. This means every version ever created is preserved in the database forever. The gRPC `GetWorkflow` call passes `version=0` to mean "latest" (execution-service always pins to the version it started with).

> **Simplified:** It's like Git commits — you never rewrite history. Publishing a workflow adds a new row (v2) alongside the old one (v1). An execution started on v1 will always fetch v1, even after v2 and v3 exist. This is how deterministic replay works: an execution's output is reproducible because its workflow definition is immutable.

**Q: What is the `jpa_version` column for?**

The `@Version` annotation on `WorkflowJpaEntity.jpaVersion` enables JPA **optimistic locking**. When you save an entity, Hibernate includes `WHERE jpa_version = ?` in the `UPDATE` statement. If another thread has already incremented the version, the update matches 0 rows and Hibernate throws `OptimisticLockException`. For definition-service this is a safety net against concurrent publish operations on the same workflow, not the primary concurrency control.

> **Simplified:** Before updating, Hibernate checks "has anyone else changed this record while I was working?". If yes, it throws an error instead of silently overwriting. Think of it as a "conflict detector" for concurrent edits.

**Q: What does the `audit_events` schema look like and why are there indexes?**

```sql
CREATE TABLE audit_events (
    id VARCHAR(64) PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    from_state VARCHAR(100), to_state VARCHAR(100),
    policy_id VARCHAR(100), policy_result BOOLEAN,
    explanation TEXT,
    actor VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(36) NOT NULL
);
CREATE INDEX idx_audit_events_execution_id ON audit_events(execution_id);
CREATE INDEX idx_audit_events_timestamp ON audit_events(timestamp);
CREATE INDEX idx_audit_events_execution_timestamp ON audit_events(execution_id, timestamp);
```
The composite index on `(execution_id, timestamp)` covers the primary query pattern: "give me all events for execution X in chronological order". Single-column indexes on each field support ad-hoc queries (all recent events, all events for a policy).

> **Simplified:** The most common query is "show the audit trail for execution X, sorted by time." The composite index `(execution_id, timestamp)` means PostgreSQL can answer that query with a single index scan — no full table scan, no sort step.

**Q: What does the `executions` table look like and how is `ExecutionStatus.Failed` stored?**

```sql
CREATE TABLE executions (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(36) NOT NULL,
    workflow_version INTEGER NOT NULL,
    current_state VARCHAR(100) NOT NULL,
    status VARCHAR(255) NOT NULL,
    input TEXT NOT NULL,
    ...
);
```
`ExecutionStatus` is a Kotlin sealed class. `toStorageString()` converts it:
- `Running` → `"RUNNING"`
- `Completed` → `"COMPLETED"`
- `Failed("Network timeout")` → `"FAILED:<Base64-encoded reason>"`
- `Suspended` → `"SUSPENDED"`

The `reason` string is Base64-encoded to avoid SQL injection and character-encoding issues when embedding arbitrary strings in a VARCHAR column.

> **Simplified:** When an execution fails, the failure reason (potentially arbitrary text) is stored as e.g. `FAILED:TmV0d29yayB0aW1lb3V0` (Base64 for "Network timeout"). On read, the prefix `FAILED:` is stripped and the rest is decoded back to the original reason string. This avoids issues with special characters, quotes, or multi-byte strings in the reason message.

**Q: What does the `workflow_definitions` schema look like and why JSONB for states and transitions?**

```sql
CREATE TABLE workflow_definitions (
    id VARCHAR(36) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    states JSONB NOT NULL,
    initial_state VARCHAR(100) NOT NULL,
    transitions JSONB NOT NULL DEFAULT '[]',
    jpa_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id, version)
);
```
`states` is a `JSONB` array of strings (e.g. `["pending","approved","rejected"]`). `transitions` is a `JSONB` array of objects. JSONB in PostgreSQL is binary-stored JSON with indexing support, unlike `TEXT` JSON which is stored as a raw string. PostgreSQL can query inside JSONB fields without parsing the whole column value. The JPA `@Convert(converter = StringListConverter.class)` and `TransitionListConverter` handle serialisation/deserialisation.

> **Simplified:** States and transitions are arrays that vary in length per workflow. Storing them as JSONB keeps the schema simple (no extra tables, no foreign keys) while PostgreSQL still stores them efficiently as binary for fast reads. The JPA converter auto-serialises/deserialises them so the Java code works with typed `List<String>` and `List<Transition>`.

**Q: Why does `gRPC getWorkflow` treat `version=0` as "latest version"?**

In the gRPC `GetWorkflowRequest`, `version` is an `int32`. Proto3 default value for integers is `0`. So if execution-service sends `version=0` (or omits it), definition-service's `DefinitionGrpcService.getWorkflow()` checks `request.getVersion() > 0` — if false, it calls `workflowService.getById(id)` which fetches the latest. This is a proto3-idiomatic way to express "not specified" for non-optional primitives without wrapping types.

> **Simplified:** Proto3 integers default to 0 when not set. The code treats 0 as "give me the latest version" — a convention to avoid sending a special optional wrapper just to express "no version preference." If version > 0, it fetches that exact version.

---

## 15. Runtime Behaviour & Edge Cases

**Q: What does `ExecutionStatus.Suspended` mean and when is it used?**

`ExecutionStatus` is a sealed class with four states: `Running`, `Completed`, `Failed(reason)`, and `Suspended`. `Suspended` represents a paused execution — one that has not terminated but is temporarily halted (e.g., awaiting external input or a manual review step). The `ExecutionEngine` currently checks for `Completed` or `Failed` before allowing advancement but does not check `Suspended`. `Suspended` is modelled in the domain and stored correctly but the trigger to enter/exit it is not yet implemented — it is a forward-looking status ready for when manual hold/resume functionality is added.

> **Simplified:** `Suspended` is the "paused" state. The data model and storage support it now. The business logic to pause and resume an execution hasn't been wired up yet — it's a deliberate placeholder for a future feature, not a bug.

**Q: What happens if definition-service is completely unreachable when execution-service starts up?**

`Application.kt` attempts to create `CacheService` inside a `try/catch`:
```kotlin
try {
    CacheService(actualGrpcClient, ...)
} catch (e: Exception) {
    logger.warn("Failed to connect to Redis, continuing without cache", e)
    null
}
```
A `null` cache means execution-service falls through to direct gRPC on every lookup. The gRPC client has its own timeout (`callDeadlineMs=5000`) and retry logic (3 attempts with exponential backoff). If definition-service is still unreachable after retries, `startExecution` / `advanceExecution` will throw and return a `CONFLICT` (500-equivalent) error to the caller. The circuit breaker will open after 5 consecutive failures, returning fast errors for 30 seconds before retrying. The service stays up and healthy; only execution operations fail.

> **Simplified:** Redis failure → execution-service keeps running, just slower. definition-service failure → execution-service keeps running but execution operations fail with clear errors until definition-service recovers. The circuit breaker prevents a flood of retries making things worse.

**Q: How does the Ktor `StatusPages` plugin map exceptions to HTTP responses?**

`Application.kt` installs `StatusPages` with explicit exception handlers:
- `ExecutionNotFoundException` → 404 Not Found
- `IllegalArgumentException` → 400 Bad Request
- `IllegalStateException` → 409 Conflict (e.g., "Execution already in terminal state", "No allowed transitions")
- `CancellationException` → **must be re-thrown**, not mapped to HTTP
- Any other `Throwable` → 500 Internal Server Error

This means callers always get meaningful HTTP status codes instead of a generic 500 for business-rule violations. The `CancellationException` re-throw is critical — if it were mapped to a 500 response, it would break Ktor's request handling lifecycle.

> **Simplified:** Instead of every error returning "500 Internal Server Error," the app explicitly maps business exceptions to the right HTTP codes: "no such execution" → 404, "bad request data" → 400, "can't do that in current state" → 409. This makes the API much easier to use and debug from the client side.

**Q: What is the HikariCP connection pool configuration and why those numbers?**

In `Application.kt`:
```kotlin
maximumPoolSize = 10
minimumIdle = 2
idleTimeout = 600000      // 10 minutes
connectionTimeout = 30000 // 30 seconds
transactionIsolation = "TRANSACTION_READ_COMMITTED"
```
`maximumPoolSize=10` is a reasonable default for a single-service deployment — HikariCP's own benchmarks suggest diminishing returns beyond 10 connections for most workloads. `TRANSACTION_READ_COMMITTED` is PostgreSQL's default isolation level and sufficient for execution-service's write patterns (no phantom read issues since executions are looked up by primary key).

> **Simplified:** HikariCP pre-warms at least 2 connections and keeps up to 10. If all 10 are in use and a new request comes in, it waits up to 30 seconds before failing. `READ_COMMITTED` means each query sees committed data — standard for most web apps. Higher isolation (REPEATABLE_READ, SERIALIZABLE) would be needed only if we needed stronger concurrent write guarantees.

**Q: How does graceful shutdown work?**

`Application.kt` subscribes to Ktor's `ApplicationStopped` lifecycle event:
```kotlin
environment.monitor.subscribe(ApplicationStopped) {
    // parameter 'cacheService' == null means the app created actualCacheService itself
    if (cacheService == null && actualCacheService != null) actualCacheService.close()
    // parameter 'grpcClient' == null means the app created actualGrpcClient itself
    if (grpcClient == null) actualGrpcClient.close()
    // parameter 'dataSource' == null means the app created actualDataSource itself
    if (dataSource == null) actualDataSource.close()
}
```
The function parameters (`grpcClient`, `cacheService`, `dataSource`) are injected by tests. When they are `null`, the app created those resources itself and therefore **owns** them and must close them. Test-injected resources (parameter != null) are owned by the test — the app must not close them. This ensures Redis connections, the gRPC channel, and the HikariCP pool are released when the JVM shuts down. `DefinitionGrpcClient.close()` calls `channel.shutdown().awaitTermination(1, TimeUnit.SECONDS)` — waiting up to 1 second for in-flight gRPC calls to complete.

> **Simplified:** The app closes only the resources it created. If a test injects a mock gRPC client (`grpcClient != null`), the app leaves it alone — the test owns it. If no mock was injected (`grpcClient == null`), the app created the real client and shuts it down cleanly on stop. Without this hook, connections would leak and the next deployment might exhaust the connection pool on startup.

**Q: What does gRPC server reflection do and why is it enabled?**

In `application.yml` for definition-service:
```yaml
grpc:
  server:
    reflection:
      enabled: true
```
gRPC reflection lets clients (like `grpcurl` or Postman's gRPC tool) discover the service's methods and message types at runtime without having the `.proto` file. This enables API exploration and debugging without sharing the proto files. In production this would typically be disabled to avoid leaking the service contract.

> **Simplified:** Without reflection, to call a gRPC service manually you'd need the `.proto` file. With reflection enabled, tools like `grpcurl` can query the server directly: "what methods do you have? what do the messages look like?" Useful for development/debugging, should be turned off in production.

**Q: What is the `VIEWER` role defined in `SecurityConfig.java` and where is it assigned?**

`SecurityConfig.java` permits the `/graphql` endpoint for `VIEWER`, `ADMIN`, and `EXECUTOR` roles:
```java
.requestMatchers("/graphql").hasAnyRole("VIEWER", "ADMIN", "EXECUTOR")
```
However, the current Google login flow (`auth/google/route.ts`) only assigns `EXECUTOR` (and `ADMIN` for admin emails). `VIEWER` is not currently assigned through the login flow — it is a forward-looking role for a future read-only user type (e.g., a stakeholder who can view workflow state but cannot start executions). It is defined in the security config and in `AuthModule.kt`'s `auth-viewer` JWT configuration but has no assignment path today.

> **Simplified:** `VIEWER` is a placeholder role. The code is ready to use it (security checks pass for it), but the login flow doesn't yet assign it to any user. It's pre-built for a "read-only guest" user type that hasn't been fully implemented yet.

**Q: What does the `generate-token.kts` / `generate-token.sh` script do?**

These scripts in the execution-service root generate a valid platform JWT signed with the dev secret, which can then be used directly in `curl` or Postman to test the execution-service API endpoints locally without going through the web Google login flow. The `.sh` script runs the `.kts` Kotlin script. This is a developer convenience tool, not a production artefact.

> **Simplified:** Instead of going through the full Google login UI to get a token, a developer can run this script to generate a valid test token in seconds and paste it into `curl -H "Authorization: Bearer <token>"`.

**Q: What JVM metrics does execution-service automatically publish?**

In `Application.kt`, five standard Micrometer binders are registered:
```kotlin
ClassLoaderMetrics().bindTo(meterRegistry)
JvmMemoryMetrics().bindTo(meterRegistry)
JvmGcMetrics().bindTo(meterRegistry)
JvmThreadMetrics().bindTo(meterRegistry)
ProcessorMetrics().bindTo(meterRegistry)
```
These automatically publish dozens of standard metrics: heap/non-heap memory usage, GC pause durations, thread counts (daemon vs non-daemon), open file descriptors, CPU usage. All are available at the `/metrics` Prometheus endpoint without any additional code.

> **Simplified:** These five lines give you a complete JVM health dashboard for free — memory, garbage collection, CPU, and thread counts — all exported to Prometheus in the standard Micrometer format.

**Q: Walk me through the proto file — what are the three RPC methods?**

```protobuf
service DefinitionService {
  rpc GetWorkflow (GetWorkflowRequest) returns (WorkflowResponse);
  rpc GetPolicy   (GetPolicyRequest)   returns (PolicyResponse);
  rpc EvaluatePolicy (EvaluatePolicyRequest) returns (EvaluatePolicyResponse);
}
```
- **GetWorkflow**: takes `{id, version}`, returns the full workflow graph (states, transitions, policyRefs). `version=0` = fetch latest.
- **GetPolicy**: takes `{id, version}`, returns the policy definition with all conditions.
- **EvaluatePolicy**: takes `{policy_id, policy_version, context: map<string,string>}`, returns `{allowed: bool, explanation: string}`. This is the hot-path called on every state transition.

The `context` field uses proto3's `map<string, string>` type, which serialises to a hash map entry list. Execution-service populates it with the execution's `input` map.

> **Simplified:** Three remote calls: (1) "Get me this workflow definition." (2) "Get me this policy definition." (3) "Run this policy against this context data and tell me allowed/denied." All three are used in `advanceExecution` — fetch workflow, then for each transition call EvaluatePolicy with the execution's input as context.

**Q: What is the difference between `NOT_FOUND` and `INVALID_ARGUMENT` in the gRPC error handling and why are they treated the same way?**

In `DefinitionGrpcClient.kt`:
```kotlin
if (e.status.code == Status.Code.NOT_FOUND || e.status.code == Status.Code.INVALID_ARGUMENT) {
    null // Return null — don't retry
}
```
`NOT_FOUND` means the workflow/policy doesn't exist. `INVALID_ARGUMENT` means the request was malformed (e.g., bad ID format). Both are **non-retryable** — retrying with the same parameters will produce the same error. Only transient errors (like `UNAVAILABLE` or `INTERNAL`) are retried. Returning `null` instead of throwing allows the caller to handle "not found" gracefully (e.g., throw a typed `IllegalArgumentException` with a user-friendly message).

> **Simplified:** If the workflow doesn't exist or you sent a bad request, retrying won't help — it'll fail again for the same reason. Only retry on server errors (network blip, server restarting). Non-retryable errors return `null` so the caller can give a clear error message rather than crashing after 3 pointless retries.

---

## 16. File-by-File Reference Index

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
| `src/test/java/.../IntegrationTestBase.java` | Base class for integration tests: spins up PostgreSQL 15 via Testcontainers, activates `test` profile |
| `src/test/java/.../TestSecurityConfig.java` | `@TestConfiguration` that disables JWT validation so integration tests can call endpoints freely |
| `src/test/java/.../WorkflowServiceTest.java` | Unit test for `WorkflowService`: mocks ports, verifies create/publish/getById logic |
| `src/test/java/.../PolicyEvaluatorTest.java` | Unit test for every policy operator (EQ, GT, IN, etc.) and AND short-circuit logic |
| `src/test/java/.../DefinitionGrpcServiceTest.java` | Unit test for the gRPC server: verifies correct proto mapping for getWorkflow/getPolicy/evaluatePolicy |
| `src/test/java/.../WorkflowRepositoryIT.java` | Integration test: real Postgres, verifies JPA composite-key persistence and query adapters |
| `src/test/resources/application-test.yml` | Test profile config: uses Testcontainers datasource, disables OTel |
| `src/test/resources/application-test-h2.yml` | Alternate test profile config: uses H2 in-memory for fast unit-test-level checks |

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
| `generate-token.kts` / `generate-token.sh` | Developer scripts to generate a valid platform JWT locally without going through the Google login UI |
| `src/test/kotlin/.../ExecutionEngineTest.kt` | Unit tests for `Execution` domain model, `AuditEvent`, `StateTransitionRequest` immutability |
| `src/test/kotlin/.../DefinitionGrpcClientTest.kt` | Tests gRPC client with in-process mock server: success, NOT_FOUND, retry on UNAVAILABLE, timeout |
| `src/test/kotlin/.../ExposedRepositoryIT.kt` | Integration tests with H2 in PostgreSQL mode: save/update/audit atomicity, pagination, status serialisation |
| `src/test/kotlin/.../AuthModuleTest.kt` | Tests JWT auth configurations: valid token passes, wrong role rejected, expired token rejected |
| `src/test/kotlin/.../ExecutionMetricsTest.kt` | Verifies all counters and timers increment correctly |

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
