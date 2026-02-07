#!/bin/bash

# Definition Service API Test Script
# Executes all REST endpoints and saves responses

BASE_URL="http://localhost:8081"
OUTPUT_DIR="./api-test-results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="$OUTPUT_DIR/test-log-$TIMESTAMP.txt"

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Logging function
log() {
  echo "$1" | tee -a "$LOG_FILE" >&2
}

# Execute curl and save combined request/response
execute_request() {
    local name=$1
    local method=$2
    local url=$3
    local data=$4
    local output_file="$OUTPUT_DIR/${name}.json"
    
    log ""
    log "=========================================="
    log "🔹 $name"
    log "=========================================="
    log "Method: $method"
    log "URL: $url"
    
    # Execute request
    if [ -n "$data" ]; then
        log "Request Body:"
      echo "$data" | jq . 2>/dev/null | tee -a "$LOG_FILE" >&2 || echo "$data" | tee -a "$LOG_FILE" >&2
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -d "$data")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url")
    fi
    
    # Split response and status code
    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | sed '$d')
    
    # Prepare request body value for JSON output
    local req_body_type="null"
    local req_body_value=""
    if [ -n "$data" ]; then
      if echo "$data" | jq -e . >/dev/null 2>&1; then
        req_body_type="json"
        req_body_value=$(echo "$data" | jq -c .)
      else
        req_body_type="string"
        req_body_value="$data"
      fi
    fi

    # Prepare response body value for JSON output
    local res_body_type="string"
    local res_body_value="$response_body"
    if echo "$response_body" | jq -e . >/dev/null 2>&1; then
      res_body_type="json"
      res_body_value=$(echo "$response_body" | jq -c .)
    fi

    # Build combined request/response JSON
    local req_body_expr="null"
    local res_body_expr="\$resBody"
    local req_body_args=()
    local res_body_args=()

    if [ "$req_body_type" = "json" ]; then
      req_body_args=(--argjson reqBody "$req_body_value")
      req_body_expr="\$reqBody"
    elif [ "$req_body_type" = "string" ]; then
      req_body_args=(--arg reqBody "$req_body_value")
      req_body_expr="\$reqBody"
    fi

    if [ "$res_body_type" = "json" ]; then
      res_body_args=(--argjson resBody "$res_body_value")
      res_body_expr="\$resBody"
    else
      res_body_args=(--arg resBody "$res_body_value")
    fi

    jq -n \
      --arg name "$name" \
      --arg method "$method" \
      --arg url "$url" \
      --arg timestamp "$(date)" \
      --arg status "$http_code" \
      "${req_body_args[@]}" \
      "${res_body_args[@]}" \
      "{request:{name:\$name,method:\$method,url:\$url,timestamp:\$timestamp,body:$req_body_expr},response:{status:\$status,body:$res_body_expr}}" \
      > "$output_file"
    
    log "Status Code: $http_code"
    log "Request/response saved to: $output_file"
    log "Response:"
    echo "$response_body" | jq . 2>/dev/null | tee -a "$LOG_FILE" >&2 || echo "$response_body" | tee -a "$LOG_FILE" >&2
    
    # Return response body for further processing
    echo "$response_body"
}

# Start testing
log "╔════════════════════════════════════════════════════════╗"
log "║     Definition Service API Test Suite                 ║"
log "║     Timestamp: $(date)                   ║"
log "╚════════════════════════════════════════════════════════╝"

# Health Check
execute_request "00-health-check" "GET" "$BASE_URL/actuator/health" ""

# ============================================
# POLICY TESTS
# ============================================
log ""
log "╔════════════════════════════════════════════════════════╗"
log "║                  POLICY ENDPOINTS                      ║"
log "╚════════════════════════════════════════════════════════╝"

