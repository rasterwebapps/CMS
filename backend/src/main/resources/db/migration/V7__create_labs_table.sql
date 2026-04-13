CREATE TABLE labs (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255)                NOT NULL,
    lab_type      VARCHAR(255)                NOT NULL,
    department_id BIGINT                      NOT NULL REFERENCES departments(id),
    building      VARCHAR(255),
    room_number   VARCHAR(255),
    capacity      INTEGER,
    status        VARCHAR(255)                NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE    NOT NULL
);
