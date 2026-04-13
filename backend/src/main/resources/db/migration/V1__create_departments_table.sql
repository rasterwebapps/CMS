CREATE TABLE departments (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)                NOT NULL,
    code        VARCHAR(255)                NOT NULL UNIQUE,
    description VARCHAR(1000),
    hod_name    VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE    NOT NULL
);
