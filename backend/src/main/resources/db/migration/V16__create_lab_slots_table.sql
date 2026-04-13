CREATE TABLE lab_slots (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)                NOT NULL,
    start_time  TIME                        NOT NULL,
    end_time    TIME                        NOT NULL,
    slot_order  INTEGER,
    is_active   BOOLEAN,
    created_at  TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE    NOT NULL
);
