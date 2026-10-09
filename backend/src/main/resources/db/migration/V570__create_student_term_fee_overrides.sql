-- V570: Per-student, per-term fee amount overrides. Set when an admin edits a specific future
-- term's fee during a Day Scholar <-> Hosteler switch (or any later correction) and persists
-- going forward until changed again. Keyed by semester_number (not term_instance_id) because a
-- future term's TermInstance row frequently doesn't exist yet when the override is set -- see
-- FeeDemandServiceImpl.deriveFeeTotalAmount / StudentService.executeBoardingStatusSwitch.

CREATE TABLE student_term_fee_overrides (
    id               BIGSERIAL     PRIMARY KEY,
    student_id       BIGINT        NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    semester_number  INTEGER       NOT NULL,
    override_amount  NUMERIC(12,2) NOT NULL,
    set_by           VARCHAR(255),
    set_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (student_id, semester_number)
);

CREATE INDEX idx_stfo_student_id ON student_term_fee_overrides(student_id);
