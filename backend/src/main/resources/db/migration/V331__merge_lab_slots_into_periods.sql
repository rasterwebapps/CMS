-- R3 Phase 1: merge LabSlot into the Period master so Theory and Lab sessions share one
-- generic time axis instead of two independently-configured, unrelated time-window lists —
-- that split is what caused the timetable grid to render more columns than the configured
-- period count (Lab Slot windows rendered as extra columns alongside Period columns).
-- lab_slots itself is left in place, unreferenced, rather than dropped -- nothing reads it
-- after this migration, but dropping a table outright is a needless extra irreversible step.

-- 1. Copy every distinct lab_slot time-window that isn't already a period, deduped on
--    (start_time, end_time). A name collision with an existing period gets a ' (Lab)' suffix
--    since periods.name is UNIQUE.
INSERT INTO periods (name, start_time, end_time, duration_minutes, period_order, is_active, created_at, updated_at)
SELECT
    CASE WHEN EXISTS (SELECT 1 FROM periods p2 WHERE p2.name = ls.name)
         THEN ls.name || ' (Lab)'
         ELSE ls.name
    END,
    ls.start_time,
    ls.end_time,
    EXTRACT(EPOCH FROM (ls.end_time - ls.start_time)) / 60,
    NULL,
    COALESCE(ls.is_active, TRUE),
    NOW(),
    NOW()
FROM lab_slots ls
WHERE NOT EXISTS (
    SELECT 1 FROM periods p WHERE p.start_time = ls.start_time AND p.end_time = ls.end_time
);

-- 2. Backfill class_schedules.period_id for every LAB row (which so far only carries
--    lab_slot_id), matching through the lab slot's time window onto the now-merged period.
UPDATE class_schedules cs
SET period_id = p.id
FROM lab_slots ls
JOIN periods p ON p.start_time = ls.start_time AND p.end_time = ls.end_time
WHERE cs.lab_slot_id = ls.id
  AND cs.period_id IS NULL;

-- 3. Renumber period_order chronologically now that lab-derived periods are merged in --
--    period_order was always just a display sequence, never business-meaningful.
UPDATE periods p
SET period_order = ranked.rn
FROM (
    SELECT id, ROW_NUMBER() OVER (ORDER BY start_time) AS rn FROM periods
) ranked
WHERE p.id = ranked.id;

-- 4. LAB rows carry period_id like THEORY rows now -- lab_slot_id is fully superseded.
ALTER TABLE class_schedules DROP CONSTRAINT chk_class_schedule_session_shape;
ALTER TABLE class_schedules ADD CONSTRAINT chk_class_schedule_session_shape CHECK (
  (session_type = 'LAB'    AND lab_id IS NOT NULL AND period_id IS NOT NULL) OR
  (session_type = 'THEORY' AND classroom_id IS NOT NULL AND period_id IS NOT NULL)
);

ALTER TABLE class_schedules DROP COLUMN lab_slot_id;
