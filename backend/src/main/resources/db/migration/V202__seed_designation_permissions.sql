-- V202: Seed DESIGNATION_VIEW and DESIGNATION_MANAGE permissions.

INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('DESIGNATION_VIEW',   'View Designations',   'MASTER', CURRENT_TIMESTAMP),
    ('DESIGNATION_MANAGE', 'Manage Designations', 'MASTER', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN: full designation management
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('DESIGNATION_VIEW', 'DESIGNATION_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- FACULTY, FRONT_OFFICE, CASHIER: read-only (needed to render designation selects in forms)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('FACULTY', 'FRONT_OFFICE', 'CASHIER')
  AND p.code = 'DESIGNATION_VIEW'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
