-- Expand execution and audit id lengths to allow prefixed UUIDs
ALTER TABLE executions
    ALTER COLUMN id TYPE VARCHAR(64);

ALTER TABLE audit_events
    ALTER COLUMN id TYPE VARCHAR(64),
    ALTER COLUMN execution_id TYPE VARCHAR(64);
