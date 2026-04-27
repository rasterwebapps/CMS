CREATE TABLE student_term_enrollments (
    id                  BIGSERIAL PRIMARY KEY,
    student_id          BIGINT NOT NULL REFERENCES students(id),
    term_instance_id    BIGINT NOT NULL REFERENCES term_instances(id),
    cohort_id           BIGINT NOT NULL REFERENCES cohorts(id),
    semester_number     INTEGER NOT NULL,
    year_of_study       INTEGER NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (student_id, term_instance_id)
);
CREATE INDEX idx_ste_term_instance ON student_term_enrollments(term_instance_id);
CREATE INDEX idx_ste_student ON student_term_enrollments(student_id);
CREATE INDEX idx_ste_cohort ON student_term_enrollments(cohort_id);
