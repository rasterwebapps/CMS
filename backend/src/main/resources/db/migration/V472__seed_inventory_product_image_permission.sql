-- Permission for managing a Product's photos. Kept distinct from INVENTORY_PRODUCT_MANAGE per
-- this repo's own established precedent (FacultyDocumentController uses a dedicated
-- FACULTY_DOC_CONFIG_MANAGE rather than reusing FACULTY's own manage permission) — uploading/
-- deleting/re-ordering photos is a genuinely distinct operation from editing a product's core
-- fields, per the operation-wise permission mapping rule. Viewing images reuses the existing
-- INVENTORY_PRODUCT_VIEW/_MANAGE (any product viewer can see its photos) plus this new
-- permission, so a photo-only manager can also view without a separate view-only permission.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_PRODUCT_IMAGE_MANAGE', 'Manage Product Images', 'MASTER', 'Products', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code = 'INVENTORY_PRODUCT_IMAGE_MANAGE'
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
