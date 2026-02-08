# Execution Service API Tests

Shell script-based API tests for the execution-service REST endpoints.

## Prerequisites

- `curl` - HTTP client
- `jq` - JSON processor
- Running execution-service (http://localhost:8082)
- Running definition-service (http://localhost:8081) with published workflow
- PostgreSQL database

## Setup

1. **Publish a workflow** in definition-service first:
   ```bash
   # Get workflow ID from definition-service API tests
   WORKFLOW_ID="<your-workflow-id>"
   curl -X POST "http://localhost:8081/api/v1/workflows/$WORKFLOW_ID/publish"
   ```

2. **Generate JWT token** (or use the provided Python script):
   ```bash
   python - <<'PY'
   import base64, json, hmac, hashlib
   def b64url(data: bytes) -> str:
       return base64.urlsafe_b64encode(data).rstrip(b'=').decode('ascii')
   header = {"alg": "HS256", "typ": "JWT"}
   payload = {
       "iss": "auctor-auth",
       "aud": "execution-service",
       "sub": "user-001",
       "roles": ["EXECUTOR"]
   }
   secret = b"dev-secret-change-later"
   segments = [b64url(json.dumps(header, separators=(',', ':')).encode()),
               b64url(json.dumps(payload, separators=(',', ':')).encode())]
   signing_input = (segments[0] + '.' + segments[1]).encode()
   signature = hmac.new(secret, signing_input, hashlib.sha256).digest()
   print(segments[0] + '.' + segments[1] + '.' + b64url(signature))
   PY
   ```

## Running Tests

### Full Test Suite (with JWT)

```bash
export EXECUTION_JWT="<your-jwt-token>"
export WORKFLOW_ID="<your-workflow-id>"
export WORKFLOW_VERSION="2"

bash test-execution-api.sh
```

### Example

```bash
export EXECUTION_JWT="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhdWN0b3ItYXV0aCIsImF1ZCI6ImV4ZWN1dGlvbi1zZXJ2aWNlIiwic3ViIjoidXNlci0wMDEiLCJyb2xlcyI6WyJFWEVDVVRPUiJdfQ.lcu_9uhGRH8EIWM9UHKH6GL-ep1bM9f_LtsYOx_i7jM"
export WORKFLOW_ID="8e09d574-9533-4694-b93b-432d9f3aba58"
export WORKFLOW_VERSION="2"

bash test-execution-api.sh
```

### Without JWT (public endpoints only)

```bash
# Will only test health, readiness, and list endpoints
bash test-execution-api.sh
```

## Environment Variables

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `EXECUTION_JWT` | No | JWT token for authenticated endpoints | `eyJhbGci...` |
| `WORKFLOW_ID` | No* | Workflow ID to start execution | `8e09d574-9533-4694-b93b-432d9f3aba58` |
| `WORKFLOW_VERSION` | No* | Workflow version to use | `2` |

*Required for creating new executions

## Test Coverage

### Public Endpoints
- `GET /health` - Health check
- `GET /ready` - Readiness check
- `GET /api/v1/executions` - List executions (with pagination)
- `GET /api/v1/executions/{id}` - Get execution by ID
- `GET /api/v1/executions/{id}/audit` - Get audit trail

### Authenticated Endpoints (require JWT)
- `POST /api/v1/executions` - Start new execution
- `POST /api/v1/executions/{id}/advance` - Advance execution state

## Output Files

All test results are saved to `./api-test-results/`:

- `00-health-check.json` - Health endpoint response
- `01-readiness-check.json` - Readiness endpoint response
- `02-list-executions.json` - List executions response
- `03-start-execution.json` - Create execution response
- `04-get-execution.json` - Get execution by ID response
- `05-advance-execution.json` - Advance execution response
- `06-audit-trail.json` - Audit trail response
- `test-log-YYYYMMDD_HHMMSS.txt` - Detailed execution log
- `SUMMARY.txt` - Test summary

Each JSON file contains both request and response data:
```json
{
  "request": {
    "name": "...",
    "method": "...",
    "url": "...",
    "timestamp": "...",
    "body": {...}
  },
  "response": {
    "status": "...",
    "body": {...}
  }
}
```

## Features Tested

- ✅ JWT authentication with roles-based authorization
- ✅ gRPC integration with definition-service
- ✅ State machine transitions with guard expressions
- ✅ Policy evaluation (via gRPC to definition-service)
- ✅ Audit trail recording (append-only)
- ✅ Database persistence (PostgreSQL)
- ✅ Pagination support
- ✅ Error handling (404, 401, 409, etc.)

## Troubleshooting

### "Missing EXECUTION_JWT; skipping auth-required request"
- Export the `EXECUTION_JWT` environment variable with a valid JWT token

### "Missing WORKFLOW_ID or WORKFLOW_VERSION; skipping execution create/advance/audit"
- Export both `WORKFLOW_ID` and `WORKFLOW_VERSION` environment variables

### "Workflow X vY is not PUBLISHED (status: DRAFT)"
- Publish the workflow first using definition-service API:
  ```bash
  curl -X POST "http://localhost:8081/api/v1/workflows/$WORKFLOW_ID/publish"
  ```

### Connection refused errors
- Ensure execution-service is running: `docker compose ps`
- Check service logs: `docker compose logs execution-service`

## See Also

- [Definition Service API Tests](../definition-service/test-definition-api.sh)
- [Execution Service Documentation](../../services/execution-service/README.md)
