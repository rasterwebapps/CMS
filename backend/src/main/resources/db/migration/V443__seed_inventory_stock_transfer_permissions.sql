-- Permissions for the new Stock Transfer workflow. No approve/confirm split this time — unlike
-- Goods Receipt (brings new stock into the system) or Purchase Order force-close (an audit-worthy
-- early stop), completing a transfer only moves stock that's already accounted for between two
-- locations the same MANAGE permission already controls, so it stays under MANAGE alone. See
-- docs/inventory-management/DECISION_LOG.md's "Stock Transfer slice" entry.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_STOCK_TRANSFER_VIEW',    'View Stock Transfers',   'MASTER', 'Stock Transfers', CURRENT_TIMESTAMP),
    ('INVENTORY_STOCK_TRANSFER_MANAGE',  'Manage Stock Transfers', 'MASTER', 'Stock Transfers', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_STOCK_TRANSFER_VIEW', 'INVENTORY_STOCK_TRANSFER_MANAGE')
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
