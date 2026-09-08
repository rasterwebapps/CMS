-- Permissions for the new Return to Supplier workflow. Two permissions only, same reasoning as
-- Stock Transfer (V443) — completing a return only moves stock the same MANAGE permission
-- already governs; no separate audit-worthy action to split out.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_SUPPLIER_RETURN_VIEW',    'View Supplier Returns',   'MASTER', 'Supplier Returns', CURRENT_TIMESTAMP),
    ('INVENTORY_SUPPLIER_RETURN_MANAGE',  'Manage Supplier Returns', 'MASTER', 'Supplier Returns', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_SUPPLIER_RETURN_VIEW', 'INVENTORY_SUPPLIER_RETURN_MANAGE')
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
