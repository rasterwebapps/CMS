-- R3 Phase 2: CLINICAL becomes a real session type, distinct from LAB, with its own venue.
-- Existing rows are all THEORY/LAB (CLINICAL never existed as a storable value before this),
-- so no backfill is needed -- this is a pure additive change to the CHECK constraint.

ALTER TABLE class_schedules ADD COLUMN clinical_venue_id BIGINT REFERENCES clinical_venues(id);
CREATE INDEX idx_class_schedules_clinical_venue_id ON class_schedules(clinical_venue_id);

ALTER TABLE class_schedules DROP CONSTRAINT chk_class_schedule_session_shape;
ALTER TABLE class_schedules ADD CONSTRAINT chk_class_schedule_session_shape CHECK (
  (session_type = 'LAB'      AND lab_id IS NOT NULL AND period_id IS NOT NULL) OR
  (session_type = 'THEORY'   AND classroom_id IS NOT NULL AND period_id IS NOT NULL) OR
  (session_type = 'CLINICAL' AND clinical_venue_id IS NOT NULL AND period_id IS NOT NULL)
);
