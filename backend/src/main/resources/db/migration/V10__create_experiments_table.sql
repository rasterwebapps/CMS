CREATE TABLE experiments (
    id                          BIGSERIAL PRIMARY KEY,
    course_id                   BIGINT                      NOT NULL REFERENCES courses(id),
    experiment_number           INTEGER                     NOT NULL,
    name                        VARCHAR(255)                NOT NULL,
    description                 VARCHAR(2000),
    aim                         VARCHAR(1000),
    apparatus                   VARCHAR(2000),
    procedure                   VARCHAR(4000),
    expected_outcome            VARCHAR(1000),
    learning_outcomes           VARCHAR(2000),
    estimated_duration_minutes  INTEGER,
    is_active                   BOOLEAN,
    created_at                  TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at                  TIMESTAMP WITH TIME ZONE    NOT NULL
);
