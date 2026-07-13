-- INC nursing curriculum compliance: per-semester Theory/Lab/Clinical contact hours and subject
-- classification. Hours and subject type live on the curriculum-term mapping (not the Subject
-- master) because the same subject's hours and INC category can differ by the semester/curriculum
-- it is mapped into (e.g. Nursing Foundations is Theory/Lab-heavy in Term I, Clinical-heavy in
-- Term II). All new columns default to zero/CORE/non-elective so existing mapping rows remain
-- valid without a backfill.

ALTER TABLE curriculum_term_courses ADD COLUMN theory_hours INTEGER NOT NULL DEFAULT 0;
ALTER TABLE curriculum_term_courses ADD COLUMN lab_hours INTEGER NOT NULL DEFAULT 0;
ALTER TABLE curriculum_term_courses ADD COLUMN clinical_hours INTEGER NOT NULL DEFAULT 0;
ALTER TABLE curriculum_term_courses ADD COLUMN subject_type VARCHAR(20) NOT NULL DEFAULT 'CORE';
ALTER TABLE curriculum_term_courses ADD COLUMN is_elective BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE curriculum_term_courses ADD COLUMN elective_group_id BIGINT REFERENCES curriculum_elective_groups(id);

CREATE INDEX idx_curriculum_term_courses_elective_group_id ON curriculum_term_courses(elective_group_id);
