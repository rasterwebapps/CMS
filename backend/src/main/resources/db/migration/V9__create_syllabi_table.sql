CREATE TABLE syllabi (
    id               BIGSERIAL PRIMARY KEY,
    course_id        BIGINT                      NOT NULL REFERENCES courses(id),
    version          INTEGER                     NOT NULL,
    theory_hours     INTEGER,
    lab_hours        INTEGER,
    tutorial_hours   INTEGER,
    objectives       VARCHAR(2000),
    content          VARCHAR(4000),
    text_books       VARCHAR(2000),
    reference_books  VARCHAR(2000),
    course_outcomes  VARCHAR(2000),
    is_active        BOOLEAN,
    created_at       TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE    NOT NULL
);
