#!/bin/bash

# Execution Service API Test Script
# Executes REST endpoints and saves responses

BASE_URL="http://localhost:8082"
OUTPUT_DIR="./api-test-results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="$OUTPUT_DIR/test-log-$TIMESTAMP.txt"

# Optional auth token for protected endpoints
# Export EXECUTION_JWT to enable POST requests
EXECUTION_JWT_VALUE="${EXECUTION_JWT:-}"
AUTH_HEADER=""
if [ -n "$EXECUTION_JWT_VALUE" ]; then
  AUTH_HEADER="Authorization: Bearer $EXECUTION_JWT_VALUE"
fi

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
    local needs_auth=$5
    local output_file="$OUTPUT_DIR/${name}.json"

    log ""
    log "=========================================="
    log "* $name"
    log "=========================================="
    log "Method: $method"
    log "URL: $url"

    # Build curl args
    local curl_args=(-s -w "\n%{http_code}" -X "$method" "$url")
    if [ "$needs_auth" = "true" ]; then
      if [ -z "$AUTH_HEADER" ]; then
        log "Missing EXECUTION_JWT; skipping auth-required request."
        return 0
      fi
      curl_args+=(-H "$AUTH_HEADER")
    fi

    if [ -n "$data" ]; then
        log "Request Body:"
        echo "$data" | jq . 2>/dev/null | tee -a "$LOG_FILE" >&2 || echo "$data" | tee -a "$LOG_FILE" >&2
        curl_args+=(-H "Content-Type: application/json" -d "$data")
    fi

    # Execute request
    response=$(curl "${curl_args[@]}")

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
log "========================================================"
log "Execution Service API Test Suite"
log "Timestamp: $(date)"
log "========================================================"

# Health Check
execute_request "00-health-check" "GET" "$BASE_URL/health" "" "false"

# Readiness Check
execute_request "01-readiness-check" "GET" "$BASE_URL/ready" "" "false"

# List executions
execute_request "02-list-executions" "GET" "$BASE_URL/api/v1/executions?limit=20&offset=0" "" "false"

# Start execution (requires JWT + workflow info)
WORKFLOW_ID_VALUE="${WORKFLOW_ID:-}"
WORKFLOW_VERSION_VALUE="${WORKFLOW_VERSION:-}"

if [ -z "$WORKFLOW_ID_VALUE" ] || [ -z "$WORKFLOW_VERSION_VALUE" ]; then
  log "Missing WORKFLOW_ID or WORKFLOW_VERSION; skipping execution create/advance/audit."
  exit 0
fi

START_DATA=$(cat <<EOF
{
  "workflowId": "$WORKFLOW_ID_VALUE",
  "workflowVersion": $WORKFLOW_VERSION_VALUE,
  "input": {
    "orderId": "order-001",
    "amount": "1500",
    "customerType": "PREMIUM"
  }
}
EOF
)

EXEC_RESPONSE=$(execute_request "03-start-execution" "POST" "$BASE_URL/api/v1/executions" "$START_DATA" "true")
EXEC_ID=$(echo "$EXEC_RESPONSE" | jq -r '.id' 2>/dev/null)

if [ "$EXEC_ID" = "null" ] || [ -z "$EXEC_ID" ]; then
  log "Execution id not found; skipping follow-up requests."
  exit 0
fi

# Get execution by id
execute_request "04-get-execution" "GET" "$BASE_URL/api/v1/executions/$EXEC_ID" "" "false"

# Advance execution (requires JWT)
ADVANCE_DATA='{"actor":"user"}'
execute_request "05-advance-execution" "POST" "$BASE_URL/api/v1/executions/$EXEC_ID/advance" "$ADVANCE_DATA" "true"

# Audit trail
execute_request "06-audit-trail" "GET" "$BASE_URL/api/v1/executions/$EXEC_ID/audit" "" "false"
