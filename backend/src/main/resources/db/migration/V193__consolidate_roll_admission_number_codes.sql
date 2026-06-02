-- Consolidate admission_number_code into roll_number_code.
-- Both fields served identical purpose (embedded in student numbers).
-- roll_number_code is kept as the single source of truth.
-- ApplicationNumberSequenceService now reads roll_number_code for admission numbers.
ALTER TABLE courses DROP COLUMN admission_number_code;
