-- Add roll_number_code to courses for roll number generation
ALTER TABLE courses ADD COLUMN IF NOT EXISTS roll_number_code VARCHAR(10);

-- Add university registration number and UMIS number to students
ALTER TABLE students ADD COLUMN IF NOT EXISTS university_registration_number VARCHAR(50) UNIQUE;
ALTER TABLE students ADD COLUMN IF NOT EXISTS umis_number VARCHAR(50) UNIQUE;
