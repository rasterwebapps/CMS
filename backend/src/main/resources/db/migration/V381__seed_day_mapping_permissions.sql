-- Permissions for the new Day Mapping Overrides section (Academic Calendar day-detail flyout) --
-- own dedicated permissions per the per-operation rule, even though the screen is shared with
-- Calendar Events/Blocked Periods. Default tier matches ACADEMIC_CALENDAR_VIEW/
-- ACADEMIC_CALENDAR_MANAGE, the closest existing equivalent (same anchors V348 used for
-- BLOCKED_PERIOD_VIEW/MANAGE).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_DAY_MAPPING_VIEW', 'View Day Mapping Overrides', 'CURRICULUM', 'Academic Calendar', CURRENT_TIMESTAMP),
    ('TIMETABLE_DAY_MAPPING_MANAGE', 'Manage Day Mapping Overrides', 'CURRICULUM', 'Academic Calendar', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'ACADEMIC_CALENDAR_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_DAY_MAPPING_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'ACADEMIC_CALENDAR_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_DAY_MAPPING_MANAGE') new_p
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
