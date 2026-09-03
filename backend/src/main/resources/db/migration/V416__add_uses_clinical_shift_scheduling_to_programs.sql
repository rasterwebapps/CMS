-- Per-program opt-in for off-campus Clinical Shift scheduling. When enabled, Timetable
-- auto-scheduling treats each cohort's Clinical Shift wall-clock window (including bus travel
-- buffer) as a hard block against on-campus Theory/Lab periods on that day. Default false is a
-- no-op for every existing program; only nursing-style programs opt in explicitly via the form.
ALTER TABLE programs
  ADD COLUMN uses_clinical_shift_scheduling BOOLEAN NOT NULL DEFAULT false;
