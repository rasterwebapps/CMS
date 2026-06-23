-- V234: Add employee_code to staff_referrers and scope name/employee_code
-- uniqueness to each institution (a referrer's name and employee code only
-- need to be unique among staff of the same sister-concern institution, not
-- globally across all institutions).
--
-- Add nullable first, backfill any pre-existing rows with a unique
-- placeholder (admins should replace these with real codes), then enforce
-- NOT NULL.

ALTER TABLE staff_referrers
    ADD COLUMN employee_code VARCHAR(50);

UPDATE staff_referrers
SET employee_code = 'LEGACY-' || id
WHERE employee_code IS NULL;

ALTER TABLE staff_referrers ALTER COLUMN employee_code SET NOT NULL;

-- Replace the old global unique-name index (from V219) with one scoped per institution.
DROP INDEX IF EXISTS idx_staff_referrers_name_ci;

CREATE UNIQUE INDEX idx_staff_referrers_institution_name_ci
    ON staff_referrers (institution_id, LOWER(name));

CREATE UNIQUE INDEX idx_staff_referrers_institution_employee_code_ci
    ON staff_referrers (institution_id, LOWER(employee_code));
