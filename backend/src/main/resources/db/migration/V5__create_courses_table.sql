CREATE TABLE courses (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255)                NOT NULL,
    code            VARCHAR(255)                NOT NULL UNIQUE,
    credits         INTEGER                     NOT NULL,
    theory_credits  INTEGER                     NOT NULL,
    lab_credits     INTEGER                     NOT NULL,
    program_id      BIGINT                      NOT NULL REFERENCES programs(id),
    semester        INTEGER                     NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE    NOT NULL
);
