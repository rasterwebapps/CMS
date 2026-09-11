-- Permissions for the new Product Variant screens (embedded under a Product's own edit page, not
-- a standalone top-level master — same tier reasoning as ProductUomChainController's own comment).
-- Codes checked against existing INVENTORY_* permission codes for collision — none.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_PRODUCT_VARIANT_VIEW',   'View Product Variants',   'MASTER', 'Product Variants', CURRENT_TIMESTAMP),
    ('INVENTORY_PRODUCT_VARIANT_MANAGE', 'Manage Product Variants', 'MASTER', 'Product Variants', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_PRODUCT_VARIANT_VIEW', 'INVENTORY_PRODUCT_VARIANT_MANAGE')
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
