-- OC-175/OC-177: a third session_occurrences shape for CLINICAL_SHIFT rows, generated from a
-- ClinicalShiftGroup's clinical block (per linked Batch, one row each) or shared theory block
-- (one row, cohort-scoped). These bypass period_id entirely -- the shift's real clock times don't
-- align to the standard Period grid -- so block_start_time/block_end_time carry the time directly.
ALTER TABLE session_occurrences
    ADD COLUMN block_start_time TIME,
    ADD COLUMN block_end_time   TIME,
    ADD COLUMN batch_id         BIGINT REFERENCES batches(id);

CREATE INDEX idx_session_occurrences_batch ON session_occurrences(batch_id);

-- Widen the BR-55 shape guard (V374) to also allow CLINICAL_SHIFT: no class_schedule_id, no
-- period_id (bypassed), a real block time range, and an audience -- batch_id for a CLINICAL block
-- (one row per venue), cohort_section_id for a shared THEORY block (the reconvened full roster).
ALTER TABLE session_occurrences DROP CONSTRAINT chk_session_occurrences_special_shape;

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
    OR (
        occurrence_source = 'CLINICAL_SHIFT'
        AND class_schedule_id IS NULL
        AND period_id IS NULL
        AND session_type IS NOT NULL
        AND course_offering_id IS NOT NULL
        AND block_start_time IS NOT NULL
        AND block_end_time IS NOT NULL
        AND (
            (session_type = 'CLINICAL' AND batch_id IS NOT NULL)
            OR (session_type = 'THEORY' AND cohort_section_id IS NOT NULL AND batch_id IS NULL)
        )
    )
);
