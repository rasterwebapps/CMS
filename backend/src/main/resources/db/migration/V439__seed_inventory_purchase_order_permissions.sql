-- Permissions for the new Purchase Order workflow. Force-close is its own permission, separate
-- from Manage, per the operation-wise permission mapping rule — it's the one action with real
-- audit-worthy consequence (closing an order before it's fully received), same pattern as Cycle
-- Count's approve/manage split (V429) and Purchase Requisition's approve/manage split (V435).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_PURCHASE_ORDER_VIEW',         'View Purchase Orders',         'MASTER', 'Purchase Orders', CURRENT_TIMESTAMP),
    ('INVENTORY_PURCHASE_ORDER_MANAGE',       'Manage Purchase Orders',       'MASTER', 'Purchase Orders', CURRENT_TIMESTAMP),
    ('INVENTORY_PURCHASE_ORDER_FORCE_CLOSE',  'Force-Close Purchase Orders',  'MASTER', 'Purchase Orders', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_PURCHASE_ORDER_VIEW', 'INVENTORY_PURCHASE_ORDER_MANAGE', 'INVENTORY_PURCHASE_ORDER_FORCE_CLOSE')
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
