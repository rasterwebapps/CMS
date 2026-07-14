-- Subjects are no longer owned by a single Course. A subject (e.g. "English", "Nursing
-- Foundations") is often taught identically across multiple courses/programs, and reused
-- across multiple terms of the same curriculum with different hours per term
-- (curriculum_term_courses already supports this). The course/program context for a
-- subject now comes entirely from which curriculum version(s) reference it via
-- curriculum_term_courses -> curriculum_versions.course_id, not from an intrinsic
-- ownership field on subjects itself.

ALTER TABLE subjects DROP COLUMN course_id;
