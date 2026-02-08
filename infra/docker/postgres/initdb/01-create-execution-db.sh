#!/bin/bash
set -e

# Dev-only defaults; override with EXECUTION_DB_PASSWORD for safer values.
EXECUTION_DB_PASSWORD="${EXECUTION_DB_PASSWORD:-execution}"

# Use psql variable interpolation to safely handle special characters
psql -v ON_ERROR_STOP=1 -v "db_password=$EXECUTION_DB_PASSWORD" --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'execution') THEN
        EXECUTE format('CREATE ROLE execution LOGIN PASSWORD %L', :'db_password');
    END IF;
END
\$\$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'execution') THEN
        EXECUTE 'CREATE DATABASE execution OWNER execution';
    END IF;
END
$$;

GRANT ALL PRIVILEGES ON DATABASE execution TO execution;
EOSQL
