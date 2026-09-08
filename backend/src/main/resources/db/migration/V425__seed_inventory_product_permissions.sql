-- Permissions for the new Product master screen. CategoryAttribute has no permission codes of its
-- own — it's edited only in the context of a Category and reuses INVENTORY_CATEGORY_VIEW/MANAGE,
-- see docs/inventory-management/DECISION_LOG.md 2026-09-07 "Product slice" entry.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_PRODUCT_VIEW',   'View Products',   'MASTER', 'Products', CURRENT_TIMESTAMP),
    ('INVENTORY_PRODUCT_MANAGE', 'Manage Products', 'MASTER', 'Products', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_PRODUCT_VIEW', 'INVENTORY_PRODUCT_MANAGE')
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
