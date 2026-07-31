-- Permissions for the two new Room Purpose Classification master screens.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('ROOM_PURPOSE_CATEGORY_VIEW',   'View Room Purpose Categories',   'MASTER', 'Room Purpose Categories', CURRENT_TIMESTAMP),
    ('ROOM_PURPOSE_CATEGORY_MANAGE', 'Manage Room Purpose Categories', 'MASTER', 'Room Purpose Categories', CURRENT_TIMESTAMP),
    ('ROOM_SUB_TYPE_VIEW',           'View Room Sub-Types',            'MASTER', 'Room Sub-Types',          CURRENT_TIMESTAMP),
    ('ROOM_SUB_TYPE_MANAGE',         'Manage Room Sub-Types',          'MASTER', 'Room Sub-Types',          CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('ROOM_PURPOSE_CATEGORY_VIEW', 'ROOM_PURPOSE_CATEGORY_MANAGE', 'ROOM_SUB_TYPE_VIEW', 'ROOM_SUB_TYPE_MANAGE')
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
