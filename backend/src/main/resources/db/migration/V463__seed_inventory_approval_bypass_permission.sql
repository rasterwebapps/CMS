-- Permission for the new Exception/Bypass action — its own operation-wise permission, separate
-- from INVENTORY_APPROVAL_ACT, per the operation-wise permission mapping rule. Deliberately NOT
-- gated by a workflow step's own referenced permission — bypassing overrides the normal approver
-- requirement, so it needs its own, narrower grant, not "anyone who could have approved anyway."

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_APPROVAL_BYPASS', 'Bypass Approval Steps (Exception)', 'MASTER', 'Approvals', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code = 'INVENTORY_APPROVAL_BYPASS'
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
