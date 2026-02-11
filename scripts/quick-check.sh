#!/bin/bash

# Quick Pre-Deployment Check
# Fast validation of essential components

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "⚡ Quick Pre-Deployment Check"
echo ""

CHECKS_PASSED=0
CHECKS_FAILED=0

# 1. Check if code compiles
echo -n "Checking if code compiles... "
if mvn -f services/definition-service/pom.xml compile -q > /dev/null 2>&1 && \
   (cd services/execution-service && ./gradlew compileKotlin -q > /dev/null 2>&1) && \
   (cd web && npm run build --silent > /dev/null 2>&1); then
    echo -e "${GREEN}✓${NC}"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    echo -e "${RED}✗${NC}"
    CHECKS_FAILED=$((CHECKS_FAILED + 1))
fi

# 2. Check Helm charts
echo -n "Checking Helm charts... "
if helm lint infra/helm > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC}"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    echo -e "${RED}✗${NC}"
    CHECKS_FAILED=$((CHECKS_FAILED + 1))
fi

# 3. Check Terraform
echo -n "Checking Terraform... "
if (cd infra/terraform/azure && terraform init -backend=false > /dev/null 2>&1 && \
   terraform validate > /dev/null 2>&1); then
    echo -e "${GREEN}✓${NC}"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    echo -e "${RED}✗${NC}"
    CHECKS_FAILED=$((CHECKS_FAILED + 1))
fi

# 4. Check docker-compose
echo -n "Checking docker-compose.yml... "
if docker-compose config > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC}"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    echo -e "${RED}✗${NC}"
    CHECKS_FAILED=$((CHECKS_FAILED + 1))
fi

echo ""
echo "Results: $CHECKS_PASSED/$((CHECKS_PASSED + CHECKS_FAILED)) passed"

if [ $CHECKS_FAILED -eq 0 ]; then
    echo -e "${GREEN}Ready for deployment!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Run full tests: ./scripts/validate-all.sh"
    echo "  2. Test locally:   ./scripts/test-local.sh"
    echo "  3. Deploy to SIT:  See docs/deployment-guide.md"
    exit 0
else
    echo -e "${RED}Fix issues before deploying${NC}"
    exit 1
fi
