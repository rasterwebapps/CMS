CREATE TABLE attendances (
    id         BIGSERIAL PRIMARY KEY,
    student_id BIGINT                      NOT NULL REFERENCES students(id),
    course_id  BIGINT                      NOT NULL REFERENCES courses(id),
    date       DATE                        NOT NULL,
    status     VARCHAR(255)                NOT NULL,
    type       VARCHAR(255)                NOT NULL,
    remarks    VARCHAR(255),
    marked_by  BIGINT                      REFERENCES faculty(id),
    created_at TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE    NOT NULL
);
