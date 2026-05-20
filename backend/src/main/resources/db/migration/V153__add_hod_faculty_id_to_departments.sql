-- Add HOD faculty FK column to departments table
-- Allows linking a department's Head of Department to a faculty record
ALTER TABLE departments
    ADD COLUMN IF NOT EXISTS hod_faculty_id BIGINT REFERENCES faculty(id) ON DELETE SET NULL;

