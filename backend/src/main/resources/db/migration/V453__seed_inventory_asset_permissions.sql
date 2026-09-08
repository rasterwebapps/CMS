-- Permissions for the new Asset register. VIEW/MANAGE only this slice — status changes and
-- edits both fall under MANAGE (no separate operation-wise split yet; Disposal (a later Phase 5
-- slice) gets its own INVENTORY_ASSET_DISPOSE permission when it's built, per the plan).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_ASSET_VIEW',    'View Assets',    'MASTER', 'Assets', CURRENT_TIMESTAMP),
    ('INVENTORY_ASSET_MANAGE',  'Manage Assets',  'MASTER', 'Assets', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_ASSET_VIEW', 'INVENTORY_ASSET_MANAGE')
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
