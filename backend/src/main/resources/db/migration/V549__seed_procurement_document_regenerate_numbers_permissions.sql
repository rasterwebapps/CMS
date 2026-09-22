-- Permissions for the "Regenerate Numbers" admin action on each of the 4 newly-numbered
-- procurement/receiving documents (Quotation Request, Purchase Order, Goods Receipt, Return to
-- Supplier) — each its own operation-wise permission, defaulting to the same tier as that
-- screen's own MANAGE permission (closest match), matching INVENTORY_PRODUCT_REGENERATE_CODES
-- (V545).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_QUOTATION_REGENERATE_NUMBERS',      'Regenerate Quotation Numbers',     'MASTER', 'Quotation Requests', CURRENT_TIMESTAMP),
    ('INVENTORY_PURCHASE_ORDER_REGENERATE_NUMBERS', 'Regenerate Purchase Order Numbers','MASTER', 'Purchase Orders',    CURRENT_TIMESTAMP),
    ('INVENTORY_GRN_REGENERATE_NUMBERS',             'Regenerate Goods Receipt Numbers', 'MASTER', 'Goods Receipts',     CURRENT_TIMESTAMP),
    ('INVENTORY_SUPPLIER_RETURN_REGENERATE_NUMBERS', 'Regenerate Supplier Return Numbers','MASTER', 'Supplier Returns',  CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN (
      'INVENTORY_QUOTATION_REGENERATE_NUMBERS',
      'INVENTORY_PURCHASE_ORDER_REGENERATE_NUMBERS',
      'INVENTORY_GRN_REGENERATE_NUMBERS',
      'INVENTORY_SUPPLIER_RETURN_REGENERATE_NUMBERS'
  )
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
