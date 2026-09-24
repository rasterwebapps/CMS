-- Companion to V556 for the three global timetable.faculty_max_*_hours System Configuration rows
-- (seeded blank by V370, since configured with real hour values in some environments) -- renames
-- them to the new *_sessions keys FacultyWorkloadRulesService/TimetableStaffingService now read,
-- and resets their value to "not configured" for the same reason V556 didn't try to migrate the
-- designation/faculty hour columns: hours and sessions aren't convertible (a Theory row is 1
-- period, a Clinical block can be 4), so there is no honest number to carry over. Also seeds the
-- new minWeeklySessions floor key, blank (V370's own convention for "not configured yet").
DELETE FROM system_configurations WHERE config_key IN (
    'timetable.faculty_max_daily_hours',
    'timetable.faculty_max_weekly_hours',
    'timetable.faculty_max_continuous_hours'
);

INSERT INTO system_configurations (config_key, config_value, description, data_type, category, is_editable, created_at, updated_at)
VALUES
    ('timetable.faculty_max_daily_sessions', '',
     'Maximum sessions a faculty member can be staffed for in a single day. Blank or 0 = no cap.',
     'INTEGER', 'TIMETABLE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('timetable.faculty_max_weekly_sessions', '',
     'Maximum sessions a faculty member can be staffed for across the whole term timetable in one week. Blank or 0 = no cap.',
     'INTEGER', 'TIMETABLE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('timetable.faculty_max_continuous_sessions', '',
     'Maximum unbroken back-to-back sessions a faculty member can be staffed for in a single day. Blank or 0 = no cap.',
     'INTEGER', 'TIMETABLE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('timetable.faculty_min_weekly_sessions', '',
     'Advisory floor: a faculty member below this many sessions/week shows as under-loaded in Assign Faculty and the Faculty Workload report. Never blocks staffing. Blank or 0 = no floor.',
     'INTEGER', 'TIMETABLE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (config_key) DO NOTHING;
