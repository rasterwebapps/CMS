-- Permissions for the new Multi-level Approval Routing engine. Workflow definition (admin-only)
-- is separate from acting on an instance's steps, per the operation-wise permission mapping
-- rule. Note: the fine-grained "who may act on THIS specific step" check is done in
-- ApprovalInstanceService against whichever permission that step's own definition references
-- (any existing permission code, chosen when the workflow is authored) — INVENTORY_APPROVAL_ACT
-- here is only the baseline gate that lets a user attempt the approve/reject endpoint at all.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_APPROVAL_WORKFLOW_VIEW',   'View Approval Workflows',   'MASTER', 'Approval Workflows', CURRENT_TIMESTAMP),
    ('INVENTORY_APPROVAL_WORKFLOW_MANAGE', 'Manage Approval Workflows', 'MASTER', 'Approval Workflows', CURRENT_TIMESTAMP),
    ('INVENTORY_APPROVAL_VIEW',            'View Approvals',            'MASTER', 'Approvals',          CURRENT_TIMESTAMP),
    ('INVENTORY_APPROVAL_ACT',             'Act on Approvals',          'MASTER', 'Approvals',          CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_APPROVAL_WORKFLOW_VIEW', 'INVENTORY_APPROVAL_WORKFLOW_MANAGE', 'INVENTORY_APPROVAL_VIEW', 'INVENTORY_APPROVAL_ACT')
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
