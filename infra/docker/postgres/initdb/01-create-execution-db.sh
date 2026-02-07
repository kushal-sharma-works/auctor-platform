#!/bin/bash
set -e

# Dev-only defaults; override with EXECUTION_DB_PASSWORD for safer values.
EXECUTION_DB_PASSWORD="${EXECUTION_DB_PASSWORD:-execution}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'execution') THEN
        CREATE ROLE execution LOGIN PASSWORD '${EXECUTION_DB_PASSWORD}';
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'execution') THEN
        EXECUTE 'CREATE DATABASE execution OWNER execution';
    END IF;
END
$$;

GRANT ALL PRIVILEGES ON DATABASE execution TO execution;
EOSQL
