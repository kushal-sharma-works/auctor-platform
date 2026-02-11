# Testing & Validation Quick Reference

## 🚀 Quick Commands

### Before You Deploy
```bash
# 1. Quick sanity check (30 seconds)
./scripts/quick-check.sh

# 2. Full validation (5-10 minutes)
./scripts/validate-all.sh

# 3. Test complete stack locally (including smoke tests)
./scripts/test-local.sh
```

## 📋 What Gets Tested

### Quick Check (`quick-check.sh`)
- ✓ Code compilation (Java, Kotlin, TypeScript)
- ✓ Helm chart syntax
- ✓ Terraform validation
- ✓ Docker Compose syntax

### Full Validation (`validate-all.sh`)
- ✓ All quick checks
- ✓ Unit tests for all services
- ✓ Helm template rendering
- ✓ Security context verification
- ✓ Documentation presence
- ✓ NPM dependency vulnerabilities

### Local Stack Test (`test-local.sh`)
- ✓ All services start successfully
- ✓ Health endpoints respond
- ✓ Metrics endpoints work
- ✓ Observability stack (Prometheus, Grafana, Jaeger)
- ✓ No errors in logs
- ✓ Structured JSON logging

## 🔍 Manual Testing

### Test Individual Services

#### Definition Service
```bash
# Start dependencies
docker-compose up -d postgres

# Run service
cd services/definition-service
mvn spring-boot:run

# Test
curl http://localhost:8081/actuator/health
curl http://localhost:8081/actuator/prometheus
```

#### Execution Service
```bash
# Start dependencies
docker-compose up -d postgres redis

# Run service
cd services/execution-service
./gradlew run

# Test
curl http://localhost:8082/health
curl http://localhost:8082/metrics
```

#### Web UI
```bash
cd web
npm run dev

# Open browser
open http://localhost:3000
```

## 🐛 Troubleshooting

### Services Won't Start
```bash
# Check logs
docker-compose logs <service-name>

# Check service status
docker-compose ps

# Restart specific service
docker-compose restart <service-name>
```

### Tests Failing
```bash
# Run tests with verbose output
mvn test -X  # Java (debug mode)
./gradlew test --info  # Kotlin
npm test -- --verbose  # TypeScript
```

### Port Conflicts
```bash
# Check what's using ports
lsof -i :8081  # Definition service
lsof -i :8082  # Execution service
lsof -i :3000  # Web UI
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis

# Kill process if needed
kill -9 <PID>
```

## 📊 Coverage Reports

### Generate Coverage
```bash
# Java
cd services/definition-service
mvn test jacoco:report
open target/site/jacoco/index.html

# Kotlin
cd services/execution-service
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html

# TypeScript
cd web
npm test -- --coverage
open coverage/lcov-report/index.html
```

## 🔐 Security Scanning

### Check Dependencies
```bash
# NPM
cd web
npm audit

# OWASP (Java)
cd services/definition-service
mvn dependency-check:check

# Gradle (Kotlin)
cd services/execution-service
./gradlew dependencyCheckAnalyze
```

### Scan Docker Images
```bash
# Build images
docker-compose build

# Scan with Trivy (install: brew install trivy)
trivy image auctor-platform-definition-service
trivy image auctor-platform-execution-service
trivy image auctor-platform-web
```

## ✅ Pre-Deployment Checklist

Before deploying to SIT:

- [ ] Quick check passes
- [ ] Full validation passes
- [ ] Local stack test successful
- [ ] No high/critical vulnerabilities
- [ ] All unit tests pass
- [ ] API tests pass
- [ ] Helm charts lint successfully
- [ ] Terraform validates
- [ ] Documentation reviewed

## 🚀 Deploy

Once all checks pass:

```bash
# Option 1: GitHub Actions
# Go to Actions → "Deploy - SIT" → Run workflow

# Option 2: Manual Helm deployment
cd infra/helm
helm upgrade --install auctor-sit . \
  -n auctor --create-namespace \
  -f values-sit.yaml \
  --set image.tag=v1.0.0
```

## 📖 More Information

- **Detailed Guide**: [Pre-Deployment Validation](../docs/pre-deployment-validation.md)
- **Deployment**: [Deployment Guide](../docs/deployment-guide.md)
- **Operations**: [Operations Guide](../docs/operations.md)
