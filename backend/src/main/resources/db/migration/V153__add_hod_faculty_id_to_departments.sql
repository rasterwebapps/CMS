-- Add HOD faculty FK column to specialities table
-- Allows linking a speciality's Head of Speciality to a faculty record
ALTER TABLE specialities
    ADD COLUMN IF NOT EXISTS hod_faculty_id BIGINT REFERENCES faculty(id) ON DELETE SET NULL;

