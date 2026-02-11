# Pre-Deployment Validation Guide

This guide walks you through validating all changes before deploying to SIT.

## 🎯 Quick Validation Checklist

```bash
# Run all validation steps
./scripts/validate-all.sh
```

Or follow the detailed steps below:

---

## 1️⃣ Code Quality & Linting

### Java (Definition Service)
```bash
cd services/definition-service

# Compile and check for errors
mvn clean compile

# Run code formatting check
mvn spotless:check

# Run static analysis
mvn verify -DskipTests
```

### Kotlin (Execution Service)
```bash
cd services/execution-service

# Compile and check for errors
./gradlew build -x test

# Run linting
./gradlew ktlintCheck

# Check for any build warnings
./gradlew compileKotlin --warning-mode all
```

### Web (TypeScript/React)
```bash
cd web

# Install dependencies
npm ci

# Type checking
npm run type-check

# Linting
npm run lint

# Build check
npm run build
```

---

## 2️⃣ Unit Tests

### Definition Service
```bash
cd services/definition-service

# Run all unit tests
mvn test

# Run with coverage report
mvn test jacoco:report

# Check coverage (optional)
open target/site/jacoco/index.html
```

**Expected**: All tests pass ✅

### Execution Service
```bash
cd services/execution-service

# Run all unit tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport

# Check coverage (optional)
open build/reports/jacoco/test/html/index.html
```

**Expected**: All tests pass ✅

### Web UI
```bash
cd web

# Run all tests
npm test -- --passWithNoTests

# Run with coverage
npm test -- --coverage --watchAll=false
```

**Expected**: All tests pass ✅

---

## 3️⃣ Integration Tests (with Docker)

### Start Infrastructure
```bash
# Start only databases
docker-compose up -d postgres redis

# Wait for databases to be ready (30 seconds)
sleep 30

# Verify databases are up
docker-compose ps
```

### Test Definition Service
```bash
cd services/definition-service

# Run integration tests (needs running Postgres)
mvn verify

# Or run specific integration tests
mvn test -Dtest="*IT"
```

### Test Execution Service
```bash
cd services/execution-service

# Run integration tests
./gradlew integrationTest

# Or all tests including integration
./gradlew check
```

### API Tests
```bash
# Start all services
docker-compose up -d

# Wait for services to be healthy (check with)
docker-compose ps

# Run API tests for Definition Service
cd api-tests/definition-service
./test-definition-api.sh

# Run API tests for Execution Service
cd api-tests/execution-service
./test-execution-api.sh
```

**Expected**: All API calls succeed with 200/201 responses ✅

---

## 4️⃣ Full Stack Test (Docker Compose)

### Start Everything
```bash
# Build and start all services
docker-compose up --build

# In another terminal, wait for services to be healthy
watch docker-compose ps
```

**Wait until all services show "healthy" status**

### Manual Smoke Tests

#### Definition Service Health
```bash
curl http://localhost:8081/actuator/health
# Expected: {"status":"UP",...}

# Check metrics endpoint
curl http://localhost:8081/actuator/prometheus | head -20
# Expected: Prometheus metrics output
```

#### Execution Service Health
```bash
curl http://localhost:8082/health
# Expected: {"status":"UP",...}

# Check metrics endpoint
curl http://localhost:8082/metrics | head -20
# Expected: Prometheus metrics output
```

#### Web UI
```bash
curl -I http://localhost:3000
# Expected: HTTP 200 OK

# Or open in browser
open http://localhost:3000
```

#### Observability Stack
```bash
# Prometheus
curl http://localhost:9091/-/healthy
# Expected: Prometheus is Healthy

# Grafana
curl http://localhost:3001/api/health
# Expected: {"database":"ok",...}

# Jaeger
curl http://localhost:16686
# Expected: HTML response

# OTel Collector health
curl http://localhost:13133
# Expected: OTel collector health response
```

