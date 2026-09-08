-- Permissions for the new Wanted List (reorder shortage / MRP planned-order) workflow.
-- Convert (commits to real spend, creating a Purchase Requisition) and Run (manual on-demand
-- recompute) are each their own permission, separate from Manage (defer/reject/reopen triage) —
-- same operation-wise mapping rule as Purchase Requisition's Approve split (V435).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_WANTED_LIST_VIEW',    'View Wanted List',              'MASTER', 'Wanted List', CURRENT_TIMESTAMP),
    ('INVENTORY_WANTED_LIST_MANAGE',  'Manage Wanted List',            'MASTER', 'Wanted List', CURRENT_TIMESTAMP),
    ('INVENTORY_WANTED_LIST_CONVERT', 'Convert Wanted List to Requisition', 'MASTER', 'Wanted List', CURRENT_TIMESTAMP),
    ('INVENTORY_WANTED_LIST_RUN',     'Run Wanted List Shortage Check','MASTER', 'Wanted List', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_WANTED_LIST_VIEW', 'INVENTORY_WANTED_LIST_MANAGE', 'INVENTORY_WANTED_LIST_CONVERT', 'INVENTORY_WANTED_LIST_RUN')
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
