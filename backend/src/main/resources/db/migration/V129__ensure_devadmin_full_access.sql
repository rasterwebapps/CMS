-- V129: Guarantee DEV_ADMIN and SUPPORT_ADMIN hold every permission
-- currently in the system.
--
-- Context: V88 granted all permissions that existed at migration time.
-- Subsequent migrations (V95, V103, etc.) added new permission codes and
-- explicitly re-granted them to DEV_ADMIN. This catch-all ensures no
-- permission is ever silently missing from either platform admin role,
-- regardless of migration execution order or future additions.
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

-- Ensure the devadmin app user exists, is active, and is linked to DEV_ADMIN.
INSERT INTO app_users (
    keycloak_username, email, full_name,
    app_role_id, is_active, created_by, created_at, updated_at
)
SELECT
    'devadmin',
    'devadmin@cms.local',
    'Developer Administrator',
    r.id,
    TRUE,
    'system',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM app_roles r
WHERE r.name = 'DEV_ADMIN'
ON CONFLICT (keycloak_username) DO UPDATE
    SET app_role_id = EXCLUDED.app_role_id,
        is_active   = TRUE,
        updated_at  = CURRENT_TIMESTAMP;
