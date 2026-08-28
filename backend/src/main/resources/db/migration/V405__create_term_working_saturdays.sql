-- A term's opt-in working-Saturday pattern: which nth-Saturday-of-the-month occurrences (FIRST,
-- SECOND, THIRD, FOURTH, LAST) count as real working days for scheduling. No rows for a term means
-- no restriction is configured -- existing behavior is unaffected until an admin explicitly picks
-- a pattern (see TimetableSkeletonService/ClassScheduleOccurrenceService's isSaturdayWorkingDay).
CREATE TABLE term_working_saturdays (
    term_instance_id BIGINT NOT NULL REFERENCES term_instances(id),
    week_of_month VARCHAR(20) NOT NULL,
    PRIMARY KEY (term_instance_id, week_of_month)
);

INSERT INTO permissions (code, display_name, category, screen_label, created_at, tier) VALUES
    ('TIMETABLE_WORKING_SATURDAYS_MANAGE', 'Manage Working Saturdays', 'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP, 4)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_SKELETON_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_WORKING_SATURDAYS_MANAGE') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- DEV_ADMIN / SUPPORT_ADMIN catch-all sync
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
CROSS JOIN permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
