-- Permissions for the new Stock Issue Request workflow. Approve is its own permission, separate
-- from Manage, per the operation-wise permission mapping rule — it's the action that posts real
-- stock movement, same pattern as Purchase Requisition's own approve/manage split (V435).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_ISSUE_REQUEST_VIEW',    'View Stock Issue Requests',    'MASTER', 'Stock Issue Requests', CURRENT_TIMESTAMP),
    ('INVENTORY_ISSUE_REQUEST_MANAGE',  'Manage Stock Issue Requests',  'MASTER', 'Stock Issue Requests', CURRENT_TIMESTAMP),
    ('INVENTORY_ISSUE_REQUEST_APPROVE', 'Approve Stock Issue Requests', 'MASTER', 'Stock Issue Requests', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_ISSUE_REQUEST_VIEW', 'INVENTORY_ISSUE_REQUEST_MANAGE', 'INVENTORY_ISSUE_REQUEST_APPROVE')
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
