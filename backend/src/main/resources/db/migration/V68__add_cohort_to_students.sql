ALTER TABLE students ADD COLUMN cohort_id BIGINT REFERENCES cohorts(id);
ALTER TABLE students ADD COLUMN expected_graduation_term_instance_id BIGINT REFERENCES term_instances(id);
CREATE INDEX idx_students_cohort ON students(cohort_id);