### Test Full Flow
```bash
# 1. Create a workflow definition
curl -X POST http://localhost:8081/api/definitions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-workflow",
    "description": "Test workflow",
    "steps": [{"action": "start"}]
  }'

# 2. Execute the workflow (via Web UI or GraphQL)
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { definitions { id name } }"
  }'

# 3. Check traces in Jaeger
open http://localhost:16686

# 4. Check metrics in Grafana
open http://localhost:3001
```

### Check Logs
```bash
# Check for errors in logs
docker-compose logs definition-service | grep -i error
docker-compose logs execution-service | grep -i error
docker-compose logs web | grep -i error

# Check structured JSON logs
docker-compose logs definition-service | tail -5 | jq .
docker-compose logs execution-service | tail -5 | jq .
```

**Expected**: No error logs, proper JSON formatting ✅

### Cleanup
```bash
docker-compose down -v
```

---

## 5️⃣ Kubernetes/Helm Validation

### Helm Chart Linting
```bash
cd infra/helm

# Lint the chart
helm lint .

# Lint with specific values
helm lint . -f values-sit.yaml

# Check for deprecations
helm lint . --strict
```

**Expected**: No errors or warnings ✅

### Helm Template Rendering
```bash
# Render templates and check for issues
helm template auctor-test . -f values-sit.yaml > /tmp/rendered.yaml

# Check rendered output
cat /tmp/rendered.yaml | grep -i "error\|invalid"

# Validate Kubernetes YAML syntax
kubectl apply --dry-run=client -f /tmp/rendered.yaml

# Check for specific resources
grep "kind: ServiceAccount" /tmp/rendered.yaml
grep "kind: PodDisruptionBudget" /tmp/rendered.yaml
grep "kind: HorizontalPodAutoscaler" /tmp/rendered.yaml
```

**Expected**: All resources render correctly ✅

### Security Context Validation
```bash
# Check that all deployments have security contexts
helm template auctor-test . -f values-sit.yaml | grep -A 5 "securityContext:"

# Verify runAsNonRoot is set
helm template auctor-test . -f values-sit.yaml | grep "runAsNonRoot: true"

# Verify capabilities are dropped
helm template auctor-test . -f values-sit.yaml | grep -A 2 "capabilities:"
```

**Expected**: Security contexts present in all deployments ✅

---

## 6️⃣ Terraform Validation

### Format Check
```bash
cd infra/terraform/azure

# Check formatting
terraform fmt -check -recursive

# Auto-format if needed
terraform fmt -recursive
```

### Validation
```bash
# Initialize (without backend for validation)
terraform init -backend=false

# Validate configuration
terraform validate

# Check for potential issues
terraform plan
```

**Expected**: "Success! The configuration is valid." ✅

### Security Check
```bash
# Check for common misconfigurations (requires tfsec)
# Install: brew install tfsec
tfsec .

# Or use checkov (requires checkov)
# Install: pip install checkov
checkov -d .
```

---

## 7️⃣ Security Scanning

### Dependency Vulnerabilities

#### Java Dependencies
```bash
cd services/definition-service

# Check for known vulnerabilities
mvn dependency-check:check

# Check results
cat target/dependency-check-report.html
```

#### Kotlin Dependencies
```bash
cd services/execution-service

# Check for vulnerabilities
./gradlew dependencyCheckAnalyze

# Check results
cat build/reports/dependency-check-report.html
```

#### NPM Dependencies
```bash
cd web

# Check for vulnerabilities
npm audit

# Check for high/critical only
npm audit --audit-level=high

# Fix if possible
npm audit fix
```

### Secret Scanning
```bash
# Check for accidentally committed secrets
# Install: brew install truffleHog
trufflehog filesystem . --json | jq .
```

### Image Vulnerability Scanning
```bash
# Build images locally
docker build -t definition-service:test services/definition-service
docker build -t execution-service:test services/execution-service
docker build -t web:test web

# Scan with Trivy (install: brew install trivy)
trivy image definition-service:test
trivy image execution-service:test
trivy image web:test

# Check for HIGH and CRITICAL only
trivy image --severity HIGH,CRITICAL definition-service:test
```

---

