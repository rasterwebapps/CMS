-- Permissions for the new UOM Conversion Templates master screen. Codes checked against existing
-- INVENTORY_* permission codes for collision — none. Follows the same DEV_ADMIN/SUPPORT_ADMIN/
-- ADMIN/COLLEGE_ADMIN grant + catch-all sync pattern as V423/V486.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_UOM_TEMPLATE_VIEW',   'View UOM Conversion Templates',   'MASTER', 'UOM Conversion Templates', CURRENT_TIMESTAMP),
    ('INVENTORY_UOM_TEMPLATE_MANAGE', 'Manage UOM Conversion Templates', 'MASTER', 'UOM Conversion Templates', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_UOM_TEMPLATE_VIEW', 'INVENTORY_UOM_TEMPLATE_MANAGE')
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
