-- V164: Add gender column to enquiries table
-- The Enquiry entity maps a gender field (nullable = false, VARCHAR(20)) that was
-- missing from the schema, causing Hibernate schema-validation to fail on startup.

ALTER TABLE enquiries
    ADD COLUMN IF NOT EXISTS gender VARCHAR(20);

-- Back-fill existing rows with a default value so we can apply NOT NULL.
UPDATE enquiries
SET gender = 'MALE'
WHERE gender IS NULL;

ALTER TABLE enquiries
    ALTER COLUMN gender SET NOT NULL;

