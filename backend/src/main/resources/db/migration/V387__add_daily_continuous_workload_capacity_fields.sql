-- Extends the weekly-only advisory capacity fields (V373) so Daily and Continuous teaching-hour
-- caps get the same per-faculty-override-then-designation-default resolution as Weekly already
-- has. All four nullable, same "not configured" semantics as V373.
ALTER TABLE designations ADD COLUMN default_daily_teaching_hours INTEGER;
ALTER TABLE designations ADD COLUMN default_continuous_teaching_hours INTEGER;
ALTER TABLE faculty ADD COLUMN planned_daily_hours_override INTEGER;
ALTER TABLE faculty ADD COLUMN planned_continuous_hours_override INTEGER;
