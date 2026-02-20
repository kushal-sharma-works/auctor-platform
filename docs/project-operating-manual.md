# Auctor Platform — Project Operating Manual

This document is the single, comprehensive reference for how the project is built, why key decisions were made, how it runs, and how to operate it safely for local demos and evaluations.

## 1) What this platform is

Auctor is a policy-driven workflow platform split into two backend services:
- **definition-service**: source of truth for policy and workflow definitions (governance side)
- **execution-service**: runtime engine for starting/advancing executions (operations side)

A web application provides UI/API routing and auth token handling.

Core goals:
- deterministic execution
- strict auditability
- clear separation of governance vs runtime concerns

## 2) Repository structure

Top-level areas:
- `services/definition-service`: Java Spring Boot service for policy/workflow definitions
- `services/execution-service`: Kotlin Ktor service for runtime execution and audit trail
- `web`: Next.js frontend + API routes + auth flow
- `infra/terraform/azure`: infrastructure provisioning templates
- `infra/helm`: Kubernetes deployment chart
- `infra/argocd`: GitOps manifests
- `monitoring`: Prometheus / Grafana / OTel config
- `scripts`: validation and local test runners
- `docs`: architecture, decisions, operations, DR, readiness

## 3) Runtime architecture and request flows

### 3.1 Main data/control path
1. User interacts with `web`
2. `web` calls:
   - definition endpoints for governance data
   - execution GraphQL endpoints for runtime operations
3. `execution-service` calls `definition-service` over gRPC for definition lookups
4. persistence:
   - Postgres for both services (separate logical data concerns)
   - Redis for execution-side caching acceleration

### 3.2 Why split definition and execution
- Keeps policy lifecycle independent from runtime workload
- Reduces accidental coupling between authoring and execution concerns
- Supports stricter release and validation controls for definitions

## 4) Service responsibilities

## 4.1 definition-service
- Manages policies and workflow definitions
- Maintains versioned definition records
- Exposes health/metrics/actuator endpoints
- Validates JWT for secured interactions

## 4.2 execution-service
- Starts and advances executions
- Maintains execution state transitions and audit events
- Uses gRPC definition lookup + Redis cache
- Exposes GraphQL and health/metrics endpoints
- Handles domain-level errors with explicit status mapping to avoid generic 500s

## 4.3 web
- UI for workflow browsing and execution operations
- Google sign-in path and app JWT issuance via API routes
- Proxies API/GraphQL traffic to backend services
- Stores/refreshes session token for browser flows

## 5) Authentication and token model

Authentication layers:
- User identity proof (Google ID token)
- Application JWT issued by backend/web auth route

Why app JWT still exists with Google login:
- Backend services authorize against internal JWT claims (`issuer`, `audience`, `roles`) expected by platform components
- Google token proves identity, app token standardizes authorization contract inside system

## 6) Data and state model

Definition domain:
- immutable-style versioned workflow/policy entities
- publication model allows clear governance states

Execution domain:
- execution lifecycle tracked as explicit state transitions
- audit trail persists transition/evaluation results

This supports replayability and compliance-focused evidence collection.

## 7) DevOps and delivery model

CI/CD highlights:
- service-specific CI workflows (build/test, docker jobs on configured branches)
- local build/test flow for repeatable demos

Infrastructure assets remain in-repo for engineering completeness and future cloud enablement.

## 8) Observability model

Provided components:
- OTel collector wiring
- Prometheus scraping + alerts
- Grafana dashboards
- Jaeger traces

Operational intent:
- request/trace correlation
- service health and runtime performance visibility
- auditable runtime diagnostics

## 9) Local-first operation (recommended for low-cost demos)

Use local mode for recruiter/demo flows:
- `docker compose up --build`
- Web at `http://localhost:3000`
- backend services on `8081`, `8082`

For guided smoke checks:
- `./scripts/test-local.sh`

Why local-first:
- near-zero cloud cost
- repeatable and fast demo reset
- no DNS/ingress complexity required

See: `docs/local-run.md`.

## 10) Local-first cost control

Primary operating mode is local runtime to avoid recurring cloud charges.

Cost control strategy:
1. Use local mode for demos and review sessions
2. Run `docker compose` stack only when needed
3. Stop local stack after each demo session

## 11) Quality posture summary

Platform shows production-minded quality patterns:
- clear service boundaries
- health/readiness/liveness checks
- structured logging and observability stack
- security context defaults and least-privilege direction
- infrastructure-as-code and environment overlays
- documented DR/readiness/operations artifacts

## 12) Decision rationale (condensed)

Key deliberate choices:
- **Monorepo** for synchronized evolution of services + infra + docs
- **gRPC** between runtime and definition for typed internal contracts
- **GraphQL** on execution side for flexible UI query/mutation patterns
- **Versioned definitions** for deterministic replay and governance traceability
- **Redis caching** for runtime lookup performance
- **Helm + Terraform** for repeatable deployment and infra provisioning

## 13) Safe cleanup policy used in this repo

Policy applied for cleanup tasks:
- Do **not** remove infra by default
- Do **not** remove gitignored/generated files unnecessarily
- Remove only explicit user-requested or clearly obsolete tracked artifacts

Applied result in current pass:
- obsolete and stale references are removed from docs
- no broad destructive cleanup beyond explicit scope

## 14) Practical runbooks

### 14.1 Demo runbook (local)
1. Start stack with Docker Compose
2. Validate health endpoints
3. Login and execute a sample workflow
4. Show audit and observability pages
5. Shutdown stack

### 14.2 Local smoke runbook
1. Start stack with `docker compose up --build`
2. Verify service health endpoints
3. Run quick workflow demo in web UI
4. Validate logs and observability endpoints
5. Shutdown stack with `docker compose down -v`

## 15) Risks and constraints to keep visible

- Autoscaling minimums can overcommit small clusters
- Cloud cost can spike quickly if environments are left running
- Token secret consistency across services is mandatory for auth stability

## 16) What this manual is for

Use this file as:
- architecture handoff document
- recruiter/interviewer project walkthrough backbone
- operator quick-reference before demos/deploys
- rationale sheet for why decisions were made

For deeper topic-specific details, also read:
- `docs/architecture.md`
- `docs/decisions.md`
- `docs/local-run.md`
