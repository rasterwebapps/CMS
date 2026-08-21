-- Permissions for per-section Theory faculty assignment, nested under Course Offering (edit
-- dialog). Auto-assigned to any role that already holds COURSE_VIEW/COURSE_MANAGE, mirroring how
-- V273 granted BATCH_VIEW/BATCH_MANAGE.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('SECTION_FACULTY_VIEW',   'View Section Faculty',   'MASTER', 'Course Offerings', CURRENT_TIMESTAMP),
    ('SECTION_FACULTY_MANAGE', 'Manage Section Faculty', 'MASTER', 'Course Offerings', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'COURSE_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'SECTION_FACULTY_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'COURSE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'SECTION_FACULTY_MANAGE') new_p
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
