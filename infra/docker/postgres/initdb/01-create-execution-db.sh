#!/bin/bash
set -e

# Dev-only defaults; override with EXECUTION_DB_PASSWORD for safer values.
EXECUTION_DB_PASSWORD="${EXECUTION_DB_PASSWORD:-execution}"

# Create role and database directly with psql
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<EOF
CREATE ROLE IF NOT EXISTS execution LOGIN PASSWORD '$EXECUTION_DB_PASSWORD';
CREATE DATABASE IF NOT EXISTS execution OWNER execution;
GRANT ALL PRIVILEGES ON DATABASE execution TO execution;
EOF
