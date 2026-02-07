-- Sample payment approval workflow
INSERT INTO workflow_definitions (id, name, version, status, states, initial_state, transitions, created_at, updated_at, jpa_version) 
VALUES (
    'payment-approval-001',
    'Payment Approval Workflow',
    1,
    'PUBLISHED',
    '["DRAFT", "REVIEW", "APPROVED", "REJECTED"]'::jsonb,
    'DRAFT',
    '[
        {"fromState": "DRAFT", "toState": "REVIEW", "policyRef": null, "guardExpression": null},
        {"fromState": "REVIEW", "toState": "APPROVED", "policyRef": "amount-check-001", "guardExpression": null},
        {"fromState": "REVIEW", "toState": "REJECTED", "policyRef": null, "guardExpression": null}
    ]'::jsonb,
    NOW(),
    NOW(),
    0
);

-- Sample amount check policy
INSERT INTO policy_definitions (id, name, version, status, conditions, created_at, jpa_version)
VALUES (
    'amount-check-001',
    'Amount and Country Check Policy',
    1,
    'PUBLISHED',
    '[
        {"field": "amount", "operator": "LTE", "value": "10000"},
        {"field": "country", "operator": "IN", "value": "DE,FR,NL"}
    ]'::jsonb,
    NOW(),
    0
);
