INSERT INTO system_configurations (config_key, config_value, description, data_type, category, is_editable, created_at, updated_at)
VALUES
    ('timetable.faculty_max_daily_hours', '',
     'Maximum teaching hours a faculty member can be staffed for in a single day. Blank or 0 = no cap.',
     'DECIMAL', 'TIMETABLE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('timetable.faculty_max_weekly_hours', '',
     'Maximum teaching hours a faculty member can be staffed for across the whole term timetable in one week. Blank or 0 = no cap.',
     'DECIMAL', 'TIMETABLE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('timetable.faculty_max_continuous_hours', '',
     'Maximum unbroken back-to-back teaching hours a faculty member can be staffed for in a single day. Blank or 0 = no cap.',
     'DECIMAL', 'TIMETABLE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (config_key) DO NOTHING;
