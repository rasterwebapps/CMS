-- Optional restriction of a subject-in-term row to one specific course under the curriculum
-- version's program. Null means the row applies to every course under that program. This is
-- how variants that share one program-wide curriculum (e.g. MSc Nursing Adult vs Child) are
-- distinguished now that subjects are no longer owned by a single course.

ALTER TABLE curriculum_term_courses ADD COLUMN course_id BIGINT REFERENCES courses(id);
