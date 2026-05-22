-- V169: Re-run catch-all permission grant for DEV_ADMIN and SUPPORT_ADMIN.
--
-- V129 ran this same pattern at migration time, but V154 added
-- DOCUMENT_VERIFICATION_MANAGE without granting it to platform admin roles.
-- This migration closes that gap and acts as a standing safety net for any
-- future permission additions that miss an explicit DEV_ADMIN grant.
--
-- Idempotent: the NOT EXISTS guard prevents duplicate rows.

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
CROSS JOIN permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
