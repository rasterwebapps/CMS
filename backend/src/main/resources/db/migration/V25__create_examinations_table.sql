CREATE TABLE examinations (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)                NOT NULL,
    course_id   BIGINT                      NOT NULL REFERENCES courses(id),
    exam_type   VARCHAR(255)                NOT NULL,
    date        DATE,
    duration    INTEGER,
    max_marks   INTEGER,
    semester_id BIGINT                      REFERENCES semesters(id),
    created_at  TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE    NOT NULL
);
