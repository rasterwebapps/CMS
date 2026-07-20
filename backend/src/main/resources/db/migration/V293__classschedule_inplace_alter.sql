-- Generalizes lab_schedules into class_schedules, covering both THEORY and LAB/CLINICAL
-- sessions, as an in-place additive alter (not a parallel table + data migration) per the
-- agreed low-risk migration approach for the timetable generator. Every pre-existing row is
-- a LAB session that was already live, so session_type backfills to 'LAB' and status backfills
-- to 'PUBLISHED' before either column is made NOT NULL/gets a default enforced going forward.
--
-- Ordering matters: the session_type backfill runs BEFORE the CHECK constraint is added, so
-- every pre-existing row already satisfies it (they all have lab_id/lab_slot_id NOT NULL from
-- before this migration, per V7/V9's original table definition).

ALTER TABLE lab_schedules RENAME TO class_schedules;

ALTER TABLE class_schedules ADD COLUMN session_type VARCHAR(20);
UPDATE class_schedules SET session_type = 'LAB';
ALTER TABLE class_schedules ALTER COLUMN session_type SET NOT NULL;

ALTER TABLE class_schedules ADD COLUMN status VARCHAR(20);
UPDATE class_schedules SET status = 'PUBLISHED';
ALTER TABLE class_schedules ALTER COLUMN status SET NOT NULL;
ALTER TABLE class_schedules ALTER COLUMN status SET DEFAULT 'PUBLISHED';

ALTER TABLE class_schedules ADD COLUMN classroom_id BIGINT REFERENCES classrooms(id);
ALTER TABLE class_schedules ADD COLUMN period_id BIGINT REFERENCES periods(id);
ALTER TABLE class_schedules ADD COLUMN course_offering_id BIGINT REFERENCES course_offerings(id);

-- THEORY rows won't have a lab/lab-slot/batch-name; LAB rows won't have a classroom/period.
ALTER TABLE class_schedules ALTER COLUMN lab_id DROP NOT NULL;
ALTER TABLE class_schedules ALTER COLUMN lab_slot_id DROP NOT NULL;
ALTER TABLE class_schedules ALTER COLUMN batch_name DROP NOT NULL;

ALTER TABLE class_schedules ADD CONSTRAINT chk_class_schedule_session_shape CHECK (
  (session_type = 'LAB'    AND lab_id IS NOT NULL AND lab_slot_id IS NOT NULL) OR
  (session_type = 'THEORY' AND classroom_id IS NOT NULL AND period_id IS NOT NULL)
);

CREATE INDEX idx_class_schedules_classroom_id ON class_schedules(classroom_id);
CREATE INDEX idx_class_schedules_period_id ON class_schedules(period_id);
CREATE INDEX idx_class_schedules_course_offering_id ON class_schedules(course_offering_id);
CREATE INDEX idx_class_schedules_term_status ON class_schedules(term_instance_id, status, session_type);
