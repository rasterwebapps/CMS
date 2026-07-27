-- Faculty availability blocks for the timetable swap feature. Rows represent BLOCKED/UNAVAILABLE
-- windows only -- a faculty member with no rows is assumed fully available. Time-range based
-- (not FK'd to periods/lab_slots) so one table covers both theory periods and lab slots, matching
-- how TimetableGenerationService.Occupied already reasons about day+time overlap.

CREATE TABLE faculty_availability (
    id BIGSERIAL PRIMARY KEY,
    faculty_id BIGINT NOT NULL REFERENCES faculty(id) ON DELETE CASCADE,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_faculty_availability_slot UNIQUE (faculty_id, day_of_week, start_time, end_time)
);

CREATE INDEX idx_faculty_availability_faculty_day ON faculty_availability(faculty_id, day_of_week);
