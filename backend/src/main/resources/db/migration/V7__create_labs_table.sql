CREATE TABLE labs (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255)                NOT NULL,
    lab_type      VARCHAR(255)                NOT NULL,
    speciality_id BIGINT                      NOT NULL REFERENCES specialities(id),
    building      VARCHAR(255),
    room_number   VARCHAR(255),
    capacity      INTEGER,
    status        VARCHAR(255)                NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE    NOT NULL
);
