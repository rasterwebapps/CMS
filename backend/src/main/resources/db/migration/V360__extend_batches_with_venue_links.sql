-- Extends batches with an optional physical-venue link (Lab or ClinicalVenue, mirroring
-- the mutually-exclusive shape of class_schedules' session-type venue columns) and
-- traceability back to the CohortRoomAllocation that created it, so a revert can find
-- and deactivate exactly the batches it produced without touching manually-created ones.
ALTER TABLE batches ADD COLUMN lab_id BIGINT REFERENCES labs(id);
ALTER TABLE batches ADD COLUMN clinical_venue_id BIGINT REFERENCES clinical_venues(id);
ALTER TABLE batches ADD COLUMN cohort_room_allocation_id BIGINT REFERENCES cohort_room_allocations(id);

ALTER TABLE batches ADD CONSTRAINT chk_batch_venue_exclusive
    CHECK (lab_id IS NULL OR clinical_venue_id IS NULL);

CREATE INDEX idx_batches_lab_id ON batches(lab_id);
CREATE INDEX idx_batches_clinical_venue_id ON batches(clinical_venue_id);
CREATE INDEX idx_batches_cohort_room_allocation_id ON batches(cohort_room_allocation_id);