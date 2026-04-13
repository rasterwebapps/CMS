CREATE TABLE programs (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255)                NOT NULL,
    code            VARCHAR(255)                NOT NULL UNIQUE,
    degree_type     VARCHAR(255)                NOT NULL,
    duration_years  INTEGER                     NOT NULL,
    department_id   BIGINT                      NOT NULL REFERENCES departments(id),
    created_at      TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE    NOT NULL
);
