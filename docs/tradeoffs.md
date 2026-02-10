# Tradeoffs and Scope Boundaries

## What We Did Not Build
- BPMN engine
- Custom auth provider
- Event bus (Kafka)
- Full RBAC admin UI

## Why
- Focused scope to keep the core deterministic workflow engine solid.
- Reduced operational burden for a small team.
- Avoided complexity that is not required for SIT validation.

## Scaling Path
- Horizontal scaling through Kubernetes HPA for execution-service.
- PostgreSQL read replicas for heavy read traffic.
- Cache warming for popular policies and workflows.
