-- V97: Add Student and Faculty referral types; track the referring person on enquiries.

-- 1. New referral type seeds
INSERT INTO referral_types (name, code, commission_amount, has_commission, description, is_active, created_at, updated_at)
VALUES
    ('Student Referral', 'STUDENT',  500, TRUE,  'Referred by a current student', TRUE, NOW(), NOW()),
    ('Faculty Referral', 'FACULTY',  500, TRUE,  'Referred by a faculty member',  TRUE, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- 2. Store the referring person on the enquiry (FK, nullable; only one will be populated at a time)
ALTER TABLE enquiries
    ADD COLUMN IF NOT EXISTS referred_student_id BIGINT REFERENCES students(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS referred_faculty_id BIGINT REFERENCES faculty(id)  ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_enquiries_referred_student ON enquiries(referred_student_id)
    WHERE referred_student_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_enquiries_referred_faculty ON enquiries(referred_faculty_id)
    WHERE referred_faculty_id IS NOT NULL;
