-- Permissions for the new Purchase Requisition workflow. Approve is its own permission, separate
-- from Manage, per the operation-wise permission mapping rule — same pattern as Cycle Count's
-- approve/manage split (V429).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_PURCHASE_REQUISITION_VIEW',    'View Purchase Requisitions',    'MASTER', 'Purchase Requisitions', CURRENT_TIMESTAMP),
    ('INVENTORY_PURCHASE_REQUISITION_MANAGE',  'Manage Purchase Requisitions',  'MASTER', 'Purchase Requisitions', CURRENT_TIMESTAMP),
    ('INVENTORY_PURCHASE_REQUISITION_APPROVE', 'Approve Purchase Requisitions', 'MASTER', 'Purchase Requisitions', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_PURCHASE_REQUISITION_VIEW', 'INVENTORY_PURCHASE_REQUISITION_MANAGE', 'INVENTORY_PURCHASE_REQUISITION_APPROVE')
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
