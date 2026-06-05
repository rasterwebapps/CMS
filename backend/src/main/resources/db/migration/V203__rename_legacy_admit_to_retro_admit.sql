UPDATE permissions SET code = 'RETRO_ADMIT', name = 'Retro Admit' WHERE code = 'LEGACY_ADMIT';

-- Sync DEV_ADMIN and SUPPORT_ADMIN to hold every permission (catches all gaps since V129)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
CROSS JOIN permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
