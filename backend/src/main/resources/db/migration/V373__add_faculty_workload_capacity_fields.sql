-- Advisory faculty capacity-planning fields (deferred initiative from the timetable engine
-- roadmap) -- both nullable so the workload report can honestly show "not configured" per
-- faculty instead of silently defaulting to 0 and flagging everyone as over-capacity.
ALTER TABLE designations ADD COLUMN default_weekly_teaching_hours INTEGER;
ALTER TABLE faculty ADD COLUMN planned_weekly_hours_override INTEGER;
