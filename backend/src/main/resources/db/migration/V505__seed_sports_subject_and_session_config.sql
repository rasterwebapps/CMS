-- Sports sessions (Run Automation, 2026-09-15): a weekly games/PE block per cohort section, placed
-- after curriculum and Library in the last free periods of a day. Like Library it is not curriculum
-- data -- one shared, institution-wide Subject (course_offering_id stays null on every Sports
-- class_schedules row) -- but unlike Library it is TAUGHT: the PE faculty are whoever the admin ticks
-- on this subject's eligible-faculty list (subject_eligible_faculty), and the venue is a classroom
-- whose Room is tagged with the SPORTS purpose category. term_number=0/credits=0 are the same
-- sentinels V412 uses so it never appears in a curriculum term listing.
INSERT INTO subjects (name, code, credits, theory_credits, lab_credits, term_number, is_active,
                       lab_session_block_periods, clinical_session_block_periods, created_at, updated_at)
VALUES ('Sports', 'SYSTEM-SPORTS', 0, 0, 0, 0, true, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO system_configurations (config_key, config_value, description, data_type, category, is_editable, created_at, updated_at)
VALUES
    ('timetable.sports_sessions_per_week', '1',
     'How many Sports sessions Run Automation places per cohort section per week, after curriculum and Library.',
     'INTEGER', 'TIMETABLE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('timetable.sports_block_size_periods', '2',
     'How many contiguous periods one Sports session occupies (placed in the last free periods of a day where possible).',
     'INTEGER', 'TIMETABLE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (config_key) DO NOTHING;

-- A published Sports row needs its PE faculty and its Sports-tagged classroom, exactly like a
-- Theory row. Every other branch is unchanged, so every existing row still satisfies the rule.
ALTER TABLE class_schedules DROP CONSTRAINT IF EXISTS chk_class_schedule_session_shape;
ALTER TABLE class_schedules ADD CONSTRAINT chk_class_schedule_session_shape CHECK (
    period_id IS NOT NULL AND (
        status <> 'PUBLISHED'
        OR (session_type = 'LIBRARY' AND classroom_id IS NOT NULL)
        OR (faculty_id IS NOT NULL AND (
            (session_type = 'LAB' AND lab_id IS NOT NULL)
            OR (session_type = 'THEORY' AND classroom_id IS NOT NULL)
            OR (session_type = 'CLINICAL' AND clinical_venue_id IS NOT NULL)
            OR (session_type = 'SPORTS' AND classroom_id IS NOT NULL)
        ))
    )
);
