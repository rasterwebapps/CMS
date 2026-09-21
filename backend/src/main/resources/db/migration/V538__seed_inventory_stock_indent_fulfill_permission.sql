-- Permission for the new store-side fulfillment decision step (Phase D) — covers all four
-- outcomes (fulfill directly, fulfill via transfer-in, raise a purchase requisition, deny), the
-- same way the existing INVENTORY_STOCK_INDENT_APPROVE already covers both approve and reject
-- outcomes of the department head's own decision point. One decision point, one permission.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_STOCK_INDENT_FULFILL', 'Fulfill Stock Indents', 'MASTER', 'Stock Indents', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code = 'INVENTORY_STOCK_INDENT_FULFILL'
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
