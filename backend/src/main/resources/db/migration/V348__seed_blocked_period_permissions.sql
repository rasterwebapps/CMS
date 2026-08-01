-- Permissions for the new Blocked Periods tab (Academic Calendar) -- own dedicated permissions
-- per the per-operation rule, even though the screen is shared with Calendar Events. Default tier
-- matches ACADEMIC_CALENDAR_VIEW/ACADEMIC_CALENDAR_MANAGE, the closest existing equivalent.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('BLOCKED_PERIOD_VIEW', 'View Blocked Periods', 'CURRICULUM', 'Academic Calendar', CURRENT_TIMESTAMP),
    ('BLOCKED_PERIOD_MANAGE', 'Manage Blocked Periods', 'CURRICULUM', 'Academic Calendar', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'ACADEMIC_CALENDAR_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'BLOCKED_PERIOD_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'ACADEMIC_CALENDAR_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'BLOCKED_PERIOD_MANAGE') new_p
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