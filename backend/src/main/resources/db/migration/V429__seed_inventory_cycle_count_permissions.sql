-- Permissions for the new Cycle Count (physical stock count / reconciliation) workflow. Approve is
-- its own permission, separate from Manage, per the operation-wise permission mapping rule — it
-- gates posting the resulting stock adjustment, not just entering/submitting a count.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_CYCLE_COUNT_VIEW',    'View Cycle Counts',             'MASTER', 'Cycle Counts', CURRENT_TIMESTAMP),
    ('INVENTORY_CYCLE_COUNT_MANAGE',  'Manage Cycle Counts',           'MASTER', 'Cycle Counts', CURRENT_TIMESTAMP),
    ('INVENTORY_CYCLE_COUNT_APPROVE', 'Approve Cycle Count Variances', 'MASTER', 'Cycle Counts', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_CYCLE_COUNT_VIEW', 'INVENTORY_CYCLE_COUNT_MANAGE', 'INVENTORY_CYCLE_COUNT_APPROVE')
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
