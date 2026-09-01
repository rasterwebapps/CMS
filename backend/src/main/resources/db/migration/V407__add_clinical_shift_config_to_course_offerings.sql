-- OC-175/OC-177: per-offering configurable clinical shift duration + travel buffer, for
-- off-campus clinical postings whose real hours are clock-time driven (7am-1pm, 1pm-7pm, ...)
-- rather than the standard ~50min on-campus Period grid. Both nullable: NULL means this offering
-- has no shift-based clinical component and keeps using the existing Period-based path untouched.
ALTER TABLE course_offerings ADD COLUMN clinical_shift_duration_minutes INTEGER;
ALTER TABLE course_offerings ADD COLUMN clinical_travel_buffer_minutes INTEGER;

ALTER TABLE course_offerings ADD CONSTRAINT chk_co_clinical_shift_duration_positive
    CHECK (clinical_shift_duration_minutes IS NULL OR clinical_shift_duration_minutes > 0);
ALTER TABLE course_offerings ADD CONSTRAINT chk_co_clinical_travel_buffer_non_negative
    CHECK (clinical_travel_buffer_minutes IS NULL OR clinical_travel_buffer_minutes >= 0);
