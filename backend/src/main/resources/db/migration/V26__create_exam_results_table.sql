CREATE TABLE exam_results (
    id             BIGSERIAL PRIMARY KEY,
    examination_id BIGINT                      NOT NULL REFERENCES examinations(id),
    student_id     BIGINT                      NOT NULL REFERENCES students(id),
    marks_obtained NUMERIC(10,2),
    grade          VARCHAR(255),
    status         VARCHAR(255),
    created_at     TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE    NOT NULL
);
