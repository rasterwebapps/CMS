-- Syllabus previously duplicated Theory/Lab/Tutorial hours as its own free-typed fields,
-- keyed only by subject + a manual version number. Curriculum Map is now the single source
-- of truth for a subject's Theory/Lab/Clinical hours (which can legitimately differ per
-- curriculum version/term for the same subject), so a syllabus now links to one specific
-- curriculum_term_courses row instead of duplicating hours. The syllabi table is empty in
-- every environment this has shipped to (the only prior seed insert in V45 matched zero rows),
-- so no data migration is needed.

ALTER TABLE syllabi DROP COLUMN subject_id;
ALTER TABLE syllabi DROP COLUMN theory_hours;
ALTER TABLE syllabi DROP COLUMN lab_hours;
ALTER TABLE syllabi DROP COLUMN tutorial_hours;

ALTER TABLE syllabi
    ADD COLUMN curriculum_term_course_id BIGINT NOT NULL REFERENCES curriculum_term_courses(id);

ALTER TABLE syllabi
    ADD CONSTRAINT uq_syllabi_curriculum_term_course_version UNIQUE (curriculum_term_course_id, version);
