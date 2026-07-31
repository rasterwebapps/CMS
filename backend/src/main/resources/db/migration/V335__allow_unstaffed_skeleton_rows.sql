-- R3 Phase 4: the new skeleton builder places period+session-type+subject first, with faculty
-- and room assigned later by the Phase 5 staffing pass -- so a row must be allowed to exist with
-- no faculty/room while it's still a DRAFT skeleton. period_id (the placement itself) stays
-- mandatory always; faculty and the type-appropriate room only become mandatory once the row is
-- actually PUBLISHED (live), enforced here at the database level rather than just in application
-- code. Every pre-existing row is already fully staffed (the old auto-generator never produced
-- an unstaffed row), so this is a pure loosening -- no backfill needed.

ALTER TABLE class_schedules ALTER COLUMN faculty_id DROP NOT NULL;

ALTER TABLE class_schedules DROP CONSTRAINT chk_class_schedule_session_shape;
ALTER TABLE class_schedules ADD CONSTRAINT chk_class_schedule_session_shape CHECK (
  period_id IS NOT NULL
  AND (
    status <> 'PUBLISHED'
    OR (
      faculty_id IS NOT NULL
      AND (
        (session_type = 'LAB'      AND lab_id IS NOT NULL) OR
        (session_type = 'THEORY'   AND classroom_id IS NOT NULL) OR
        (session_type = 'CLINICAL' AND clinical_venue_id IS NOT NULL)
      )
    )
  )
);
