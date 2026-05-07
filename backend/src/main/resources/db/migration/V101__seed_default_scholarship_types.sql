INSERT INTO scholarship_types (
    code, name, description, govt_scheme, discount_type, discount_value,
    max_amount_per_year, renewal_required, is_active
)
SELECT 'FIRST_GRAD', 'First Graduate Scholarship', 'For students who are first in family to attend college', FALSE, 'FIXED_AMOUNT', 20000.00, 20000.00, FALSE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM scholarship_types WHERE code = 'FIRST_GRAD');

INSERT INTO scholarship_types (
    code, name, description, govt_scheme, discount_type, discount_value,
    max_amount_per_year, renewal_required, is_active
)
SELECT 'SC_GOVT', 'SC Government Scholarship', 'Government scholarship for SC category students', TRUE, 'PERCENTAGE', 100.00, NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM scholarship_types WHERE code = 'SC_GOVT');

INSERT INTO scholarship_types (
    code, name, description, govt_scheme, discount_type, discount_value,
    max_amount_per_year, renewal_required, is_active
)
SELECT 'ST_GOVT', 'ST Government Scholarship', 'Government scholarship for ST category students', TRUE, 'PERCENTAGE', 100.00, NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM scholarship_types WHERE code = 'ST_GOVT');

INSERT INTO scholarship_types (
    code, name, description, govt_scheme, discount_type, discount_value,
    max_amount_per_year, renewal_required, is_active
)
SELECT 'OBC_GOVT', 'OBC Government Scholarship', 'Government scholarship for OBC category students', TRUE, 'FIXED_AMOUNT', 30000.00, 30000.00, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM scholarship_types WHERE code = 'OBC_GOVT');

INSERT INTO scholarship_types (
    code, name, description, govt_scheme, discount_type, discount_value,
    max_amount_per_year, renewal_required, is_active
)
SELECT 'BC_STATE', 'BC/MBC State Scholarship', 'State scholarship for BC/MBC category students', TRUE, 'FIXED_AMOUNT', 25000.00, 25000.00, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM scholarship_types WHERE code = 'BC_STATE');

INSERT INTO scholarship_types (
    code, name, description, govt_scheme, discount_type, discount_value,
    max_amount_per_year, renewal_required, is_active
)
SELECT 'EWS', 'Economically Weaker Section', 'For students with annual family income below ₹3,00,000', FALSE, 'PERCENTAGE', 50.00, NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM scholarship_types WHERE code = 'EWS');

INSERT INTO scholarship_types (
    code, name, description, govt_scheme, discount_type, discount_value,
    max_amount_per_year, renewal_required, is_active
)
SELECT 'MERIT', 'Merit Scholarship', 'For students with high merit in qualifying examination', FALSE, 'FIXED_AMOUNT', 30000.00, 30000.00, FALSE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM scholarship_types WHERE code = 'MERIT');

