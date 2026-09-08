-- Permissions for the new Consignment (vendor-owned stock) workflow. Recording consumption
-- ("ownership transfer" — the portion the supplier should now bill for) is its own permission,
-- separate from Manage (defining agreements, receiving stock), per the operation-wise permission
-- mapping rule — mirrors the Wanted List's own MANAGE/RUN/CONVERT split.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_CONSIGNMENT_VIEW',    'View Consignment Stock',           'MASTER', 'Consignment Stock', CURRENT_TIMESTAMP),
    ('INVENTORY_CONSIGNMENT_MANAGE',  'Manage Consignment Stock',         'MASTER', 'Consignment Stock', CURRENT_TIMESTAMP),
    ('INVENTORY_CONSIGNMENT_CONVERT', 'Record Consignment Consumption',   'MASTER', 'Consignment Stock', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_CONSIGNMENT_VIEW', 'INVENTORY_CONSIGNMENT_MANAGE', 'INVENTORY_CONSIGNMENT_CONVERT')
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
