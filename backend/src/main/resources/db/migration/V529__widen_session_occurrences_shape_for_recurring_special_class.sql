-- BR-55 follow-up: a weekly-recurring special class (same subject/venue/faculty, same weekday,
-- from a start date to an end date) is a fourth session_occurrences shape -- RECURRING_SPECIAL_CLASS
-- -- one row per eligible week, grouped by request_batch_id exactly like DAY_REPEAT. Shaped
-- identically to SPECIAL_CLASS/DAY_REPEAT (no class_schedule_id, subject/period/session_type/
-- approval_status all required), so it simply joins that existing bucket rather than adding a new
-- one.
ALTER TABLE session_occurrences DROP CONSTRAINT chk_session_occurrences_special_shape;

ALTER TABLE session_occurrences ADD CONSTRAINT chk_session_occurrences_special_shape CHECK (
    (occurrence_source = 'REGULAR' AND class_schedule_id IS NOT NULL)
    OR (
        occurrence_source IN ('SPECIAL_CLASS', 'DAY_REPEAT', 'RECURRING_SPECIAL_CLASS')
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
