-- Informational-only backup/co-instructor note on a CourseOffering — never eligible for
-- staffing/substitution, never gets its own ClassSchedule rows, no scheduling logic reads it.
-- Mirrors the existing faculty_id column exactly: nullable, no FK.
ALTER TABLE course_offerings ADD COLUMN secondary_faculty_id BIGINT;
