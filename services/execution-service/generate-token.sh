#!/bin/bash

# JWT Token Generator for Execution Service
# This script generates a valid JWT token for testing

# JWT Configuration (prefer env, fallback to application.conf)
CONFIG_FILE="$(dirname "$0")/src/main/resources/application.conf"
SECRET="${EXECUTION_JWT_SECRET}"
ISSUER="${EXECUTION_JWT_ISSUER}"
AUDIENCE="${EXECUTION_JWT_AUDIENCE}"
SUBJECT="${EXECUTION_JWT_SUBJECT:-test-user}"

if [[ -z "$SECRET" && -f "$CONFIG_FILE" ]]; then
    SECRET=$(grep -E "^\s*secret\s*=\s*\"" "$CONFIG_FILE" | head -n 1 | sed -E 's/.*"(.*)".*/\1/')
fi
if [[ -z "$ISSUER" && -f "$CONFIG_FILE" ]]; then
    ISSUER=$(grep -E "^\s*issuer\s*=\s*\"" "$CONFIG_FILE" | head -n 1 | sed -E 's/.*"(.*)".*/\1/')
fi
if [[ -z "$AUDIENCE" && -f "$CONFIG_FILE" ]]; then
    AUDIENCE=$(grep -E "^\s*audience\s*=\s*\"" "$CONFIG_FILE" | head -n 1 | sed -E 's/.*"(.*)".*/\1/')
fi

SECRET=${SECRET:-"dev-secret-change-later"}
ISSUER=${ISSUER:-"auctor-auth"}
AUDIENCE=${AUDIENCE:-"execution-service"}

# Check if openssl is available
if ! command -v openssl &> /dev/null; then
    echo "Error: openssl is required but not installed"
    exit 1
fi

# Create JWT Header
header='{"typ":"JWT","alg":"HS256"}'

# Create JWT Payload (compact JSON without whitespace)
payload="{\"iss\":\"$ISSUER\",\"aud\":\"$AUDIENCE\",\"sub\":\"$SUBJECT\",\"roles\":[\"VIEWER\",\"EXECUTOR\"]}"

# Function to base64url encode
base64url_encode() {
    openssl enc -base64 -A | tr '+/' '-_' | tr -d '='
}

# Encode header and payload
header_base64=$(echo -n "$header" | base64url_encode)
payload_base64=$(echo -n "$payload" | base64url_encode)

# Create signature
signature=$(echo -n "${header_base64}.${payload_base64}" | openssl dgst -sha256 -hmac "$SECRET" -binary | base64url_encode)

# Combine to create JWT
jwt="${header_base64}.${payload_base64}.${signature}"

echo "$jwt"
