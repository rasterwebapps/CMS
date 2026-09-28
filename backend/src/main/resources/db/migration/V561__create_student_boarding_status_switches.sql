-- V561: History of Day Scholar <-> Hosteler switches, with the fee adjustment applied at
-- switch time (demandsAdjusted/feeDelta), so cashiers can see why a demand total changed.

CREATE TABLE student_boarding_status_switches (
    id                BIGSERIAL    PRIMARY KEY,
    student_id        BIGINT       NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    old_student_type  VARCHAR(20)  NOT NULL,
    new_student_type  VARCHAR(20)  NOT NULL,
    switched_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    switched_by       VARCHAR(255),
    remarks           TEXT,
    demands_adjusted  INTEGER      NOT NULL DEFAULT 0,
    fee_delta         NUMERIC(12,2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_sbss_student_id ON student_boarding_status_switches(student_id);
