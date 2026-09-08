-- Permissions for the two new Inventory Catalog master screens (Categories, Units of Measure).
-- Codes checked against the legacy INVENTORY_VIEW/INVENTORY_CREATE/... codes (old lab-consumables
-- feature) for collision — none; see docs/inventory-management/DECISION_LOG.md 2026-09-07 entry.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_CATEGORY_VIEW',   'View Item Categories',    'MASTER', 'Item Categories',     CURRENT_TIMESTAMP),
    ('INVENTORY_CATEGORY_MANAGE', 'Manage Item Categories',  'MASTER', 'Item Categories',     CURRENT_TIMESTAMP),
    ('INVENTORY_UOM_VIEW',        'View Units of Measure',   'MASTER', 'Units of Measure',    CURRENT_TIMESTAMP),
    ('INVENTORY_UOM_MANAGE',      'Manage Units of Measure', 'MASTER', 'Units of Measure',    CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_CATEGORY_VIEW', 'INVENTORY_CATEGORY_MANAGE', 'INVENTORY_UOM_VIEW', 'INVENTORY_UOM_MANAGE')
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
