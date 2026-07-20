-- Period master for THEORY session scheduling. Mirrors the existing `lab_slots` table shape.
-- Unlike LabSlot (which has a backend endpoint but no admin screen and no dedicated
-- permissions), Period gets a full admin screen since generation needs curated period data
-- and there was no prior "1st period 9:00-9:50" concept anywhere in this system for lectures.

CREATE TABLE periods (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL UNIQUE,
    start_time    TIME NOT NULL,
    end_time      TIME NOT NULL,
    period_order  INTEGER,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL
);
