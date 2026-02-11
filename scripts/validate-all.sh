#!/bin/bash

# Pre-Deployment Validation Script
# Runs all validation checks before deploying to SIT

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Track overall status
FAILED_CHECKS=()

print_header() {
    echo ""
    echo "=========================================="
    echo "$1"
    echo "=========================================="
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
    FAILED_CHECKS+=("$1")
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_header "🔍 Pre-Deployment Validation"
echo "Starting comprehensive validation..."

# 1. Code Compilation
print_header "1️⃣ Code Compilation"

echo "Compiling Definition Service..."
if cd "$PROJECT_ROOT/services/definition-service" && mvn clean compile -q; then
    print_success "Definition Service compiles"
else
    print_error "Definition Service compilation failed"
fi

echo "Compiling Execution Service..."
if cd "$PROJECT_ROOT/services/execution-service" && ./gradlew compileKotlin -q; then
    print_success "Execution Service compiles"
else
    print_error "Execution Service compilation failed"
fi

echo "Compiling Web UI..."
if cd "$PROJECT_ROOT/web" && npm run build --silent > /dev/null 2>&1; then
    print_success "Web UI compiles"
else
    print_error "Web UI compilation failed"
fi

# 2. Unit Tests
print_header "2️⃣ Unit Tests"

echo "Running Definition Service tests..."
if cd "$PROJECT_ROOT/services/definition-service" && mvn test -q; then
    print_success "Definition Service tests pass"
else
    print_error "Definition Service tests failed"
fi

echo "Running Execution Service tests..."
if cd "$PROJECT_ROOT/services/execution-service" && ./gradlew test -q; then
    print_success "Execution Service tests pass"
else
    print_error "Execution Service tests failed"
fi

echo "Running Web UI tests..."
if cd "$PROJECT_ROOT/web" && npm test -- --passWithNoTests --silent > /dev/null 2>&1; then
    print_success "Web UI tests pass"
else
    print_error "Web UI tests failed"
fi

# 3. Helm Validation
print_header "3️⃣ Helm Chart Validation"

echo "Linting Helm charts..."
if cd "$PROJECT_ROOT/infra/helm" && helm lint . > /dev/null 2>&1; then
    print_success "Helm charts lint successfully"
else
    print_error "Helm chart linting failed"
fi

echo "Linting with SIT values..."
if cd "$PROJECT_ROOT/infra/helm" && helm lint . -f values-sit.yaml > /dev/null 2>&1; then
    print_success "Helm charts lint with SIT values"
else
    print_error "Helm chart linting with SIT values failed"
fi

echo "Validating template rendering..."
if cd "$PROJECT_ROOT/infra/helm" && helm template auctor-test . -f values-sit.yaml > /tmp/rendered.yaml 2>&1; then
    print_success "Helm templates render successfully"
else
    print_error "Helm template rendering failed"
fi

echo "Checking for required resources..."
REQUIRED_RESOURCES=("ServiceAccount" "Deployment" "Service" "PodDisruptionBudget" "HorizontalPodAutoscaler")
for resource in "${REQUIRED_RESOURCES[@]}"; do
    if grep -q "kind: $resource" /tmp/rendered.yaml; then
        print_success "Found $resource resources"
    else
        print_warning "No $resource resources found"
    fi
done

# 4. Terraform Validation
print_header "4️⃣ Terraform Validation"

echo "Checking Terraform formatting..."
if cd "$PROJECT_ROOT/infra/terraform/azure" && terraform fmt -check -recursive > /dev/null 2>&1; then
    print_success "Terraform is properly formatted"
else
    print_warning "Terraform needs formatting (run: terraform fmt -recursive)"
fi

echo "Validating Terraform configuration..."
if cd "$PROJECT_ROOT/infra/terraform/azure" && terraform init -backend=false > /dev/null 2>&1 && terraform validate > /dev/null 2>&1; then
    print_success "Terraform configuration is valid"
else
    print_error "Terraform validation failed"
fi

# 5. Security Checks
print_header "5️⃣ Security Checks"

echo "Checking for security contexts in deployments..."
if grep -q "runAsNonRoot: true" /tmp/rendered.yaml; then
    print_success "Security contexts configured"
else
    print_error "Security contexts missing"
fi

echo "Checking for capabilities drop..."
if grep -q "drop:" /tmp/rendered.yaml && grep -q "ALL" /tmp/rendered.yaml; then
    print_success "Capabilities properly dropped"
else
    print_warning "Capabilities drop not found"
fi

echo "Checking npm dependencies for vulnerabilities..."
if cd "$PROJECT_ROOT/web" && npm audit --audit-level=high > /dev/null 2>&1; then
    print_success "No high-severity npm vulnerabilities"
else
    print_warning "High-severity npm vulnerabilities found - review with: npm audit"
fi

# 6. Docker Compose Test
print_header "6️⃣ Docker Compose Validation"

echo "Checking docker-compose.yml syntax..."
if cd "$PROJECT_ROOT" && docker-compose config > /dev/null 2>&1; then
    print_success "docker-compose.yml is valid"
else
    print_error "docker-compose.yml validation failed"
fi

# 7. Documentation Check
print_header "7️⃣ Documentation Check"

REQUIRED_DOCS=(
    "docs/architecture.md"
    "docs/operations.md"
    "docs/production-readiness.md"
    "docs/deployment-guide.md"
    "docs/disaster-recovery.md"
)

for doc in "${REQUIRED_DOCS[@]}"; do
    if [ -f "$PROJECT_ROOT/$doc" ]; then
        print_success "Found $doc"
    else
        print_warning "Missing $doc"
    fi
done

# Summary
print_header "📊 Validation Summary"

if [ ${#FAILED_CHECKS[@]} -eq 0 ]; then
    echo -e "${GREEN}"
    echo "╔════════════════════════════════════════╗"
    echo "║                                        ║"
    echo "║   ✓ ALL CHECKS PASSED!                 ║"
    echo "║   Ready for deployment                 ║"
    echo "║                                        ║"
    echo "╚════════════════════════════════════════╝"
    echo -e "${NC}"
    exit 0
else
    echo -e "${RED}"
    echo "╔════════════════════════════════════════╗"
    echo "║                                        ║"
    echo "║   ✗ VALIDATION FAILED                  ║"
    echo "║                                        ║"
    echo "╚════════════════════════════════════════╝"
    echo -e "${NC}"
    echo ""
    echo "Failed checks:"
    for check in "${FAILED_CHECKS[@]}"; do
        echo -e "${RED}  ✗${NC} $check"
    done
    echo ""
    echo "Please fix the issues above before deploying."
    exit 1
fi