## 8️⃣ Performance & Load Testing (Optional)

### Basic Load Test
```bash
# Install: brew install hey
# Or: go install github.com/rakyll/hey@latest

# Start services
docker-compose up -d

# Test Definition Service
hey -n 1000 -c 10 http://localhost:8081/actuator/health

# Test Execution Service
hey -n 1000 -c 10 http://localhost:8082/health

# Monitor metrics during load
curl http://localhost:9091/api/v1/query?query=rate(http_server_requests_seconds_count[1m])
```

---

## 9️⃣ Pre-Deployment Checklist

Before deploying to SIT, ensure:

### Code Quality ✅
- [ ] All unit tests pass
- [ ] No compilation errors
- [ ] Linting passes
- [ ] Code coverage acceptable

### Integration ✅
- [ ] API tests pass
- [ ] Docker Compose starts successfully
- [ ] All services healthy
- [ ] No error logs

### Infrastructure ✅
- [ ] Helm charts lint successfully
- [ ] Templates render without errors
- [ ] Security contexts configured
- [ ] Terraform validates

### Security ✅
- [ ] No high/critical vulnerabilities
- [ ] No secrets in code
- [ ] Image scanning passed
- [ ] Security contexts present

### Documentation ✅
- [ ] README updated
- [ ] Environment variables documented
- [ ] Deployment guide reviewed

---

## 🚀 Ready to Deploy

Once all checks pass:

### Option 1: Manual Deployment
```bash
# Build and push images
cd infra/helm
./scripts/build-and-push.sh v1.0.0

# Deploy with Helm
helm upgrade --install auctor-sit . \
  -n auctor --create-namespace \
  -f values-sit.yaml \
  --set image.tag=v1.0.0
```

### Option 2: GitHub Actions
1. Go to Actions → "Deploy - SIT"
2. Click "Run workflow"
3. Enter git ref and image tag
4. Monitor deployment

---

## 🔍 Post-Deployment Verification

After deploying to SIT:

```bash
# Get AKS credentials
az aks get-credentials --resource-group <rg> --name <aks-name>

# Check pod status
kubectl get pods -n auctor

# Check services
kubectl get svc -n auctor

# Check ingress
kubectl get ingress -n auctor

# View logs
kubectl logs -n auctor -l app.kubernetes.io/name=definition-service --tail=50

# Port-forward to test locally
kubectl port-forward -n auctor svc/auctor-sit-definition-service 8081:8081

# Test endpoints
curl http://localhost:8081/actuator/health
```

### Smoke Tests in SIT
```bash
# Define your SIT domain
SIT_URL="https://auctor-platform.sit.example.com"

# Test web UI
curl -I $SIT_URL
# Expected: 200 OK

# Test definition service
curl $SIT_URL/api/actuator/health
# Expected: {"status":"UP"}

# Test execution service
curl $SIT_URL/graphql -X POST \
  -H "Content-Type: application/json" \
  -d '{"query":"{ __schema { queryType { name } } }"}'
# Expected: Valid GraphQL response

# Check metrics are being collected
curl $SIT_URL/api/actuator/prometheus | head -20
```

---

## 🐛 Troubleshooting

### Services Not Starting
```bash
# Check pod status
kubectl describe pod <pod-name> -n auctor

# Check logs
kubectl logs <pod-name> -n auctor

# Check events
kubectl get events -n auctor --sort-by='.lastTimestamp'
```

### Database Connection Issues
```bash
# Check if PostgreSQL is accessible
kubectl exec -it <pod-name> -n auctor -- psql -h <db-host> -U <user> -l

# Check database logs in Azure Portal
```

### Image Pull Errors
```bash
# Verify ACR permissions
az aks check-acr --resource-group <rg> --name <aks> --acr <acr-name>

# Check image exists
az acr repository show --name <acr> --repository definition-service
```

---

## 📞 Need Help?

- Documentation: See `docs/` directory
- Issues: Check existing GitHub issues
- Logs: Always check pod logs first
- Metrics: Review Grafana dashboards
