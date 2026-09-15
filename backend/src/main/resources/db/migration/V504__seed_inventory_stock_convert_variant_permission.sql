-- Operation-wise permission for the stranded-balance "convert to variant" action on the Stock
-- Balance screen (a dedicated permission, not a reuse of INVENTORY_STOCK_MANAGE) — see the
-- 2026-09-15 "null-variant stock is stranded" specialist round in DECISION_LOG.md. Tier defaults
-- to the same roles as INVENTORY_STOCK_MANAGE (V427), since this is a stock-mutating action.
-- Columns verified against V427/V496 (permissions/role_permissions).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_STOCK_CONVERT_VARIANT', 'Convert Unassigned Stock Balance to Variant', 'MASTER', 'Stock Balance', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code = 'INVENTORY_STOCK_CONVERT_VARIANT'
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
