-- V220: Seed STAFF_REFERRER_VIEW and STAFF_REFERRER_MANAGE permissions.

INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('STAFF_REFERRER_VIEW',   'View Staff Referrers',   'MASTER', CURRENT_TIMESTAMP),
    ('STAFF_REFERRER_MANAGE', 'Manage Staff Referrers', 'MASTER', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN: full management
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('STAFF_REFERRER_VIEW', 'STAFF_REFERRER_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- FRONT_OFFICE, CASHIER: read-only (needed to render staff dropdown on enquiry form)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('FRONT_OFFICE', 'CASHIER')
  AND p.code = 'STAFF_REFERRER_VIEW'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
