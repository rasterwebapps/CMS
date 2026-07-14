-- Optional course scope for curriculum versions. When set, a curriculum version applies
-- only to that specific course (e.g. MSc Nursing (Adult) vs MSc Nursing (Child), which
-- share the same Program but need independent, non-overlapping subject curricula). When
-- left NULL, the version applies program-wide, preserving existing single-course-per-program
-- behaviour (e.g. BSc Nursing, the only course under the Bachelor program).

ALTER TABLE curriculum_versions ADD COLUMN course_id BIGINT REFERENCES courses(id);

CREATE INDEX idx_curriculum_versions_course_id ON curriculum_versions(course_id);
