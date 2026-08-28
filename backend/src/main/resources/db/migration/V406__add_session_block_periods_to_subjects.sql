-- How many consecutive periods one single Lab/Clinical session must occupy for this
-- subject (e.g. a 3-hour lab runs as 3 back-to-back periods on the same day, never as
-- 3 independent single-period placements scattered across the week). Consumed by
-- TimetableGlobalAutoScheduleService's auto-scheduler via ClassSchedule's existing
-- periodSpan mechanism (sessionGroupId/spanPeriodIds -- see V331/OC-127), which already
-- supports multi-period placement; this just tells the auto-scheduler how many periods
-- to group per session instead of always placing one at a time. Default 1 = today's
-- existing behavior for every subject (independent single-period placements) --
-- fully backward compatible, no data migration needed. Not applicable to THEORY.

ALTER TABLE subjects ADD COLUMN lab_session_block_periods INTEGER NOT NULL DEFAULT 1;
ALTER TABLE subjects ADD COLUMN clinical_session_block_periods INTEGER NOT NULL DEFAULT 1;
