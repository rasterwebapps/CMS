-- Ensure DEV_ADMIN holds every permission in the system.
-- V88 did the initial bulk grant, but permissions added in later migrations
-- were only granted if each migration explicitly included DEV_ADMIN.
-- This catch-all closes any gaps without touching existing assignments.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'DEV_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
