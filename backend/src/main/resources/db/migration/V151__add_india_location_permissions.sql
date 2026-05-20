-- V151: Add RBAC permissions for India Location master management.

INSERT INTO permissions (code, display_name, category)
SELECT 'INDIA_LOCATION_VIEW', 'View India Locations', 'MASTER'
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = 'INDIA_LOCATION_VIEW');

INSERT INTO permissions (code, display_name, category)
SELECT 'INDIA_LOCATION_MANAGE', 'Manage India Locations', 'MASTER'
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = 'INDIA_LOCATION_MANAGE');

-- Grant to system admins and COLLEGE_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
JOIN permissions p ON p.code IN ('INDIA_LOCATION_VIEW', 'INDIA_LOCATION_MANAGE')
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Grant view-only to FRONT_OFFICE and FACULTY (they use the dropdowns)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
JOIN permissions p ON p.code = 'INDIA_LOCATION_VIEW'
WHERE r.name IN ('FRONT_OFFICE', 'FACULTY')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

