# Operations

This document captures the minimum operational context for running Auctor in SIT.

## Environments
- Primary target: SIT
- Optional: dev (local or cluster)

## Release Flow (SIT)
- Build and test via GitHub Actions.
- Deploy manually with the `Deploy - SIT` workflow, selecting a branch and optional image tag.
	The workflow builds images, pushes them to ACR, and deploys via Helm.

## Zero-to-Hero (Local -> SIT)
### 1) Run services locally (individual)
```bash
# Terminal 1: Postgres + Redis
docker compose up -d postgres redis

# Terminal 2: Definition service
cd services/definition-service
mvn spring-boot:run

# Terminal 3: Execution service
cd services/execution-service
./gradlew run

# Terminal 4: Web
cd web
npm run dev
```

Local URLs:
- Web UI: http://localhost:3000
- Definition API: http://localhost:8081
- Execution GraphQL: http://localhost:8082

### 2) Provision SIT infrastructure (Azure)
```bash
cd infra/terraform/azure
cp terraform.tfvars.example terraform.tfvars
terraform init -backend=false
terraform apply
```

### 3) Build & push images
```bash
# Example (replace registry and tag)
docker build -t <acr-login-server>/definition-service:<tag> services/definition-service
docker build -t <acr-login-server>/execution-service:<tag> services/execution-service
docker build -t <acr-login-server>/web:<tag> web
docker push <acr-login-server>/definition-service:<tag>
docker push <acr-login-server>/execution-service:<tag>
docker push <acr-login-server>/web:<tag>
```

### 4) Deploy to SIT with Helm
```bash
cd infra/helm
helm upgrade --install auctor-sit . -n auctor --create-namespace -f values-sit.yaml \
	--set image.registry=<acr-login-server> \
	--set image.tag=<tag> \
	--set ingress.host=auctor-platform.sit.example.com
```

### 5) Point DNS to ingress
- Create an `A` record for your SIT domain (e.g. `auctor-platform.sit.example.com`).
- Point it to the public IP of your ingress controller.

## Kubernetes Health Checks
- definition-service: `/actuator/health`
- execution-service: `/health`
- web: `/`

## Runtime Configuration
Non-sensitive configuration is set in the Helm ConfigMap:
- Database URLs and usernames
- gRPC target for definition-service
- Redis URL
- OTLP endpoints

Sensitive configuration is set in the Helm Secret:
- Definition DB password
- Execution DB password
- Definition JWT secret

## Observability Endpoints (Local)
- Prometheus: http://localhost:9091
- Grafana: http://localhost:3001
- Jaeger: http://localhost:16686

## Rollback
Use Helm rollback for SIT deployments:
```bash
helm rollback auctor-sit -n auctor
```
