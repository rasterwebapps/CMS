-- Permissions for the new per-location Reorder Configuration screen (Phase B of the Stock Indent
-- auto-indent feature — see docs/inventory-management/DECISION_LOG.md's 2026-09-21 entry). View/
-- Manage split per the operation-wise permission mapping rule; no separate Approve action here —
-- unlike Stock Indent itself, saving a reorder config has no approval step, it's a plain master
-- record. Tier defaulted to the same standard role set every other Stock/Inventory master uses
-- (V437, V443, V447).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_REORDER_CONFIG_VIEW',   'View Reorder Configuration',   'MASTER', 'Reorder Configuration', CURRENT_TIMESTAMP),
    ('INVENTORY_REORDER_CONFIG_MANAGE', 'Manage Reorder Configuration', 'MASTER', 'Reorder Configuration', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_REORDER_CONFIG_VIEW', 'INVENTORY_REORDER_CONFIG_MANAGE')
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
