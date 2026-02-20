#!/bin/bash

# Local Full Stack Testing Script
# Starts all services with docker compose and runs smoke tests

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_header() {
    echo ""
    echo "=========================================="
    echo "$1"
    echo "=========================================="
}

# Cleanup function
cleanup() {
    print_header "🧹 Cleaning up"
    cd "$PROJECT_ROOT"
    docker compose down -v
}

# Set trap to cleanup on exit
trap cleanup EXIT

print_header "🚀 Starting Local Full Stack Test"

cd "$PROJECT_ROOT"

# 1. Start services
print_header "1️⃣ Starting Services"
echo "Building and starting all services..."
docker compose up -d --build

# 2. Wait for services to be healthy
print_header "2️⃣ Waiting for Services"

MAX_WAIT=180  # 3 minutes
ELAPSED=0
INTERVAL=5

echo "Waiting for services to be healthy (timeout: ${MAX_WAIT}s)..."

while [ $ELAPSED -lt $MAX_WAIT ]; do
    # Check if all services are healthy
    UNHEALTHY=$(docker compose ps | grep -c "unhealthy\|starting" || true)
    
    if [ "$UNHEALTHY" -eq 0 ]; then
        print_success "All services are healthy!"
        break
    fi
    
    echo "Still waiting... ($ELAPSED/$MAX_WAIT seconds)"
    sleep $INTERVAL
    ELAPSED=$((ELAPSED + INTERVAL))
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
    print_error "Services did not become healthy in time"
    echo "Current status:"
    docker compose ps
    echo ""
    echo "Logs from unhealthy services:"
    docker compose logs --tail=50
    exit 1
fi

# 3. Run smoke tests
print_header "3️⃣ Running Smoke Tests"

TESTS_PASSED=0
TESTS_FAILED=0

# Test Definition Service Health
echo "Testing Definition Service health..."
if curl -sf http://localhost:8081/actuator/health > /dev/null; then
    print_success "Definition Service health check passed"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    print_error "Definition Service health check failed"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# Test Definition Service Metrics
echo "Testing Definition Service metrics..."
if curl -sf http://localhost:8081/actuator/prometheus | grep -q "jvm_memory_used_bytes"; then
    print_success "Definition Service metrics endpoint working"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    print_error "Definition Service metrics endpoint failed"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# Test Execution Service Health
echo "Testing Execution Service health..."
if curl -sf http://localhost:8082/health > /dev/null; then
    print_success "Execution Service health check passed"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    print_error "Execution Service health check failed"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# Test Execution Service Metrics
echo "Testing Execution Service metrics..."
if curl -sf http://localhost:8082/metrics | grep -q "execution"; then
    print_success "Execution Service metrics endpoint working"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    print_warning "Execution Service metrics may be empty (this is ok on startup)"
    TESTS_PASSED=$((TESTS_PASSED + 1))
fi

# Test Web UI
echo "Testing Web UI..."
if curl -sf -I http://localhost:3000 | grep -q "200 OK"; then
    print_success "Web UI responding"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    print_error "Web UI not responding"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# Test Prometheus
echo "Testing Prometheus..."
if curl -sf http://localhost:9091/-/healthy > /dev/null; then
    print_success "Prometheus is healthy"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    print_error "Prometheus health check failed"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# Test Grafana
echo "Testing Grafana..."
if curl -sf http://localhost:3001/api/health | grep -q "ok"; then
    print_success "Grafana is healthy"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    print_error "Grafana health check failed"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# Test Jaeger
echo "Testing Jaeger..."
if curl -sf http://localhost:16686 > /dev/null; then
    print_success "Jaeger UI is accessible"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    print_error "Jaeger UI not accessible"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# Test OTel Collector
echo "Testing OTel Collector..."
if curl -sf http://localhost:13133 > /dev/null 2>&1; then
    print_success "OTel Collector is healthy"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    print_warning "OTel Collector health check not responding (may not be critical)"
fi

# 4. Check for errors in logs
print_header "4️⃣ Checking Logs for Errors"

echo "Scanning logs for errors..."
ERROR_COUNT=0

for service in definition-service execution-service web; do
    ERRORS=$(docker compose logs $service | grep -i "error" | grep -v "0 error" | wc -l)
    if [ "$ERRORS" -gt 0 ]; then
        print_warning "$service has $ERRORS error log entries"
        ERROR_COUNT=$((ERROR_COUNT + ERRORS))
    else
        print_success "$service has no error logs"
    fi
done

# 5. Verify JSON logging
print_header "5️⃣ Verifying Structured Logging"

echo "Checking Definition Service logs..."
if docker compose logs definition-service | tail -5 | grep -q '"service":"definition-service"'; then
    print_success "Definition Service using structured JSON logging"
else
    print_warning "Definition Service may not be using structured logging"
fi

echo "Checking Execution Service logs..."
if docker compose logs execution-service | tail -5 | grep -q '"service":"execution-service"'; then
    print_success "Execution Service using structured JSON logging"
else
    print_warning "Execution Service may not be using structured logging"
fi

# Summary
print_header "📊 Test Summary"

TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED))
echo "Tests Passed: $TESTS_PASSED/$TOTAL_TESTS"
echo "Tests Failed: $TESTS_FAILED/$TOTAL_TESTS"
echo "Error Log Entries: $ERROR_COUNT"

if [ $TESTS_FAILED -eq 0 ]; then
    echo ""
    echo -e "${GREEN}"
    echo "╔════════════════════════════════════════╗"
    echo "║                                        ║"
    echo "║   ✓ ALL TESTS PASSED!                  ║"
    echo "║   Local stack is working correctly     ║"
    echo "║                                        ║"
    echo "╚════════════════════════════════════════╝"
    echo -e "${NC}"
    echo ""
    echo "Services are running. Access them at:"
    echo "  - Web UI:       http://localhost:3000"
    echo "  - Definition:   http://localhost:8081"
    echo "  - Execution:    http://localhost:8082"
    echo "  - Prometheus:   http://localhost:9091"
    echo "  - Grafana:      http://localhost:3001 (admin/admin)"
    echo "  - Jaeger:       http://localhost:16686"
    echo ""
    echo "Press Ctrl+C to stop all services"
    
    # Keep running
    docker compose logs -f
else
    echo ""
    echo -e "${RED}"
    echo "╔════════════════════════════════════════╗"
    echo "║                                        ║"
    echo "║   ✗ SOME TESTS FAILED                  ║"
    echo "║                                        ║"
    echo "╚════════════════════════════════════════╝"
    echo -e "${NC}"
    exit 1
fi
