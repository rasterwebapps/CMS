-- Permission for the new Disposal action — its own operation-wise permission, separate from
-- _MANAGE, per the operation-wise permission mapping rule (disposing an asset is a distinct,
-- audit-worthy, terminal action, same reasoning as Purchase Order's force-close and Goods
-- Receipt's confirm). Already flagged as reserved in the "Asset register slice" decision-log
-- entry — created now that the Disposal slice itself is built.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_ASSET_DISPOSE', 'Dispose Assets', 'MASTER', 'Assets', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code = 'INVENTORY_ASSET_DISPOSE'
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
