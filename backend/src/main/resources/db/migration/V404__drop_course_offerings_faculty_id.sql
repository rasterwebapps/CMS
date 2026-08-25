-- Retired: every offering+cohort assignment now lives in course_offering_section_faculty
-- (generalized in V402, backfilled in V403) -- a single scalar per offering couldn't represent
-- more than one cohort sharing an offering being assigned independently. Verified no remaining
-- Java references before writing this DROP, per the migration column-verification gate.
ALTER TABLE course_offerings DROP COLUMN faculty_id;
