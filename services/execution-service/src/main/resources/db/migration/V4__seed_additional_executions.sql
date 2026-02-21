-- Additional execution seed data to expand demo coverage
-- Existing executions from V3: 6
-- Added below: 4
-- Final total executions: 10

INSERT INTO executions (id, workflow_id, workflow_version, current_state, status, input, created_at, updated_at) VALUES
(
    'exec-fin-0001',
    'fintech-payment-screening-001',
    1,
    'CLEARED',
    'COMPLETED',
    '{"txnCount10m":"2","amount":"9800","country":"NL"}',
    NOW() - INTERVAL '8 minutes',
    NOW() - INTERVAL '6 minutes'
),
(
    'exec-ins-0001',
    'insurance-claim-adjudication-001',
    1,
    'APPROVED',
    'COMPLETED',
    '{"claimAmount":"12000","policyActive":"true","claimType":"medical"}',
    NOW() - INTERVAL '15 minutes',
    NOW() - INTERVAL '11 minutes'
),
(
    'exec-trv-0001',
    'travel-booking-confirmation-001',
    1,
    'CANCELLED',
    'FAILED:UmlzayBjaGVjayBmYWlsZWQgZm9yIGRlc3RpbmF0aW9u',
    '{"riskScore":"83","destination":"restricted-zone","bookingValue":"4300"}',
    NOW() - INTERVAL '13 minutes',
    NOW() - INTERVAL '9 minutes'
),
(
    'exec-edu-0001',
    'education-enrollment-review-001',
    1,
    'PREREQ_CHECK',
    'RUNNING',
    '{"gpa":"3.1","requiredCredits":"42","program":"data-science"}',
    NOW() - INTERVAL '7 minutes',
    NOW() - INTERVAL '2 minutes'
);

INSERT INTO audit_events (id, execution_id, timestamp, event_type, from_state, to_state, policy_id, policy_result, explanation, actor, correlation_id) VALUES
(
    'audit-fin-0001-01',
    'exec-fin-0001',
    NOW() - INTERVAL '8 minutes',
    'EXECUTION_STARTED',
    NULL,
    'RECEIVED',
    NULL,
    NULL,
    'Fintech screening execution started',
    'system',
    '77777777-7777-7777-7777-777777777777'
),
(
    'audit-fin-0001-02',
    'exec-fin-0001',
    NOW() - INTERVAL '7 minutes',
    'POLICY_EVALUATED',
    'SCREENED',
    'CLEARED',
    'fintech-fraud-velocity-001',
    TRUE,
    'Velocity and amount checks passed',
    'policy-engine',
    '77777777-7777-7777-7777-777777777777'
),
(
    'audit-fin-0001-03',
    'exec-fin-0001',
    NOW() - INTERVAL '6 minutes',
    'EXECUTION_COMPLETED',
    'CLEARED',
    'CLEARED',
    NULL,
    NULL,
    'Execution completed in cleared state',
    'engine',
    '77777777-7777-7777-7777-777777777777'
),
(
    'audit-ins-0001-01',
    'exec-ins-0001',
    NOW() - INTERVAL '15 minutes',
    'EXECUTION_STARTED',
    NULL,
    'SUBMITTED',
    NULL,
    NULL,
    'Insurance claim workflow started',
    'system',
    '88888888-8888-8888-8888-888888888888'
),
(
    'audit-ins-0001-02',
    'exec-ins-0001',
    NOW() - INTERVAL '12 minutes',
    'POLICY_EVALUATED',
    'VALIDATED',
    'APPROVED',
    'insurance-claim-threshold-001',
    TRUE,
    'Claim amount within policy threshold',
    'policy-engine',
    '88888888-8888-8888-8888-888888888888'
),
(
    'audit-ins-0001-03',
    'exec-ins-0001',
    NOW() - INTERVAL '11 minutes',
    'EXECUTION_COMPLETED',
    'APPROVED',
    'APPROVED',
    NULL,
    NULL,
    'Claim adjudication completed',
    'engine',
    '88888888-8888-8888-8888-888888888888'
),
(
    'audit-trv-0001-01',
    'exec-trv-0001',
    NOW() - INTERVAL '13 minutes',
    'EXECUTION_STARTED',
    NULL,
    'INITIATED',
    NULL,
    NULL,
    'Travel booking workflow started',
    'system',
    '99999999-9999-9999-9999-999999999999'
),
(
    'audit-trv-0001-02',
    'exec-trv-0001',
    NOW() - INTERVAL '10 minutes',
    'POLICY_EVALUATED',
    'RISK_CHECK',
    'CANCELLED',
    'travel-booking-risk-001',
    FALSE,
    'Destination blocked by travel risk policy',
    'policy-engine',
    '99999999-9999-9999-9999-999999999999'
),
(
    'audit-trv-0001-03',
    'exec-trv-0001',
    NOW() - INTERVAL '9 minutes',
    'EXECUTION_FAILED',
    'RISK_CHECK',
    'CANCELLED',
    NULL,
    NULL,
    'Booking cancelled after failed risk check',
    'engine',
    '99999999-9999-9999-9999-999999999999'
),
(
    'audit-edu-0001-01',
    'exec-edu-0001',
    NOW() - INTERVAL '7 minutes',
    'EXECUTION_STARTED',
    NULL,
    'APPLIED',
    NULL,
    NULL,
    'Enrollment review started',
    'system',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
),
(
    'audit-edu-0001-02',
    'exec-edu-0001',
    NOW() - INTERVAL '3 minutes',
    'STATE_TRANSITION',
    'APPLIED',
    'PREREQ_CHECK',
    NULL,
    NULL,
    'Moved to prerequisite evaluation stage',
    'engine',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
);
