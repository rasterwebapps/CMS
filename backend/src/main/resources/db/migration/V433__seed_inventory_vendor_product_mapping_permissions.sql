-- Permissions for the new Vendor Product Mapping master (Phase 2 second slice). RateContractLine
-- has no lifecycle independent of its parent RateContract (per the operation-wise permission
-- mapping rule's "no distinct operation" carve-out, same as CategoryAttribute under Category), so
-- it stays under the existing INVENTORY_RATE_CONTRACT_MANAGE and needs no permission of its own.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_VENDOR_PRODUCT_MAPPING_VIEW',   'View Vendor Product Rates',   'MASTER', 'Vendor Product Rates', CURRENT_TIMESTAMP),
    ('INVENTORY_VENDOR_PRODUCT_MAPPING_MANAGE', 'Manage Vendor Product Rates', 'MASTER', 'Vendor Product Rates', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_VENDOR_PRODUCT_MAPPING_VIEW', 'INVENTORY_VENDOR_PRODUCT_MAPPING_MANAGE')
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
