-- Courses with NULL admission_number_code get a placeholder '???'.
-- After migration, open Course Master and update any course showing '???' with the correct code.
UPDATE courses SET admission_number_code = '???' WHERE admission_number_code IS NULL OR admission_number_code = '';
ALTER TABLE courses ALTER COLUMN admission_number_code SET NOT NULL;
