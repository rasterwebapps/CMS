CREATE TABLE academic_years (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)                NOT NULL UNIQUE,
    start_date  DATE                        NOT NULL,
    end_date    DATE                        NOT NULL,
    is_current  BOOLEAN                     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE    NOT NULL
);
