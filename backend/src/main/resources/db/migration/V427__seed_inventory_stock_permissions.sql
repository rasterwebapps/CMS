-- Permissions for the new Inventory Location master and the Stock Balance/Record Movement screens.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_LOCATION_VIEW',   'View Inventory Locations',   'MASTER', 'Inventory Locations', CURRENT_TIMESTAMP),
    ('INVENTORY_LOCATION_MANAGE', 'Manage Inventory Locations', 'MASTER', 'Inventory Locations', CURRENT_TIMESTAMP),
    ('INVENTORY_STOCK_VIEW',      'View Stock Balances',        'MASTER', 'Stock Balance',        CURRENT_TIMESTAMP),
    ('INVENTORY_STOCK_MANAGE',    'Record Stock Movements',     'MASTER', 'Stock Balance',        CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_LOCATION_VIEW', 'INVENTORY_LOCATION_MANAGE', 'INVENTORY_STOCK_VIEW', 'INVENTORY_STOCK_MANAGE')
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
