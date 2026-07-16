-- Curriculum Version's course scope (added nullable in V264 to support a "program-wide"
-- version) is now mandatory — every curriculum version must be tied to one specific course,
-- eliminating the shared/program-wide pattern that caused confusion about which curriculum
-- applied to a given cohort. Backfill any existing NULL course_id to the program's lowest-id
-- course (deterministic; all pre-existing data is test/dummy, confirmed safe to backfill this
-- way) before enforcing NOT NULL. A program with zero courses has no valid course to backfill
-- to and will correctly fail this migration rather than silently leave bad data — that is the
-- desired stop condition, not a bug in this migration.
UPDATE curriculum_versions cv
SET course_id = (SELECT MIN(c.id) FROM courses c WHERE c.program_id = cv.program_id)
WHERE cv.course_id IS NULL;

ALTER TABLE curriculum_versions ALTER COLUMN course_id SET NOT NULL;

-- The per-row course restriction on curriculum_term_courses (V278) existed to let one
-- program-wide curriculum version carve out course-specific exceptions for a handful of
-- subjects. Now that every version is itself course-scoped, a row's course is always
-- unambiguously its parent version's course, so this column is pure duplication.
ALTER TABLE curriculum_term_courses DROP COLUMN course_id;
