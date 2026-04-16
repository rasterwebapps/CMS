-- Make referral_type_id NOT NULL (first set any NULLs to the WALK_IN referral type)
UPDATE enquiries SET referral_type_id = (SELECT id FROM referral_types WHERE code = 'WALK_IN') WHERE referral_type_id IS NULL;
ALTER TABLE enquiries ALTER COLUMN referral_type_id SET NOT NULL;

-- Drop the source column
ALTER TABLE enquiries DROP COLUMN IF EXISTS source;

-- Add course_id FK to enquiries
ALTER TABLE enquiries ADD COLUMN course_id BIGINT REFERENCES courses(id);

-- Remove assigned_to column
ALTER TABLE enquiries DROP COLUMN IF EXISTS assigned_to;
