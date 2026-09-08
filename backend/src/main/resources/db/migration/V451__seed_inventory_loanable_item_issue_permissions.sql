-- Permissions for the new Loanable Item Issue workflow. Return is its own permission, separate
-- from Manage, per the operation-wise permission mapping rule — marking an item returned (with
-- its condition assessment) is a distinct action from issuing it out.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_LOAN_ISSUE_VIEW',    'View Loanable Item Issues',   'MASTER', 'Loanable Item Issues', CURRENT_TIMESTAMP),
    ('INVENTORY_LOAN_ISSUE_MANAGE',  'Manage Loanable Item Issues', 'MASTER', 'Loanable Item Issues', CURRENT_TIMESTAMP),
    ('INVENTORY_LOAN_ISSUE_RETURN',  'Return Loanable Item Issues', 'MASTER', 'Loanable Item Issues', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_LOAN_ISSUE_VIEW', 'INVENTORY_LOAN_ISSUE_MANAGE', 'INVENTORY_LOAN_ISSUE_RETURN')
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
