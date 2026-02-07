-- Seed a sample execution for testing
INSERT INTO executions (id, workflow_id, workflow_version, current_state, status, input, created_at, updated_at)
VALUES (
    'exec-sample-001',
    'wf-order-approval',
    1,
    'pending',
    'RUNNING',
    '{"amount": "5000", "country": "US", "userId": "user-001"}',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Seed corresponding audit event
INSERT INTO audit_events (id, execution_id, timestamp, event_type, from_state, to_state, policy_id, policy_result, explanation, actor, correlation_id)
VALUES (
    'audit-sample-001',
    'exec-sample-001',
    CURRENT_TIMESTAMP,
    'EXECUTION_STARTED',
    NULL,
    'pending',
    NULL,
    NULL,
    'Execution started for workflow wf-order-approval version 1',
    'system',
    'corr-sample-001'
);
