-- Create audit_events table (append-only)
CREATE TABLE audit_events (
    id VARCHAR(64) PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    from_state VARCHAR(100),
    to_state VARCHAR(100),
    policy_id VARCHAR(100),
    policy_result BOOLEAN,
    explanation TEXT,
    actor VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(36) NOT NULL
);

-- Create index on execution_id for audit trail queries
CREATE INDEX idx_audit_events_execution_id ON audit_events(execution_id);

-- Create index on timestamp for ordering
CREATE INDEX idx_audit_events_timestamp ON audit_events(timestamp);

-- Create composite index for typical query pattern
CREATE INDEX idx_audit_events_execution_timestamp ON audit_events(execution_id, timestamp);
