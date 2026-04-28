CREATE TABLE fee_demands (
    id                              BIGSERIAL PRIMARY KEY,
    student_term_enrollment_id      BIGINT NOT NULL REFERENCES student_term_enrollments(id),
    term_instance_id                BIGINT NOT NULL REFERENCES term_instances(id),
    academic_year_id                BIGINT NOT NULL REFERENCES academic_years(id),
    total_amount                    NUMERIC(12, 2) NOT NULL,
    due_date                        DATE NOT NULL,
    paid_amount                     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status                          VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (student_term_enrollment_id)
);

CREATE INDEX idx_fd_term_instance ON fee_demands(term_instance_id);
CREATE INDEX idx_fd_status ON fee_demands(status);
