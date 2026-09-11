-- Permissions for the new Brand master screen. Codes checked against existing INVENTORY_*
-- permission codes for collision — none. Follows the same DEV_ADMIN/SUPPORT_ADMIN/ADMIN/
-- COLLEGE_ADMIN grant + catch-all sync pattern as V423 (Categories, Units of Measure).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_BRAND_VIEW',   'View Brands',   'MASTER', 'Brands', CURRENT_TIMESTAMP),
    ('INVENTORY_BRAND_MANAGE', 'Manage Brands', 'MASTER', 'Brands', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_BRAND_VIEW', 'INVENTORY_BRAND_MANAGE')
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
