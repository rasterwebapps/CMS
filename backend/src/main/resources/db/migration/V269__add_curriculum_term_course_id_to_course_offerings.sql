-- Direct join from a concrete term's CourseOffering back to the curriculum-mapping row it was
-- generated from. Needed to resolve per-offering attendance thresholds and elective status from
-- a CourseOffering without re-deriving the (curriculum_version_id, term_number, subject_id) match
-- every time. Nullable and backfilled best-effort: the unique-constraint shape on both tables
-- guarantees a 1:1 match for existing rows, but is left nullable in case a curriculum mapping was
-- since edited/removed out from under an older offering.

ALTER TABLE course_offerings ADD COLUMN curriculum_term_course_id BIGINT REFERENCES curriculum_term_courses(id);

CREATE INDEX idx_course_offerings_curriculum_term_course_id ON course_offerings(curriculum_term_course_id);

UPDATE course_offerings co
SET curriculum_term_course_id = ctc.id
FROM curriculum_term_courses ctc
WHERE ctc.curriculum_version_id = co.curriculum_version_id
  AND ctc.term_number = co.term_number
  AND ctc.subject_id = co.subject_id
  AND co.curriculum_term_course_id IS NULL;
