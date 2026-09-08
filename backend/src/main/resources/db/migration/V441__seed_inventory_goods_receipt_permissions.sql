-- Permissions for the new Goods Receipt workflow. Confirm is its own permission, separate from
-- Manage, per the operation-wise permission mapping rule — it's the action with real stock/
-- financial consequence (posts to the stock ledger, advances the parent PO's status), same
-- pattern as Cycle Count's approve/manage split (V429).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_GRN_VIEW',     'View Goods Receipts',    'MASTER', 'Goods Receipts', CURRENT_TIMESTAMP),
    ('INVENTORY_GRN_MANAGE',   'Manage Goods Receipts',  'MASTER', 'Goods Receipts', CURRENT_TIMESTAMP),
    ('INVENTORY_GRN_CONFIRM',  'Confirm Goods Receipts', 'MASTER', 'Goods Receipts', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_GRN_VIEW', 'INVENTORY_GRN_MANAGE', 'INVENTORY_GRN_CONFIRM')
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
