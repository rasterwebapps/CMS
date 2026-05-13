-- V128: Track student program transfers with consent and document reconciliation.
-- RETURNED status is stored as VARCHAR in existing document tables — no enum change required.

CREATE TABLE student_program_transfers (
    id              BIGSERIAL    PRIMARY KEY,
    student_id      BIGINT       NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    old_program_id  BIGINT       NOT NULL REFERENCES programs(id),
    new_program_id  BIGINT       NOT NULL REFERENCES programs(id),
    transferred_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    transferred_by  VARCHAR(255),
    consent_confirmed BOOLEAN    NOT NULL DEFAULT FALSE,
    notes           TEXT
);

CREATE INDEX idx_spt_student_id ON student_program_transfers(student_id);
