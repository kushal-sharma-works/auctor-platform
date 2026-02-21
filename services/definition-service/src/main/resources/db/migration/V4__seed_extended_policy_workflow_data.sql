-- Extended policy/workflow seed data with realistic combinations

-- Version bump for existing policy to demonstrate immutable versioning
INSERT INTO policy_definitions (id, name, version, status, conditions, created_at, jpa_version)
VALUES (
    'amount-check-001',
    'Amount and Country Check Policy',
    2,
    'PUBLISHED',
    '[
        {"field": "amount", "operator": "LTE", "value": "15000"},
        {"field": "country", "operator": "IN", "value": "DE,FR,NL,SE"}
    ]'::jsonb,
    NOW(),
    0
);

-- Additional policies covering practical operator combinations
INSERT INTO policy_definitions (id, name, version, status, conditions, created_at, jpa_version) VALUES
(
    'geo-allowlist-001',
    'Geo Allowlist Policy',
    1,
    'PUBLISHED',
    '[
        {"field": "country", "operator": "IN", "value": "DE,FR,NL,SE,ES,IT"},
        {"field": "region", "operator": "NEQ", "value": "sanctioned"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'vip-expedite-001',
    'VIP Expedite Policy',
    1,
    'PUBLISHED',
    '[
        {"field": "customerTier", "operator": "EQ", "value": "VIP"},
        {"field": "openIncidents", "operator": "LTE", "value": "0"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'kyc-risk-001',
    'KYC Risk Gate Policy',
    1,
    'PUBLISHED',
    '[
        {"field": "kycScore", "operator": "GTE", "value": "80"},
        {"field": "riskScore", "operator": "LT", "value": "60"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'sanction-screen-001',
    'Sanction Screening Policy',
    1,
    'PUBLISHED',
    '[
        {"field": "sanctionFlag", "operator": "EQ", "value": "false"},
        {"field": "watchlistHits", "operator": "LTE", "value": "0"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'basket-size-001',
    'Basket Size Threshold Policy',
    1,
    'PUBLISHED',
    '[
        {"field": "itemCount", "operator": "GT", "value": "0"},
        {"field": "itemCount", "operator": "LTE", "value": "100"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'channel-block-001',
    'Restricted Channel Policy',
    1,
    'PUBLISHED',
    '[
        {"field": "salesChannel", "operator": "NOT_IN", "value": "black-market,untrusted-reseller"},
        {"field": "merchantStatus", "operator": "EQ", "value": "ACTIVE"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'age-gate-001',
    'Age Eligibility Policy',
    1,
    'DRAFT',
    '[
        {"field": "age", "operator": "GTE", "value": "18"},
        {"field": "country", "operator": "NOT_IN", "value": "restricted-country"}
    ]'::jsonb,
    NOW(),
    0
);

-- New workflow version for payment approval with manual review path
INSERT INTO workflow_definitions (id, name, version, status, states, initial_state, transitions, created_at, updated_at, jpa_version)
VALUES (
    'payment-approval-001',
    'Payment Approval Workflow',
    2,
    'PUBLISHED',
    '["DRAFT", "REVIEW", "MANUAL_REVIEW", "APPROVED", "REJECTED"]'::jsonb,
    'DRAFT',
    '[
        {"fromState": "DRAFT", "toState": "REVIEW", "policyRef": null, "guardExpression": null},
        {"fromState": "REVIEW", "toState": "APPROVED", "policyRef": "amount-check-001", "guardExpression": null},
        {"fromState": "REVIEW", "toState": "MANUAL_REVIEW", "policyRef": "geo-allowlist-001", "guardExpression": "countryNotAllowlisted"},
        {"fromState": "MANUAL_REVIEW", "toState": "APPROVED", "policyRef": "vip-expedite-001", "guardExpression": null},
        {"fromState": "MANUAL_REVIEW", "toState": "REJECTED", "policyRef": null, "guardExpression": null}
    ]'::jsonb,
    NOW(),
    NOW(),
    0
);

-- Shipping eligibility workflow
INSERT INTO workflow_definitions (id, name, version, status, states, initial_state, transitions, created_at, updated_at, jpa_version)
VALUES (
    'shipping-eligibility-001',
    'Shipping Eligibility Workflow',
    1,
    'PUBLISHED',
    '["CREATED", "CHECKED", "ELIGIBLE", "INELIGIBLE", "HOLD"]'::jsonb,
    'CREATED',
    '[
        {"fromState": "CREATED", "toState": "CHECKED", "policyRef": null, "guardExpression": null},
        {"fromState": "CHECKED", "toState": "ELIGIBLE", "policyRef": "channel-block-001", "guardExpression": null},
        {"fromState": "CHECKED", "toState": "HOLD", "policyRef": "geo-allowlist-001", "guardExpression": "manualGeoReview"},
        {"fromState": "HOLD", "toState": "ELIGIBLE", "policyRef": "basket-size-001", "guardExpression": null},
        {"fromState": "HOLD", "toState": "INELIGIBLE", "policyRef": null, "guardExpression": null}
    ]'::jsonb,
    NOW(),
    NOW(),
    0
);

-- Loan underwriting workflow
INSERT INTO workflow_definitions (id, name, version, status, states, initial_state, transitions, created_at, updated_at, jpa_version)
VALUES (
    'loan-underwriting-001',
    'Loan Underwriting Workflow',
    1,
    'PUBLISHED',
    '["SUBMITTED", "KYC_CHECK", "RISK_REVIEW", "APPROVED", "DECLINED"]'::jsonb,
    'SUBMITTED',
    '[
        {"fromState": "SUBMITTED", "toState": "KYC_CHECK", "policyRef": null, "guardExpression": null},
        {"fromState": "KYC_CHECK", "toState": "RISK_REVIEW", "policyRef": "kyc-risk-001", "guardExpression": null},
        {"fromState": "RISK_REVIEW", "toState": "APPROVED", "policyRef": "sanction-screen-001", "guardExpression": null},
        {"fromState": "RISK_REVIEW", "toState": "DECLINED", "policyRef": null, "guardExpression": null}
    ]'::jsonb,
    NOW(),
    NOW(),
    0
);

-- Draft workflow for upcoming release planning
INSERT INTO workflow_definitions (id, name, version, status, states, initial_state, transitions, created_at, updated_at, jpa_version)
VALUES (
    'returns-routing-001',
    'Returns Routing Workflow',
    1,
    'DRAFT',
    '["RECEIVED", "INSPECTION", "RESTOCK", "REFUND", "DISPOSE"]'::jsonb,
    'RECEIVED',
    '[
        {"fromState": "RECEIVED", "toState": "INSPECTION", "policyRef": null, "guardExpression": null},
        {"fromState": "INSPECTION", "toState": "RESTOCK", "policyRef": "basket-size-001", "guardExpression": null},
        {"fromState": "INSPECTION", "toState": "REFUND", "policyRef": "vip-expedite-001", "guardExpression": null},
        {"fromState": "INSPECTION", "toState": "DISPOSE", "policyRef": null, "guardExpression": null}
    ]'::jsonb,
    NOW(),
    NOW(),
    0
);