# Create Policy 1
POLICY_DATA_1='{
  "name": "High Value Order Policy",
  "conditions": [
    {
      "field": "amount",
      "operator": "GT",
      "value": "1000"
    },
    {
      "field": "customerType",
      "operator": "EQ",
      "value": "PREMIUM"
    }
  ]
}'
POLICY_RESPONSE_1=$(execute_request "01-create-policy-high-value" "POST" "$BASE_URL/api/v1/policies" "$POLICY_DATA_1")
POLICY_ID_1=$(echo "$POLICY_RESPONSE_1" | jq -r '.id' 2>/dev/null)

# Create Policy 2
POLICY_DATA_2='{
  "name": "Shipping Eligibility Policy",
  "conditions": [
    {
      "field": "destination",
      "operator": "IN",
      "value": "USA,CANADA,MEXICO"
    },
    {
      "field": "weight",
      "operator": "LT",
      "value": "50"
    },
    {
      "field": "hazardous",
      "operator": "EQ",
      "value": "false"
    }
  ]
}'
POLICY_RESPONSE_2=$(execute_request "02-create-policy-shipping" "POST" "$BASE_URL/api/v1/policies" "$POLICY_DATA_2")
POLICY_ID_2=$(echo "$POLICY_RESPONSE_2" | jq -r '.id' 2>/dev/null)

# List all policies
execute_request "03-list-policies-page-0" "GET" "$BASE_URL/api/v1/policies?page=0&size=10" ""

# Get specific policy
if [ "$POLICY_ID_1" != "null" ] && [ -n "$POLICY_ID_1" ]; then
    execute_request "04-get-policy-by-id" "GET" "$BASE_URL/api/v1/policies/$POLICY_ID_1" ""
    
    # Publish policy
    PUBLISHED_POLICY=$(execute_request "05-publish-policy" "POST" "$BASE_URL/api/v1/policies/$POLICY_ID_1/publish" "")
    
    # Get specific version
    execute_request "06-get-policy-version-1" "GET" "$BASE_URL/api/v1/policies/$POLICY_ID_1/versions/1" ""
fi

# ============================================
# WORKFLOW TESTS
# ============================================
log ""
log "╔════════════════════════════════════════════════════════╗"
log "║                 WORKFLOW ENDPOINTS                     ║"
log "╚════════════════════════════════════════════════════════╝"

# Create Workflow 1
WORKFLOW_DATA_1='{
  "name": "Order Approval Workflow",
  "states": ["PENDING", "REVIEWING", "APPROVED", "REJECTED", "COMPLETED"],
  "initialState": "PENDING",
  "transitions": [
    {
      "fromState": "PENDING",
      "toState": "REVIEWING",
      "guardExpression": "amount < 10000"
    },
    {
      "fromState": "REVIEWING",
      "toState": "APPROVED",
      "policyRef": "'$POLICY_ID_1'",
      "guardExpression": "approverCount >= 1"
    },
    {
      "fromState": "REVIEWING",
      "toState": "REJECTED",
      "guardExpression": "!approved"
    },
    {
      "fromState": "APPROVED",
      "toState": "COMPLETED",
      "policyRef": "'$POLICY_ID_2'"
    }
  ]
}'
WORKFLOW_RESPONSE_1=$(execute_request "07-create-workflow-order-approval" "POST" "$BASE_URL/api/v1/workflows" "$WORKFLOW_DATA_1")
WORKFLOW_ID_1=$(echo "$WORKFLOW_RESPONSE_1" | jq -r '.id' 2>/dev/null)

# Create Workflow 2
WORKFLOW_DATA_2='{
  "name": "Simple Two-State Workflow",
  "states": ["START", "END"],
  "initialState": "START",
  "transitions": [
    {
      "fromState": "START",
      "toState": "END",
      "guardExpression": "completed == true"
    }
  ]
}'
WORKFLOW_RESPONSE_2=$(execute_request "08-create-workflow-simple" "POST" "$BASE_URL/api/v1/workflows" "$WORKFLOW_DATA_2")
WORKFLOW_ID_2=$(echo "$WORKFLOW_RESPONSE_2" | jq -r '.id' 2>/dev/null)

