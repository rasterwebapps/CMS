-- V163: Add date_of_birth column to enquiries table
-- The Enquiry entity maps a dateOfBirth field (nullable = false) that was
-- missing from the schema, causing Hibernate schema-validation to fail.

ALTER TABLE enquiries
    ADD COLUMN IF NOT EXISTS date_of_birth DATE;

-- Back-fill existing rows with a placeholder date so we can apply NOT NULL.
UPDATE enquiries
SET date_of_birth = '2000-01-01'
WHERE date_of_birth IS NULL;

ALTER TABLE enquiries
    ALTER COLUMN date_of_birth SET NOT NULL;

