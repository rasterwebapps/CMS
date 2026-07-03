-- ============================================================
-- V248: Add FEE_REFUND_EXPORT permission for the Refunds screen
-- ============================================================

INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at)
VALUES ('FEE_REFUND_EXPORT', 'Export Fee Refunds', 'FINANCE', 'Refunds', 4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Auto-assign to roles that already hold FEE_REFUND_APPROVE
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'FEE_REFUND_APPROVE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'FEE_REFUND_EXPORT') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- DEV_ADMIN / SUPPORT_ADMIN catch-all sync
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
