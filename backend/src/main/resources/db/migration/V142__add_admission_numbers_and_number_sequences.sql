-- ==========================================================================
-- V142: Permanent admission numbers + common number sequence registry
-- ==========================================================================
-- Admission numbers are generated only when admission is completed/confirmed.
-- Format: ADM-2526-0001, unique across the college and scoped by academic year.
-- ==========================================================================

CREATE TABLE application_number_sequences (
    id               BIGSERIAL PRIMARY KEY,
    series_code      VARCHAR(50)  NOT NULL,
    series_name      VARCHAR(100) NOT NULL,
    scope_type       VARCHAR(50)  NOT NULL,
    scope_key        VARCHAR(50)  NOT NULL,
    prefix           VARCHAR(20)  NOT NULL,
    sequence_padding INTEGER      NOT NULL DEFAULT 4,
    last_sequence    INTEGER      NOT NULL DEFAULT 0,
    description      TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_application_number_sequences_series_scope UNIQUE (series_code, scope_key)
);

CREATE INDEX idx_application_number_sequences_series
    ON application_number_sequences (series_code, scope_key);

ALTER TABLE students ADD COLUMN admission_number VARCHAR(20);

WITH ranked_admissions AS (
    SELECT
        s.id AS student_id,
        substring(split_part(ay.name, '-', 1) from 3 for 2)
            || substring(split_part(ay.name, '-', 2) from 3 for 2) AS scope_key,
        row_number() OVER (
            PARTITION BY ay.id
            ORDER BY a.application_date NULLS LAST, a.id, s.id
        ) AS seq
    FROM admissions a
    JOIN students s ON s.id = a.student_id
    JOIN academic_years ay ON ay.id = a.joining_academic_year_id
    WHERE s.admission_number IS NULL
)
UPDATE students s
SET admission_number = 'ADM-' || r.scope_key || '-' || lpad(r.seq::TEXT, 4, '0')
FROM ranked_admissions r
WHERE s.id = r.student_id;

CREATE UNIQUE INDEX uk_students_admission_number
    ON students (admission_number)
    WHERE admission_number IS NOT NULL;

INSERT INTO application_number_sequences (
    series_code, series_name, scope_type, scope_key, prefix,
    sequence_padding, last_sequence, description
)
SELECT
    'ADMISSION_NUMBER',
    'Admission Number',
    'ACADEMIC_YEAR',
    scope_key,
    'ADM',
    4,
    MAX(seq),
    'Permanent admission reference generated when admission is completed'
FROM (
    SELECT
        substring(split_part(admission_number, '-', 2) from 1 for 4) AS scope_key,
        split_part(admission_number, '-', 3)::INTEGER AS seq
    FROM students
    WHERE admission_number IS NOT NULL
) existing_admissions
GROUP BY scope_key
ON CONFLICT (series_code, scope_key) DO UPDATE
SET last_sequence = GREATEST(application_number_sequences.last_sequence, EXCLUDED.last_sequence),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO application_number_sequences (
    series_code, series_name, scope_type, scope_key, prefix,
    sequence_padding, last_sequence, description
)
SELECT
    'RECEIPT_NUMBER',
    'Receipt Number',
    'CALENDAR_YEAR',
    year::TEXT,
    'RCP',
    5,
    last_seq,
    'Global receipt number generated for every payment receipt'
FROM receipt_number_sequence
ON CONFLICT (series_code, scope_key) DO UPDATE
SET last_sequence = GREATEST(application_number_sequences.last_sequence, EXCLUDED.last_sequence),
    updated_at = CURRENT_TIMESTAMP;

ALTER TABLE payment_receipts ADD COLUMN admission_number VARCHAR(20);

UPDATE payment_receipts pr
SET admission_number = s.admission_number
FROM students s
WHERE pr.payer_type = 'STUDENT'
  AND pr.payer_id = s.id
  AND pr.admission_number IS NULL;

INSERT INTO permissions (code, display_name, category, created_at)
VALUES ('NUMBER_SEQUENCE_VIEW', 'View Number Sequences', 'SETTINGS', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE p.code = 'NUMBER_SEQUENCE_VIEW'
  AND r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
ON CONFLICT DO NOTHING;
