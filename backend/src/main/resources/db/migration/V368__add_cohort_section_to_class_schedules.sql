-- Optional link from a THEORY ClassSchedule row to the CohortSection sub-cohort it was placed
-- for, once a cohort's committed Theory room has been split into sections (see V364). Mirrors
-- how Batch.cohortSection already links LAB/CLINICAL batches to their section (V365). Nullable:
-- null means "whole cohort" (no committed CohortRoomAllocation for this cohort/term, or a row
-- placed before this column existed). LAB/CLINICAL rows never set this directly -- their section
-- scope is derived from their own Batch's cohortSection instead.
ALTER TABLE class_schedules ADD COLUMN cohort_section_id BIGINT REFERENCES cohort_sections(id);

CREATE INDEX idx_class_schedules_cohort_section_id ON class_schedules(cohort_section_id);
