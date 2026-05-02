-- Remove status column from admissions table
-- Admission is now just an enrollment record without workflow status
-- Student lifecycle is tracked via Student.status instead

ALTER TABLE admissions
    DROP COLUMN IF EXISTS status;

