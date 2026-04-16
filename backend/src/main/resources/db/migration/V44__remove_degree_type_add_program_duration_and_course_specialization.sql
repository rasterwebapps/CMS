-- Add duration_years to programs table (moved from courses)
ALTER TABLE programs ADD COLUMN duration_years INTEGER NOT NULL DEFAULT 1;

-- Add specialization to courses table
ALTER TABLE courses ADD COLUMN specialization VARCHAR(255);

-- Remove degree_type and duration_years from courses table
ALTER TABLE courses DROP COLUMN degree_type;
ALTER TABLE courses DROP COLUMN duration_years;
