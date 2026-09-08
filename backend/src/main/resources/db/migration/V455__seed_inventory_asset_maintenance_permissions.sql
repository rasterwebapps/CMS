-- Permissions for Asset Maintenance Schedules + Service Contracts. One permission pair covers
-- both entities in this slice (per the plan's own wording bundling them as one slice) — no
-- distinct audit-worthy action beyond ordinary manage yet.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_ASSET_MAINTENANCE_VIEW',    'View Asset Maintenance',    'MASTER', 'Asset Maintenance', CURRENT_TIMESTAMP),
    ('INVENTORY_ASSET_MAINTENANCE_MANAGE',  'Manage Asset Maintenance',  'MASTER', 'Asset Maintenance', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_ASSET_MAINTENANCE_VIEW', 'INVENTORY_ASSET_MAINTENANCE_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
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
