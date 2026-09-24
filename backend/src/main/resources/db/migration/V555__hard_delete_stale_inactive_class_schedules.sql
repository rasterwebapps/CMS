-- Backfill companion to TimetableGlobalAutoScheduleService.purgeDraftCellsForRebuild's switch
-- from soft-delete to hard-delete: every prior Global Auto-Schedule rebuild flipped a DRAFT
-- cell's is_active to false instead of deleting it, so any environment that has ever run
-- automation carries an unbounded, ever-growing set of dead is_active=false class_schedules rows.
-- Nothing reads them -- every real consumer of "this term's timetable" already goes through the
-- ...AndIsActiveTrue repository finders -- and they were even leaking into the GET /lab-schedules
-- API through a handful of unfiltered finders (now fixed alongside this migration). Scoped
-- strictly to is_active = false rows; no is_active = true row is ever touched. Mirrors
-- purgeOccurrencesForCells/purgeRotationRowsForCells's own bottom-up cleanup order so it can
-- never trip an FK it didn't already know to clear first. Idempotent: a re-run finds nothing left
-- to do, since every row this touches is gone after the first run.

-- 1) Unswap any session_occurrences row OUTSIDE this batch that still points at one INSIDE it as
--    its swap partner. swap_partner_occurrence_id (V329) has no ON DELETE clause, so an untouched
--    external pointer would otherwise block step 2's delete. A row fully inside the batch (both
--    swap partners being deleted together) needs no unswapping -- step 2 deletes both sides in one
--    statement, and PostgreSQL's own FK trigger sees that within the same statement.
UPDATE session_occurrences ext
SET swap_partner_occurrence_id = NULL
WHERE ext.swap_partner_occurrence_id IN (
    SELECT so.id
    FROM session_occurrences so
    JOIN class_schedules cs ON cs.id = so.class_schedule_id
    WHERE cs.is_active = false
)
AND (
    ext.class_schedule_id IS NULL
    OR ext.class_schedule_id NOT IN (SELECT id FROM class_schedules WHERE is_active = false)
);

-- 2) Delete the occurrences themselves. session_occurrence_units cascades away with its parent
--    occurrence automatically (ON DELETE CASCADE, V324).
DELETE FROM session_occurrences
WHERE class_schedule_id IN (SELECT id FROM class_schedules WHERE is_active = false);

-- 3) Rotation cleanup, bottom-up -- mirrors purgeRotationRowsForCells exactly: assignments, then
--    slots, then any group (and its members) a slot deletion above left with zero remaining slots.
--    The affected-group snapshot is taken BEFORE the slot delete, same as the Java version's
--    affectedGroupIds, so only groups actually touched by this batch are considered for removal.
CREATE TEMP TABLE tmp_v555_affected_rotation_groups AS
SELECT DISTINCT rs.rotation_group_id AS id
FROM rotation_slots rs
JOIN class_schedules cs ON cs.id = rs.class_schedule_id
WHERE cs.is_active = false;

DELETE FROM rotation_member_assignments
WHERE rotation_slot_id IN (
    SELECT rs.id
    FROM rotation_slots rs
    JOIN class_schedules cs ON cs.id = rs.class_schedule_id
    WHERE cs.is_active = false
);

DELETE FROM rotation_slots
WHERE class_schedule_id IN (SELECT id FROM class_schedules WHERE is_active = false);

DELETE FROM rotation_members
WHERE rotation_group_id IN (
    SELECT g.id FROM tmp_v555_affected_rotation_groups g
    WHERE NOT EXISTS (SELECT 1 FROM rotation_slots rs WHERE rs.rotation_group_id = g.id)
);

DELETE FROM rotation_groups
WHERE id IN (
    SELECT g.id FROM tmp_v555_affected_rotation_groups g
    WHERE NOT EXISTS (SELECT 1 FROM rotation_slots rs WHERE rs.rotation_group_id = g.id)
);

DROP TABLE tmp_v555_affected_rotation_groups;

-- 4) Finally, the class_schedules rows themselves -- safe now that nothing references them.
DELETE FROM class_schedules WHERE is_active = false;
