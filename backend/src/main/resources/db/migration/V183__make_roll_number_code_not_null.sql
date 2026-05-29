-- Courses with NULL roll_number_code get a placeholder '??'.
-- After migration, open Course Master and update any course showing '??' with the correct 2-digit code.
UPDATE courses SET roll_number_code = '??' WHERE roll_number_code IS NULL OR roll_number_code = '';
ALTER TABLE courses ALTER COLUMN roll_number_code SET NOT NULL;
