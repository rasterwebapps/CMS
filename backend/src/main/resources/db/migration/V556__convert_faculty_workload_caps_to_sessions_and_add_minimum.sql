-- Converts the advisory faculty workload cap system (V373, V387) from hours to a raw weekly-
-- Period-count unit ("sessions"), and adds a new minimum-sessions floor alongside the existing
-- maximum. Hours and sessions are not convertible (a Theory row is 1 period, a Clinical block can
-- be 4) -- there is no honest formula to turn an existing hour-based limit into a session count,
-- so this does not attempt to migrate values. Every faculty/designation that had a real configured
-- hour-based limit goes back to "not configured" (null) until an admin re-enters a session-based
-- number -- same "not configured -> gate doesn't enforce" semantics V373 already established, so
-- nothing silently defaults to 0 and flags everyone as over-capacity.
--
-- Actual worked HOURS (for INC/university reporting) are never stored here -- they're computed on
-- demand from each real scheduled session's own Period.startTime/endTime, so that figure is always
-- exact and never goes stale.
ALTER TABLE designations ADD COLUMN default_weekly_teaching_sessions INTEGER;
ALTER TABLE designations ADD COLUMN default_daily_teaching_sessions INTEGER;
ALTER TABLE designations ADD COLUMN default_continuous_teaching_sessions INTEGER;
ALTER TABLE designations ADD COLUMN default_min_weekly_sessions INTEGER;

ALTER TABLE faculty ADD COLUMN planned_weekly_sessions_override INTEGER;
ALTER TABLE faculty ADD COLUMN planned_daily_sessions_override INTEGER;
ALTER TABLE faculty ADD COLUMN planned_continuous_sessions_override INTEGER;
ALTER TABLE faculty ADD COLUMN planned_min_weekly_sessions_override INTEGER;

ALTER TABLE designations DROP COLUMN default_weekly_teaching_hours;
ALTER TABLE designations DROP COLUMN default_daily_teaching_hours;
ALTER TABLE designations DROP COLUMN default_continuous_teaching_hours;

ALTER TABLE faculty DROP COLUMN planned_weekly_hours_override;
ALTER TABLE faculty DROP COLUMN planned_daily_hours_override;
ALTER TABLE faculty DROP COLUMN planned_continuous_hours_override;
