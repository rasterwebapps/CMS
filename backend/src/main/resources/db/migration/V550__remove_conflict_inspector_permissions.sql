-- Conflict Inspector screen retired: OC-260 already moved Publish's conflict gate to a per-cohort
-- "Check & Resolve Conflicts" action in Timetable Builder (CohortConflictAcknowledgment), leaving
-- Conflict Inspector's own term-wide scan/acknowledge screen (V379, V528) with no other consumer --
-- its acknowledgment never fed any other gate (TermInstanceService's term-advance checklist never
-- read conflict_acknowledged_at) and its GET /acknowledgment-status endpoint had no frontend caller
-- left. role_permissions.permission_id has ON DELETE CASCADE (V87), so no separate cleanup needed
-- there. term_instances.conflict_acknowledged_at/conflict_acknowledged_cell_count columns (V528)
-- are deliberately left in place, unused -- dropping columns is out of scope for this cleanup.
DELETE FROM permissions WHERE code IN ('TIMETABLE_CONFLICT_INSPECTOR_VIEW', 'TIMETABLE_CONFLICT_INSPECTOR_ACKNOWLEDGE');
