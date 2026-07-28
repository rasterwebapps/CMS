-- Periods are now configured by start time + duration (minutes) rather than start+end time
-- entered independently; end_time stays as a stored, derived column since generation/swap/
-- conflict-checking already read it directly (see ClassScheduleRepository.findOverlapping).
-- Backfill existing rows from their current start/end time before the column becomes NOT NULL.

ALTER TABLE periods ADD COLUMN duration_minutes INTEGER;

UPDATE periods
SET duration_minutes = EXTRACT(EPOCH FROM (end_time - start_time)) / 60
WHERE duration_minutes IS NULL;

ALTER TABLE periods ALTER COLUMN duration_minutes SET NOT NULL;
