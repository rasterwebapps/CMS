-- OC-127 gap-closure follow-up (periodSpan): links N consecutive-period ClassSchedule rows that
-- together represent one multi-period session (e.g. a 2-period lab), so they can be placed/staffed/
-- removed atomically as one unit instead of independently. Null for every ordinary single-period
-- session (the overwhelming majority of rows) -- no behavior change for existing single-period
-- conflict checks, which already operate per-row against each row's own Period start/end time.
ALTER TABLE class_schedules ADD COLUMN session_group_id UUID;

CREATE INDEX idx_class_schedules_session_group_id ON class_schedules(session_group_id);
