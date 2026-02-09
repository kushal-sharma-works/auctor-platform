-- Create executions table
CREATE TABLE executions (
    id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(36) NOT NULL,
    workflow_version INTEGER NOT NULL,
    current_state VARCHAR(100) NOT NULL,
    status VARCHAR(255) NOT NULL,
    input TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create index on workflow_id for faster lookups
CREATE INDEX idx_executions_workflow_id ON executions(workflow_id);

-- Create index on created_at for pagination
CREATE INDEX idx_executions_created_at ON executions(created_at DESC);
