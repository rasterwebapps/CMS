CREATE TABLE semester_results (
    id BIGSERIAL PRIMARY KEY,
    student_term_enrollment_id BIGINT NOT NULL REFERENCES student_term_enrollments(id),
    total_max_marks NUMERIC(10, 2) NOT NULL,
    total_marks_obtained NUMERIC(10, 2) NOT NULL,
    percentage NUMERIC(5, 2) NOT NULL,
    result_status VARCHAR(20) NOT NULL DEFAULT 'NOT_PUBLISHED',
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_semester_results_enrollment UNIQUE (student_term_enrollment_id)
);
