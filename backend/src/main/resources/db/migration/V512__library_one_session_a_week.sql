-- Library is ONE 2-period session per section a week (user's call, 2026-09-15), not the two V412
-- seeded. A second session is now a bonus Run Automation adds only when the section's week still has
-- at least timetable.library_extra_session_min_free_periods free periods after curriculum, Library
-- and Sports. The quota change is guarded on V412's seed value, so it is a no-op wherever it has
-- already been changed.
UPDATE system_configurations
SET config_value = '1',
    updated_at = CURRENT_TIMESTAMP
WHERE config_key = 'timetable.library_sessions_per_week'
  AND config_value = '2';

UPDATE system_configurations
SET description = 'How many Library sessions Run Automation must place per cohort section per week (the required quota), before Sports and the extra-hours filler.',
    updated_at = CURRENT_TIMESTAMP
WHERE config_key = 'timetable.library_sessions_per_week';

INSERT INTO system_configurations (config_key, config_value, description, data_type, category, is_editable, created_at, updated_at)
VALUES
    ('timetable.library_extra_session_min_free_periods', '8',
     'Run Automation gives a section one bonus Library session only when its week still has at least this many free periods after curriculum, Library and Sports.',
     'INTEGER', 'TIMETABLE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (config_key) DO NOTHING;
