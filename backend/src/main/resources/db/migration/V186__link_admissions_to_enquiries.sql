-- Chain fix: bind admissions to their source enquiry (business rule: every admission comes from an enquiry)
-- Backfill path 1: link via enquiries.converted_student_id (all real admissions through enquiry flow)
-- Backfill path 2: create synthetic ADMITTED enquiries for seed/legacy admissions with no enquiry record

ALTER TABLE admissions ADD COLUMN enquiry_id BIGINT REFERENCES enquiries(id);

-- Path 1: link real admissions (first admission per student, handles program-transfer rows)
UPDATE admissions a
SET enquiry_id = e.id
FROM enquiries e
WHERE e.converted_student_id = a.student_id
  AND a.id = (
      SELECT MIN(a2.id) FROM admissions a2 WHERE a2.student_id = a.student_id
  );

-- Path 2: for any still-unlinked admissions (e.g. V45 seed data), create a synthetic ADMITTED enquiry
INSERT INTO enquiries (
    name, email, phone, program_id,
    enquiry_date, status,
    date_of_birth, gender,
    referral_type_id, converted_student_id,
    created_at, updated_at
)
SELECT
    s.first_name || ' ' || s.last_name,
    s.email,
    s.phone,
    s.program_id,
    a.application_date,
    'ADMITTED',
    COALESCE(s.date_of_birth, '2000-01-01'::date),
    COALESCE(s.gender::VARCHAR, 'FEMALE'),
    (SELECT id FROM referral_types WHERE code = 'WALK_IN' LIMIT 1),
    s.id,
    a.created_at,
    a.updated_at
FROM admissions a
JOIN students s ON s.id = a.student_id
WHERE a.enquiry_id IS NULL;

-- Link those new synthetic enquiries back to their admissions
UPDATE admissions a
SET enquiry_id = e.id
FROM enquiries e
WHERE e.converted_student_id = a.student_id
  AND a.enquiry_id IS NULL;

-- All rows must now be linked — fail loudly if anything slipped through
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM admissions WHERE enquiry_id IS NULL) THEN
        RAISE EXCEPTION
            'V186 failed: % admission(s) still have no enquiry_id after both backfill passes.',
            (SELECT COUNT(*) FROM admissions WHERE enquiry_id IS NULL);
    END IF;
END $$;

ALTER TABLE admissions ALTER COLUMN enquiry_id SET NOT NULL;
ALTER TABLE admissions ADD CONSTRAINT uq_admissions_enquiry_id UNIQUE (enquiry_id);
CREATE INDEX idx_admissions_enquiry_id ON admissions(enquiry_id);
