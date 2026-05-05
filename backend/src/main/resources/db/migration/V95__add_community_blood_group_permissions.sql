-- V95: Add RBAC permission codes for Community & Blood Group master screens.
--
-- These masters were added in V92, but the UI nav is permission-driven.
-- Without explicit permission codes, the screens are hidden from the menu.

-- 1) Insert permission codes (idempotent)
INSERT INTO permissions (code, display_name, category)
SELECT 'COMMUNITY_VIEW', 'View Communities', 'MASTER'
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = 'COMMUNITY_VIEW');

INSERT INTO permissions (code, display_name, category)
SELECT 'COMMUNITY_MANAGE', 'Manage Communities', 'MASTER'
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = 'COMMUNITY_MANAGE');

INSERT INTO permissions (code, display_name, category)
SELECT 'BLOOD_GROUP_VIEW', 'View Blood Groups', 'MASTER'
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = 'BLOOD_GROUP_VIEW');

INSERT INTO permissions (code, display_name, category)
SELECT 'BLOOD_GROUP_MANAGE', 'Manage Blood Groups', 'MASTER'
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = 'BLOOD_GROUP_MANAGE');

-- 2) Grant new permissions to system admins + Admin
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
JOIN permissions p ON p.code IN (
    'COMMUNITY_VIEW',
    'COMMUNITY_MANAGE',
    'BLOOD_GROUP_VIEW',
    'BLOOD_GROUP_MANAGE'
)
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 3) Grant manage (and view) to COLLEGE_ADMIN so they can maintain the masters.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
JOIN permissions p ON p.code IN (
    'COMMUNITY_VIEW',
    'COMMUNITY_MANAGE',
    'BLOOD_GROUP_VIEW',
    'BLOOD_GROUP_MANAGE'
)
WHERE r.name = 'COLLEGE_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

