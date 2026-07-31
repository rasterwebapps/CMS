-- Faculty absence marking (Timetable planner Round 2, Phase 6) -- date-specific, unlike
-- faculty_availability (V313) which is recurring-weekly-only. No existing HR/leave module to
-- integrate with (FacultyStatus.ON_LEAVE is a coarse lifecycle enum value, not a dated record),
-- so this is a genuinely new, standalone subsystem. recorded_by is a plain string, matching the
-- existing approvedBy pattern on fee_refunds/student_scholarships/onebook_payment_requests --
-- not a new audit-FK convention.

CREATE TABLE faculty_absences (
    id            BIGSERIAL PRIMARY KEY,
    faculty_id    BIGINT NOT NULL REFERENCES faculty(id),
    absence_date  DATE NOT NULL,
    reason        VARCHAR(500),
    recorded_by   VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_faculty_absences_faculty_date UNIQUE (faculty_id, absence_date)
);

CREATE INDEX idx_faculty_absences_faculty ON faculty_absences(faculty_id);