# List all workflows
execute_request "09-list-workflows-page-0" "GET" "$BASE_URL/api/v1/workflows?page=0&size=10" ""

# Get specific workflow
if [ "$WORKFLOW_ID_1" != "null" ] && [ -n "$WORKFLOW_ID_1" ]; then
    execute_request "10-get-workflow-by-id" "GET" "$BASE_URL/api/v1/workflows/$WORKFLOW_ID_1" ""
    
    # Publish workflow
    PUBLISHED_WORKFLOW=$(execute_request "11-publish-workflow" "POST" "$BASE_URL/api/v1/workflows/$WORKFLOW_ID_1/publish" "")
    
    # Get specific version
    execute_request "12-get-workflow-version-1" "GET" "$BASE_URL/api/v1/workflows/$WORKFLOW_ID_1/versions/1" ""
fi

# List workflows with different pagination
execute_request "13-list-workflows-page-1" "GET" "$BASE_URL/api/v1/workflows?page=1&size=5" ""

# List policies with different pagination
execute_request "14-list-policies-page-1" "GET" "$BASE_URL/api/v1/policies?page=1&size=5" ""

# ============================================
# ACTUATOR ENDPOINTS
# ============================================
log ""
log "╔════════════════════════════════════════════════════════╗"
log "║             ACTUATOR/MONITORING ENDPOINTS              ║"
log "╚════════════════════════════════════════════════════════╝"

execute_request "15-actuator-info" "GET" "$BASE_URL/actuator/info" ""
execute_request "16-actuator-metrics" "GET" "$BASE_URL/actuator/metrics" ""
execute_request "17-actuator-health-liveness" "GET" "$BASE_URL/actuator/health/liveness" ""
execute_request "18-actuator-health-readiness" "GET" "$BASE_URL/actuator/health/readiness" ""

# ============================================
# SUMMARY
# ============================================
log ""
log "╔════════════════════════════════════════════════════════╗"
log "║                    TEST SUMMARY                        ║"
log "╚════════════════════════════════════════════════════════╝"
log ""
log "✅ Test execution completed!"
log ""
log "📂 Results Location: $OUTPUT_DIR"
log "📋 Log File: $LOG_FILE"
log ""
log "📊 Created Resources:"
log "   • Policy 1 ID: $POLICY_ID_1"
log "   • Policy 2 ID: $POLICY_ID_2"
log "   • Workflow 1 ID: $WORKFLOW_ID_1"
log "   • Workflow 2 ID: $WORKFLOW_ID_2"
log ""
log "📁 Output Files:"
ls -lh "$OUTPUT_DIR"/*.json | awk '{print "   •", $9, "-", $5}' | tee -a "$LOG_FILE" >&2
log ""
log "🔍 View results:"
log "   cat $OUTPUT_DIR/*.json"
log "   cat $LOG_FILE"
log ""
log "═══════════════════════════════════════════════════════════"

# Create a summary file
SUMMARY_FILE="$OUTPUT_DIR/SUMMARY.txt"
{
    echo "Definition Service API Test Summary"
    echo "===================================="
    echo "Timestamp: $(date)"
    echo ""
    echo "Created Resources:"
    echo "  Policy 1: $POLICY_ID_1 (High Value Order Policy)"
    echo "  Policy 2: $POLICY_ID_2 (Shipping Eligibility Policy)"
    echo "  Workflow 1: $WORKFLOW_ID_1 (Order Approval Workflow)"
    echo "  Workflow 2: $WORKFLOW_ID_2 (Simple Two-State Workflow)"
    echo ""
    echo "Test Files Generated:"
    ls -1 "$OUTPUT_DIR"/*.json | wc -l | xargs echo "  Total Response Files:"
    echo ""
    echo "See $LOG_FILE for detailed execution log"
} > "$SUMMARY_FILE"

cat "$SUMMARY_FILE" | tee -a "$LOG_FILE" >&2
