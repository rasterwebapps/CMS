-- V455 bundled Asset Maintenance Schedules and Service Contracts under one shared permission
-- pair (INVENTORY_ASSET_MAINTENANCE_VIEW/MANAGE), violating the operation-wise permission
-- mapping rule (every distinct screen/operation gets its own dedicated permission). This
-- migration gives Service Contracts its own dedicated pair and backfills them onto every role
-- that currently holds the maintenance permissions, so no role silently loses Service Contract
-- access as a side effect of the split. AssetMaintenanceScheduleController/nav-config/app.routes
-- keep INVENTORY_ASSET_MAINTENANCE_VIEW/MANAGE unchanged for the Maintenance Schedules screen.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_ASSET_SERVICE_CONTRACT_VIEW',   'View Asset Service Contracts',   'MASTER', 'Asset Service Contracts', CURRENT_TIMESTAMP),
    ('INVENTORY_ASSET_SERVICE_CONTRACT_MANAGE', 'Manage Asset Service Contracts', 'MASTER', 'Asset Service Contracts', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Backfill: any role holding the old maintenance VIEW/MANAGE permission gets the matching new
-- service-contract permission, preserving prior effective access across the split.
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, p_new.id
FROM role_permissions rp
JOIN permissions p_old ON p_old.id = rp.permission_id
JOIN permissions p_new ON p_new.code = CASE p_old.code
    WHEN 'INVENTORY_ASSET_MAINTENANCE_VIEW' THEN 'INVENTORY_ASSET_SERVICE_CONTRACT_VIEW'
    WHEN 'INVENTORY_ASSET_MAINTENANCE_MANAGE' THEN 'INVENTORY_ASSET_SERVICE_CONTRACT_MANAGE'
END
WHERE p_old.code IN ('INVENTORY_ASSET_MAINTENANCE_VIEW', 'INVENTORY_ASSET_MAINTENANCE_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp2
      WHERE rp2.role_id = rp.role_id AND rp2.permission_id = p_new.id
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
