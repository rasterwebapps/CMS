-- OC-175/OC-177: a ClinicalShiftGroup is the top-level recurring shift unit ("Shift A -- Morning
-- Clinical"). Several Batch rows -- each with its own existing lab_id/clinical_venue_id -- link to
-- the SAME group when they run clinical in parallel at different off-campus venues under the same
-- shift window; the group's students reconvene into one shared theory class afterward (or before),
-- captured by clinical_shift_theory_blocks. Clinical block end time is derived at read time from
-- course_offerings.clinical_shift_duration_minutes, not stored redundantly here.
CREATE TABLE clinical_shift_groups (
    id                   BIGSERIAL PRIMARY KEY,
    course_offering_id   BIGINT NOT NULL REFERENCES course_offerings(id),
    cohort_section_id    BIGINT REFERENCES cohort_sections(id),
    term_instance_id     BIGINT NOT NULL REFERENCES term_instances(id),
    label                VARCHAR(150) NOT NULL,
    day_of_week          VARCHAR(20) NOT NULL,
    clinical_start_time  TIME NOT NULL,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_clinical_shift_groups_offering ON clinical_shift_groups(course_offering_id);
CREATE INDEX idx_clinical_shift_groups_term ON clinical_shift_groups(term_instance_id);
CREATE INDEX idx_clinical_shift_groups_cohort_section ON clinical_shift_groups(cohort_section_id);

-- The shared, reconvened theory block(s) for a shift group -- exactly one row per position
-- (sequence_order), never one per batch. Theory may run before or after the clinical block
-- (the two example shift patterns run it on opposite sides).
CREATE TABLE clinical_shift_theory_blocks (
    id                   BIGSERIAL PRIMARY KEY,
    shift_group_id       BIGINT NOT NULL REFERENCES clinical_shift_groups(id),
    sequence_order       INTEGER NOT NULL,
    start_time           TIME NOT NULL,
    end_time             TIME NOT NULL,
    subject_id           BIGINT NOT NULL REFERENCES subjects(id),
    classroom_id         BIGINT REFERENCES classrooms(id),
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_shift_theory_block_time_order CHECK (end_time > start_time),
    UNIQUE (shift_group_id, sequence_order)
);

CREATE INDEX idx_clinical_shift_theory_blocks_group ON clinical_shift_theory_blocks(shift_group_id);

-- Several batches (different venues) can point at the same shift group; a batch outside any shift
-- (on-campus-only clinical, or not yet configured) leaves this null.
ALTER TABLE batches ADD COLUMN clinical_shift_group_id BIGINT REFERENCES clinical_shift_groups(id);
CREATE INDEX idx_batches_clinical_shift_group_id ON batches(clinical_shift_group_id);
