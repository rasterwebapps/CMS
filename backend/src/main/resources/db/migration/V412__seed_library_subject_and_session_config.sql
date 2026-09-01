-- Library gap-fill sessions (Run Automation) need a real Subject row because class_schedules.subject_id
-- is NOT NULL, but Library is not curriculum-driven -- one shared, institution-wide Subject, never
-- attached to any curriculum_term_courses/course_offerings row (course_offering_id stays null on every
-- Library class_schedules row). term_number=0/credits=0 are sentinel values: this subject never appears
-- in a curriculum term listing, which all filter by a real term_number/curriculum_version.
INSERT INTO subjects (name, code, credits, theory_credits, lab_credits, term_number, is_active,
                       lab_session_block_periods, clinical_session_block_periods, created_at, updated_at)
VALUES ('Library', 'SYSTEM-LIBRARY', 0, 0, 0, 0, true, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO system_configurations (config_key, config_value, description, data_type, category, is_editable, created_at, updated_at)
VALUES
    ('timetable.library_sessions_per_week', '2',
     'How many Library gap-fill sessions Run Automation places per cohort per week, before Self-Study fills the rest.',
     'INTEGER', 'TIMETABLE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('timetable.library_block_size_periods', '2',
     'How many contiguous periods one Library gap-fill session occupies.',
     'INTEGER', 'TIMETABLE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (config_key) DO NOTHING;
