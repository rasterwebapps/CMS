-- Permission for the "Regenerate Product Codes" admin action (bulk-reassigns every product's code
-- to the new <CategoryShortCode>-<sequence> pattern, e.g. STA-000001) — its own operation-wise
-- permission per the mandatory pattern, separate from _MANAGE: this is a distinct, audit-worthy,
-- mass-rewrite action, not an ordinary single-product edit. Defaults to the same tier as
-- INVENTORY_PRODUCT_MANAGE (closest match), matching INVENTORY_STOCK_INDENT_AUTO_RUN (V536).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_PRODUCT_REGENERATE_CODES', 'Regenerate Product Codes', 'MASTER', 'Products', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code = 'INVENTORY_PRODUCT_REGENERATE_CODES'
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
