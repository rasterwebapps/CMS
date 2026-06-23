-- V233: Replace staff_referrers.institution free-text column with a FK to
-- the new institutions master, so a referrer's sister-concern college must
-- be one of a known, admin-managed list rather than arbitrary text.
--
-- Add nullable first, backfill from existing free-text institution values
-- (creating institution master rows as needed, same approach V219 used for
-- referred_staff_name), then enforce NOT NULL.

ALTER TABLE staff_referrers
    ADD COLUMN institution_id BIGINT REFERENCES institutions(id);

-- Create institution master rows for any distinct existing free-text values.
INSERT INTO institutions (name, code, is_active, created_at, updated_at)
SELECT DISTINCT
    TRIM(sr.institution),
    UPPER(SUBSTRING(REGEXP_REPLACE(TRIM(sr.institution), '[^A-Za-z0-9]+', '_', 'g'), 1, 50)),
    TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM staff_referrers sr
WHERE sr.institution IS NOT NULL AND TRIM(sr.institution) <> ''
ON CONFLICT DO NOTHING;

-- Point existing referrers at the matching institution.
UPDATE staff_referrers sr
SET institution_id = i.id
FROM institutions i
WHERE LOWER(TRIM(sr.institution)) = LOWER(i.name)
  AND sr.institution IS NOT NULL;

-- Any referrer with no institution text (or no match) falls back to a
-- placeholder "Unspecified" institution so NOT NULL can be enforced
-- without losing rows.
INSERT INTO institutions (name, code, is_active, created_at, updated_at)
SELECT 'Unspecified', 'UNSPECIFIED', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM staff_referrers WHERE institution_id IS NULL)
  AND NOT EXISTS (SELECT 1 FROM institutions WHERE code = 'UNSPECIFIED');

UPDATE staff_referrers sr
SET institution_id = (SELECT id FROM institutions WHERE code = 'UNSPECIFIED')
WHERE sr.institution_id IS NULL;

ALTER TABLE staff_referrers ALTER COLUMN institution_id SET NOT NULL;
ALTER TABLE staff_referrers DROP COLUMN institution;

CREATE INDEX idx_staff_referrers_institution ON staff_referrers(institution_id);
