#!/bin/bash

# JWT Token Generator for Execution Service
# This script generates a valid JWT token for testing

# JWT Configuration
SECRET="local-dev-secret"
ISSUER="auctor-auth"
AUDIENCE="execution-service"
SUBJECT="test-user"

# Check if openssl is available
if ! command -v openssl &> /dev/null; then
    echo "Error: openssl is required but not installed"
    exit 1
fi

# Create JWT Header
header='{"typ":"JWT","alg":"HS256"}'

# Create JWT Payload (compact JSON without whitespace)
payload="{\"iss\":\"$ISSUER\",\"aud\":\"$AUDIENCE\",\"sub\":\"$SUBJECT\",\"roles\":[\"EXECUTOR\"]}"

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
