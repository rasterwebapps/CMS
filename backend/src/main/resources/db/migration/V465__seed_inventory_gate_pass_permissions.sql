-- Permissions for the new Gate Pass workflow. Approve and Verify are two separate permissions,
-- per the operation-wise permission mapping rule — whoever approves a gate pass and whoever
-- physically verifies the item at the gate (security) are always distinct operations even when
-- the same person happens to hold both. Return is its own permission too, matching the
-- Loanable Item Issue precedent (V451).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_GATE_PASS_VIEW',    'View Gate Passes',        'MASTER', 'Gate Passes', CURRENT_TIMESTAMP),
    ('INVENTORY_GATE_PASS_MANAGE',  'Manage Gate Passes',      'MASTER', 'Gate Passes', CURRENT_TIMESTAMP),
    ('INVENTORY_GATE_PASS_APPROVE', 'Approve Gate Passes',     'MASTER', 'Gate Passes', CURRENT_TIMESTAMP),
    ('INVENTORY_GATE_PASS_VERIFY',  'Verify Gate Passes',      'MASTER', 'Gate Passes', CURRENT_TIMESTAMP),
    ('INVENTORY_GATE_PASS_RETURN',  'Return Gate Passes',      'MASTER', 'Gate Passes', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_GATE_PASS_VIEW', 'INVENTORY_GATE_PASS_MANAGE', 'INVENTORY_GATE_PASS_APPROVE', 'INVENTORY_GATE_PASS_VERIFY', 'INVENTORY_GATE_PASS_RETURN')
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
