# Architecture Decision Records

## ADR-001: Monorepo vs Polyrepo
- Status: Accepted
- Decision: Monorepo to align service evolution and shared infra changes.
- Rationale: Faster cross-service refactors and simpler release coordination.

## ADR-002: GraphQL and gRPC Protocol Strategy
- Status: Accepted
- Decision: GraphQL for UI-facing definition/execution operations, gRPC for service-to-service definition lookups.
- Rationale: GraphQL fits UI query/mutation shape flexibility, while gRPC preserves typed low-latency internal calls.

## ADR-003: JPA vs Exposed
- Status: Accepted
- Decision: JPA for definition-service, Exposed for execution-service.
- Rationale: Strong schema mapping and transactional history in definition-service; lower-level control and performance in execution-service.

## ADR-004: Sealed Types for Status Modeling
- Status: Accepted
- Decision: Sealed hierarchies to model statuses.
- Rationale: Compiler-exhaustive handling and clearer intent.

## ADR-005: Append-Only Audit Log
- Status: Accepted
- Decision: Write-only audit records for every execution step.
- Rationale: Deterministic replay and easier execution traceability.

## ADR-006: Cache Strategy
- Status: Accepted
- Decision: L1 Caffeine, L2 Redis.
- Rationale: Reduce latency for hot paths while keeping shared state consistent.

## ADR-007: Virtual Threads in Definition Service
- Status: Accepted
- Decision: Enable virtual threads for better concurrency.
- Rationale: IO-bound workloads benefit without complex thread tuning.

## ADR-008: Authentication Approach
- Status: Accepted
- Decision: Use Google Sign-In at web login and issue a platform JWT for service authorization.
- Rationale: Keep user identity verification at login while maintaining consistent internal authorization claims across services.
