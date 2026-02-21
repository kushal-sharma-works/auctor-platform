-- Industry policy + workflow matrix extension
-- Targets after this migration (including V3 + V4):
-- - policy_definitions rows: 20
-- - workflow_definitions rows: 20

-- Add 11 policies (existing total is 9 -> new total 20)
INSERT INTO policy_definitions (id, name, version, status, conditions, created_at, jpa_version) VALUES
(
    'fintech-fraud-velocity-001',
    'Fintech Fraud Velocity Policy',
    1,
    'PUBLISHED',
    '[
      {"field": "txnCount10m", "operator": "LTE", "value": "5"},
      {"field": "amount", "operator": "LTE", "value": "25000"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'insurance-claim-threshold-001',
    'Insurance Claim Threshold Policy',
    1,
    'PUBLISHED',
    '[
      {"field": "claimAmount", "operator": "LTE", "value": "50000"},
      {"field": "policyActive", "operator": "EQ", "value": "true"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'healthcare-consent-001',
    'Healthcare Consent Validation Policy',
    1,
    'PUBLISHED',
    '[
      {"field": "consentSigned", "operator": "EQ", "value": "true"},
      {"field": "patientAge", "operator": "GTE", "value": "18"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'retail-discount-guard-001',
    'Retail Discount Guard Policy',
    1,
    'PUBLISHED',
    '[
      {"field": "discountPct", "operator": "LTE", "value": "40"},
      {"field": "customerTier", "operator": "IN", "value": "STANDARD,VIP,PLATINUM"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'ecommerce-return-window-001',
    'E-commerce Return Window Policy',
    1,
    'PUBLISHED',
    '[
      {"field": "daysSinceDelivery", "operator": "LTE", "value": "30"},
      {"field": "orderStatus", "operator": "EQ", "value": "DELIVERED"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'logistics-hazard-route-001',
    'Logistics Hazard Route Policy',
    1,
    'PUBLISHED',
    '[
      {"field": "isHazmat", "operator": "EQ", "value": "false"},
      {"field": "routeClass", "operator": "NOT_IN", "value": "restricted,tunnel-banned"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'telecom-porting-eligibility-001',
    'Telecom Porting Eligibility Policy',
    1,
    'PUBLISHED',
    '[
      {"field": "arrearsAmount", "operator": "LTE", "value": "0"},
      {"field": "contractState", "operator": "IN", "value": "EXPIRED,MONTHLY"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'travel-booking-risk-001',
    'Travel Booking Risk Policy',
    1,
    'PUBLISHED',
    '[
      {"field": "riskScore", "operator": "LT", "value": "70"},
      {"field": "destination", "operator": "NOT_IN", "value": "restricted-zone"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'education-prereq-001',
    'Education Prerequisite Check Policy',
    1,
    'PUBLISHED',
    '[
      {"field": "gpa", "operator": "GTE", "value": "2.5"},
      {"field": "requiredCredits", "operator": "GTE", "value": "30"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'manufacturing-qc-001',
    'Manufacturing QC Release Policy',
    1,
    'PUBLISHED',
    '[
      {"field": "defectRate", "operator": "LTE", "value": "0.02"},
      {"field": "sampleSize", "operator": "GTE", "value": "50"}
    ]'::jsonb,
    NOW(),
    0
),
(
    'hospitality-overbooking-001',
    'Hospitality Overbooking Control Policy',
    1,
    'PUBLISHED',
    '[
      {"field": "occupancyRate", "operator": "LTE", "value": "1.05"},
      {"field": "bookingChannel", "operator": "IN", "value": "direct,ota,corporate"}
    ]'::jsonb,
    NOW(),
    0
);

-- Add 15 workflows (existing total is 5 -> new total 20)
INSERT INTO workflow_definitions (id, name, version, status, states, initial_state, transitions, created_at, updated_at, jpa_version) VALUES
(
    'fintech-payment-screening-001',
    'Fintech Payment Screening Workflow',
    1,
    'PUBLISHED',
    '["RECEIVED","SCREENED","CLEARED","BLOCKED"]'::jsonb,
    'RECEIVED',
    '[
      {"fromState":"RECEIVED","toState":"SCREENED","policyRef":null,"guardExpression":null},
      {"fromState":"SCREENED","toState":"CLEARED","policyRef":"fintech-fraud-velocity-001","guardExpression":null},
      {"fromState":"SCREENED","toState":"BLOCKED","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
),
(
    'insurance-claim-adjudication-001',
    'Insurance Claim Adjudication Workflow',
    1,
    'PUBLISHED',
    '["SUBMITTED","VALIDATED","APPROVED","DENIED"]'::jsonb,
    'SUBMITTED',
    '[
      {"fromState":"SUBMITTED","toState":"VALIDATED","policyRef":null,"guardExpression":null},
      {"fromState":"VALIDATED","toState":"APPROVED","policyRef":"insurance-claim-threshold-001","guardExpression":null},
      {"fromState":"VALIDATED","toState":"DENIED","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
),
(
    'healthcare-intake-approval-001',
    'Healthcare Intake Approval Workflow',
    1,
    'PUBLISHED',
    '["INTAKE","CONSENT_CHECK","ACCEPTED","REJECTED"]'::jsonb,
    'INTAKE',
    '[
      {"fromState":"INTAKE","toState":"CONSENT_CHECK","policyRef":null,"guardExpression":null},
      {"fromState":"CONSENT_CHECK","toState":"ACCEPTED","policyRef":"healthcare-consent-001","guardExpression":null},
      {"fromState":"CONSENT_CHECK","toState":"REJECTED","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
),
(
    'retail-promo-approval-001',
    'Retail Promotion Approval Workflow',
    1,
    'PUBLISHED',
    '["DRAFT","REVIEW","APPROVED","DECLINED"]'::jsonb,
    'DRAFT',
    '[
      {"fromState":"DRAFT","toState":"REVIEW","policyRef":null,"guardExpression":null},
      {"fromState":"REVIEW","toState":"APPROVED","policyRef":"retail-discount-guard-001","guardExpression":null},
      {"fromState":"REVIEW","toState":"DECLINED","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
),
(
    'ecommerce-return-authorization-001',
    'E-commerce Return Authorization Workflow',
    1,
    'PUBLISHED',
    '["REQUESTED","WINDOW_CHECK","AUTHORIZED","REJECTED"]'::jsonb,
    'REQUESTED',
    '[
      {"fromState":"REQUESTED","toState":"WINDOW_CHECK","policyRef":null,"guardExpression":null},
      {"fromState":"WINDOW_CHECK","toState":"AUTHORIZED","policyRef":"ecommerce-return-window-001","guardExpression":null},
      {"fromState":"WINDOW_CHECK","toState":"REJECTED","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
),
(
    'logistics-route-clearance-001',
    'Logistics Route Clearance Workflow',
    1,
    'PUBLISHED',
    '["PLANNED","ROUTE_CHECK","CLEARED","HOLD"]'::jsonb,
    'PLANNED',
    '[
      {"fromState":"PLANNED","toState":"ROUTE_CHECK","policyRef":null,"guardExpression":null},
      {"fromState":"ROUTE_CHECK","toState":"CLEARED","policyRef":"logistics-hazard-route-001","guardExpression":null},
      {"fromState":"ROUTE_CHECK","toState":"HOLD","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
),
(
    'telecom-number-porting-001',
    'Telecom Number Porting Workflow',
    1,
    'PUBLISHED',
    '["REQUESTED","ELIGIBILITY_CHECK","APPROVED","REJECTED"]'::jsonb,
    'REQUESTED',
    '[
      {"fromState":"REQUESTED","toState":"ELIGIBILITY_CHECK","policyRef":null,"guardExpression":null},
      {"fromState":"ELIGIBILITY_CHECK","toState":"APPROVED","policyRef":"telecom-porting-eligibility-001","guardExpression":null},
      {"fromState":"ELIGIBILITY_CHECK","toState":"REJECTED","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
),
(
    'travel-booking-confirmation-001',
    'Travel Booking Confirmation Workflow',
    1,
    'PUBLISHED',
    '["INITIATED","RISK_CHECK","CONFIRMED","CANCELLED"]'::jsonb,
    'INITIATED',
    '[
      {"fromState":"INITIATED","toState":"RISK_CHECK","policyRef":null,"guardExpression":null},
      {"fromState":"RISK_CHECK","toState":"CONFIRMED","policyRef":"travel-booking-risk-001","guardExpression":null},
      {"fromState":"RISK_CHECK","toState":"CANCELLED","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
),
(
    'education-enrollment-review-001',
    'Education Enrollment Review Workflow',
    1,
    'PUBLISHED',
    '["APPLIED","PREREQ_CHECK","ENROLLED","DECLINED"]'::jsonb,
    'APPLIED',
    '[
      {"fromState":"APPLIED","toState":"PREREQ_CHECK","policyRef":null,"guardExpression":null},
      {"fromState":"PREREQ_CHECK","toState":"ENROLLED","policyRef":"education-prereq-001","guardExpression":null},
      {"fromState":"PREREQ_CHECK","toState":"DECLINED","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
),
(
    'manufacturing-lot-release-001',
    'Manufacturing Lot Release Workflow',
    1,
    'PUBLISHED',
    '["ASSEMBLED","QC_CHECK","RELEASED","SCRAPPED"]'::jsonb,
    'ASSEMBLED',
    '[
      {"fromState":"ASSEMBLED","toState":"QC_CHECK","policyRef":null,"guardExpression":null},
      {"fromState":"QC_CHECK","toState":"RELEASED","policyRef":"manufacturing-qc-001","guardExpression":null},
      {"fromState":"QC_CHECK","toState":"SCRAPPED","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
),
(
    'hospitality-booking-allocation-001',
    'Hospitality Booking Allocation Workflow',
    1,
    'PUBLISHED',
    '["PENDING","CAPACITY_CHECK","ALLOCATED","WAITLISTED"]'::jsonb,
    'PENDING',
    '[
      {"fromState":"PENDING","toState":"CAPACITY_CHECK","policyRef":null,"guardExpression":null},
      {"fromState":"CAPACITY_CHECK","toState":"ALLOCATED","policyRef":"hospitality-overbooking-001","guardExpression":null},
      {"fromState":"CAPACITY_CHECK","toState":"WAITLISTED","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
),
(
    'energy-grid-request-001',
    'Energy Grid Access Workflow',
    1,
    'PUBLISHED',
    '["REQUESTED","VALIDATED","APPROVED","REJECTED"]'::jsonb,
    'REQUESTED',
    '[
      {"fromState":"REQUESTED","toState":"VALIDATED","policyRef":null,"guardExpression":null},
      {"fromState":"VALIDATED","toState":"APPROVED","policyRef":"geo-allowlist-001","guardExpression":null},
      {"fromState":"VALIDATED","toState":"REJECTED","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
),
(
    'realestate-tenant-screening-001',
    'Real Estate Tenant Screening Workflow',
    1,
    'PUBLISHED',
    '["SUBMITTED","SCREENING","APPROVED","DECLINED"]'::jsonb,
    'SUBMITTED',
    '[
      {"fromState":"SUBMITTED","toState":"SCREENING","policyRef":null,"guardExpression":null},
      {"fromState":"SCREENING","toState":"APPROVED","policyRef":"kyc-risk-001","guardExpression":null},
      {"fromState":"SCREENING","toState":"DECLINED","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
),
(
    'publicsector-grant-eligibility-001',
    'Public Sector Grant Eligibility Workflow',
    1,
    'PUBLISHED',
    '["FILED","ELIGIBILITY_CHECK","AWARDED","DENIED"]'::jsonb,
    'FILED',
    '[
      {"fromState":"FILED","toState":"ELIGIBILITY_CHECK","policyRef":null,"guardExpression":null},
      {"fromState":"ELIGIBILITY_CHECK","toState":"AWARDED","policyRef":"sanction-screen-001","guardExpression":null},
      {"fromState":"ELIGIBILITY_CHECK","toState":"DENIED","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
),
(
    'automotive-warranty-approval-001',
    'Automotive Warranty Approval Workflow',
    1,
    'PUBLISHED',
    '["OPENED","VALIDATION","APPROVED","REJECTED"]'::jsonb,
    'OPENED',
    '[
      {"fromState":"OPENED","toState":"VALIDATION","policyRef":null,"guardExpression":null},
      {"fromState":"VALIDATION","toState":"APPROVED","policyRef":"insurance-claim-threshold-001","guardExpression":null},
      {"fromState":"VALIDATION","toState":"REJECTED","policyRef":null,"guardExpression":null}
    ]'::jsonb,
    NOW(), NOW(), 0
);
