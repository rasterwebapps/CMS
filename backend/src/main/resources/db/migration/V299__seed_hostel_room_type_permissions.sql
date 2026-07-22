-- Permissions for the new Hostel Room Type master screen — first screen of the Hostel module,
-- so no existing permission to auto-inherit from; granted explicitly to admin-tier roles,
-- matching the Designation/Period master pattern.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('HOSTEL_ROOM_TYPE_VIEW',   'View Hostel Room Types',   'MASTER', 'Hostel Room Types', CURRENT_TIMESTAMP),
    ('HOSTEL_ROOM_TYPE_MANAGE', 'Manage Hostel Room Types', 'MASTER', 'Hostel Room Types', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('HOSTEL_ROOM_TYPE_VIEW', 'HOSTEL_ROOM_TYPE_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
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
