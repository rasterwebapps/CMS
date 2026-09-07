-- A ClinicalShiftGroup previously recurred on its day_of_week for the ENTIRE term instance, with
-- no way to bound it to a shorter real-world window (e.g. a 4-week internship posting inside a
-- 26-week term). These two columns are optional: NULL/NULL (the default, and the only value every
-- pre-existing row has) means "whole term", matching today's behavior exactly. Both non-null means
-- the group is only real between those two dates -- used by hours-crediting
-- (TimetableSkeletonService#toClinicalShiftHours) and occurrence generation
-- (ClinicalShiftOccurrenceService#generateForDate) to avoid crediting/generating hours for weeks
-- the shift was never actually running. Note: the Skeleton Builder grid itself still renders the
-- group's day-of-week as blocked for the whole term -- it has no per-week/per-occurrence grid
-- model yet, so a date-bounded group's real end is only honored by these two consumers, not by
-- the recurring weekly template display. That gap is a known follow-up, not solved here.
ALTER TABLE clinical_shift_groups
    ADD COLUMN effective_start_date DATE,
    ADD COLUMN effective_end_date DATE;

ALTER TABLE clinical_shift_groups
    ADD CONSTRAINT chk_clinical_shift_group_date_range CHECK (
        (effective_start_date IS NULL AND effective_end_date IS NULL)
        OR (effective_start_date IS NOT NULL AND effective_end_date IS NOT NULL
            AND effective_end_date >= effective_start_date)
    );
