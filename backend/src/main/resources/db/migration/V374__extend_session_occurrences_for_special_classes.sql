-- BR-55: Special/Remedial Class Scheduler. Extends session_occurrences (rather than a new table)
-- to also carry ad-hoc, single-date sessions that have no backing recurring ClassSchedule row --
-- a single-subject special class, or one row of a whole-day-repeat batch (see request_batch_id).
-- The existing regular-session path (class_schedule_id NOT NULL) is completely untouched: the
-- original unique constraint on (class_schedule_id, occurrence_date) still protects it, and
-- Postgres' NULL <> NULL semantics naturally exempt every special-class row from that constraint,
-- which is why a separate partial unique index (below) is needed for the special-class case.

ALTER TABLE session_occurrences ALTER COLUMN class_schedule_id DROP NOT NULL;

ALTER TABLE session_occurrences
    ADD COLUMN subject_id              BIGINT REFERENCES subjects(id),
    ADD COLUMN course_offering_id      BIGINT REFERENCES course_offerings(id),
    ADD COLUMN cohort_section_id       BIGINT REFERENCES cohort_sections(id),
    ADD COLUMN period_id               BIGINT REFERENCES periods(id),
    ADD COLUMN session_type            VARCHAR(20),
    ADD COLUMN classroom_id            BIGINT REFERENCES classrooms(id),
    ADD COLUMN lab_id                  BIGINT REFERENCES labs(id),
    ADD COLUMN clinical_venue_id       BIGINT REFERENCES clinical_venues(id),
    ADD COLUMN requested_faculty_id    BIGINT REFERENCES faculty(id),
    ADD COLUMN occurrence_source       VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    ADD COLUMN approval_status         VARCHAR(20),
    ADD COLUMN requested_by_faculty_id BIGINT REFERENCES faculty(id),
    ADD COLUMN requested_at            TIMESTAMPTZ,
    ADD COLUMN request_reason          VARCHAR(500),
    ADD COLUMN source_day_of_week      VARCHAR(10),
    ADD COLUMN request_batch_id        UUID,
    ADD COLUMN approved_by             VARCHAR(255),
    ADD COLUMN approved_at             TIMESTAMPTZ,
    ADD COLUMN rejection_reason        VARCHAR(500);

-- Shape guard: a REGULAR row must keep pointing at its ClassSchedule (unchanged contract); a
-- SPECIAL_CLASS/DAY_REPEAT row must never have one, and must carry the minimum fields needed to
-- place and display it (subject/period/session type) plus a real approval status.
ALTER TABLE session_occurrences ADD CONSTRAINT chk_session_occurrences_special_shape CHECK (
    (occurrence_source = 'REGULAR' AND class_schedule_id IS NOT NULL)
    OR (
        occurrence_source IN ('SPECIAL_CLASS', 'DAY_REPEAT')
        AND class_schedule_id IS NULL
        AND subject_id IS NOT NULL
        AND period_id IS NOT NULL
        AND session_type IS NOT NULL
        AND approval_status IS NOT NULL
    )
);

-- Duplicate-request guard for special-class rows only: blocks resubmitting the same
-- date/period/subject/cohort while a prior request is still PENDING or APPROVED. Rejected or
-- cancelled rows must not block resubmission, so they're excluded from the index.
CREATE UNIQUE INDEX ux_session_occurrences_special_slot
    ON session_occurrences (occurrence_date, period_id, subject_id, COALESCE(cohort_section_id, -1))
    WHERE class_schedule_id IS NULL AND approval_status NOT IN ('REJECTED', 'CANCELLED');

CREATE INDEX idx_session_occurrences_special_date_source ON session_occurrences (occurrence_date, occurrence_source);
CREATE INDEX idx_session_occurrences_requested_by ON session_occurrences (requested_by_faculty_id);
CREATE INDEX idx_session_occurrences_request_batch ON session_occurrences (request_batch_id);
