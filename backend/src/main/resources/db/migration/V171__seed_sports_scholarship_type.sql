-- BR-14: SPORTS is part of the default scholarship type master set.
-- Keep this idempotent so upgraded databases and fresh databases converge.

INSERT INTO scholarship_types (
    code, name, description, govt_scheme, scheme_code, discount_type, discount_value,
    max_amount_per_year, renewal_required, is_active, application_mode, portal_name,
    portal_url, eligible_from_year, eligible_to_year, created_at, updated_at
)
VALUES (
    'SPORTS',
    'Sports Scholarship',
    'Institution scholarship for students admitted under sports quota',
    FALSE,
    NULL,
    'FIXED_AMOUNT',
    25000.00,
    25000.00,
    FALSE,
    TRUE,
    'INSTITUTION',
    NULL,
    NULL,
    NULL,
    NULL,
    NOW(),
    NOW()
)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    govt_scheme = EXCLUDED.govt_scheme,
    scheme_code = EXCLUDED.scheme_code,
    discount_type = EXCLUDED.discount_type,
    discount_value = EXCLUDED.discount_value,
    max_amount_per_year = EXCLUDED.max_amount_per_year,
    renewal_required = EXCLUDED.renewal_required,
    is_active = TRUE,
    application_mode = EXCLUDED.application_mode,
    portal_name = EXCLUDED.portal_name,
    portal_url = EXCLUDED.portal_url,
    eligible_from_year = EXCLUDED.eligible_from_year,
    eligible_to_year = EXCLUDED.eligible_to_year,
    updated_at = NOW();
