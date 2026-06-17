-- V222: Seed COMMISSION_VIEW and COMMISSION_MANAGE permissions.

INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('COMMISSION_VIEW',   'View Commission Explorer',    'FINANCE', CURRENT_TIMESTAMP),
    ('COMMISSION_MANAGE', 'Record Commission Payouts',   'FINANCE', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN: full management
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('COMMISSION_VIEW', 'COMMISSION_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- FRONT_OFFICE, CASHIER: view + request payment (COMMISSION_VIEW only)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('FRONT_OFFICE', 'CASHIER')
  AND p.code = 'COMMISSION_VIEW'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
