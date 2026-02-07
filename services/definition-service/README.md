# Definition Service

A Spring Boot service that owns policy and workflow definitions for the Auctor platform. It exposes REST and gRPC APIs, persists versioned definitions in PostgreSQL, and provides deterministic, auditable behavior suitable for compliance-focused systems.

## Highlights
- Clean architecture separation: API layer, domain model, application services, and persistence adapters.
- Versioned definitions: publish creates a new version and preserves immutable history.
- Explicit domain validation for consistent, deterministic behavior.
- Flyway-managed schema with JSONB for flexible, structured conditions and transitions.
- REST for CRUD and lifecycle; gRPC for cross-service consumption.

## Architecture at a Glance

Layers and flow (left to right):

Client -> REST/gRPC API -> Domain Services -> Ports -> JPA Adapters -> PostgreSQL

Key packages:
- API (REST): [services/definition-service/src/main/java/com/auctor/definition/api/rest](services/definition-service/src/main/java/com/auctor/definition/api/rest)
- API (gRPC): [services/definition-service/src/main/java/com/auctor/definition/grpc](services/definition-service/src/main/java/com/auctor/definition/grpc)
- Domain model: [services/definition-service/src/main/java/com/auctor/definition/domain/model](services/definition-service/src/main/java/com/auctor/definition/domain/model)
- Domain services: [services/definition-service/src/main/java/com/auctor/definition/domain/service](services/definition-service/src/main/java/com/auctor/definition/domain/service)
- Ports (interfaces): [services/definition-service/src/main/java/com/auctor/definition/domain/port](services/definition-service/src/main/java/com/auctor/definition/domain/port)
- Persistence adapters and mapper: [services/definition-service/src/main/java/com/auctor/definition/infra/adapter](services/definition-service/src/main/java/com/auctor/definition/infra/adapter)
- JPA entities and converters: [services/definition-service/src/main/java/com/auctor/definition/infra/jpa](services/definition-service/src/main/java/com/auctor/definition/infra/jpa)
- DB migrations: [services/definition-service/src/main/resources/db/migration](services/definition-service/src/main/resources/db/migration)

## Core Domain Concepts

PolicyDefinition
- Immutable definition with conditions and status.
- Conditions are typed with an Operator enum (EQ, NEQ, GT, LT, GTE, LTE, IN, NOT_IN).
- Stored with a composite key (id, version) to preserve history.

WorkflowDefinition
- Immutable workflow graph definition with states, transitions, and status.
- Validates that all transition states exist in the workflow state set.
- Stored with a composite key (id, version) for time-travel and audit.

Statuses
- PolicyStatus and WorkflowStatus are sealed hierarchies: DRAFT and PUBLISHED.

## Versioning and Publishing

Publishing creates a new version with status PUBLISHED rather than updating in place. This:
- Preserves immutability and auditability.
- Avoids write conflicts with composite primary keys.
- Supports time-travel queries and deterministic replays.

Example behavior:
- Create policy version 1 in DRAFT.
- Publish -> version 2 in PUBLISHED.
- Query latest -> returns version 2.
- Query version 1 -> returns original DRAFT.

## Code Flow (REST)

Create policy
1. Controller validates request and maps DTO to domain.
2. PolicyService builds a new PolicyDefinition with a UUID and DRAFT status.
3. JpaPolicyCommandAdapter persists via DomainMapper.
4. Response is mapped back to a DTO.

Publish policy
1. Controller calls PolicyService.publish(id).
2. Service loads latest version via PolicyQueryPort.
3. Service validates DRAFT status.
4. Service creates a new version with status PUBLISHED.
5. Adapter persists and returns the published version.

Workflow flow is identical with additional validation for transitions.

## Persistence Model

Tables (Flyway)
- policy_definitions (id, version, name, status, conditions, created_at)
- workflow_definitions (id, version, name, status, states, initial_state, transitions, created_at, updated_at)

Notes
- JSONB is used for flexible conditions and transitions without sacrificing structure.
- Composite keys ensure versioned history per definition ID.
- Optimistic locking is enabled via jpa_version.

## Error Handling

All REST errors return RFC 7807 ProblemDetail responses:
- 400 for validation and illegal arguments.
- 404 for missing resources.
- 409 for optimistic locking conflicts.
- 500 for unexpected errors.

See [services/definition-service/src/main/java/com/auctor/definition/api/rest/exception/GlobalExceptionHandler.java](services/definition-service/src/main/java/com/auctor/definition/api/rest/exception/GlobalExceptionHandler.java).

## API Surface

REST endpoints
- Policies: POST /api/v1/policies, GET /api/v1/policies, GET /api/v1/policies/{id}, GET /api/v1/policies/{id}/versions/{version}, POST /api/v1/policies/{id}/publish
- Workflows: POST /api/v1/workflows, GET /api/v1/workflows, GET /api/v1/workflows/{id}, GET /api/v1/workflows/{id}/versions/{version}, POST /api/v1/workflows/{id}/publish

gRPC endpoints
- GetPolicy, GetWorkflow, EvaluatePolicy
- Proto: [services/definition-service/src/main/proto/definition.proto](services/definition-service/src/main/proto/definition.proto)

## Local Development

Prereqs
- Java 21
- PostgreSQL (or use docker compose)

Run with docker compose (from repo root)
- docker compose up -d postgres

Run the service
- cd services/definition-service
- mvn spring-boot:run

Run REST API tests
- cd api-tests/definition-service
- bash test-definition-api.sh

## Testing Strategy

- Unit tests for domain and mappers.
- Integration tests for REST controllers and JPA adapters.
- API test script that records request and response for all REST endpoints.

## Why this is a strong engineering artifact

- Clear separation of concerns and dependency inversion via ports.
- Immutable domain objects with explicit validation and reproducible behavior.
- Versioned persistence strategy aligned with auditability and compliance.
- Minimal framework leakage across layers for long-term maintainability.
- Both REST and gRPC interfaces for diverse client integrations.
