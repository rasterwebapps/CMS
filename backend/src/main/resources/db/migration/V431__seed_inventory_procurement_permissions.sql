-- Permissions for the new Tax Rule, Supplier, and Rate Contract masters (Phase 2 first slice).
-- Approving a supplier is its own permission, separate from Manage, per the operation-wise
-- permission mapping rule — same pattern as the earlier Cycle Count approve/manage split.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_TAX_RULE_VIEW',       'View Tax Rules',              'MASTER', 'Tax Rules',       CURRENT_TIMESTAMP),
    ('INVENTORY_TAX_RULE_MANAGE',     'Manage Tax Rules',            'MASTER', 'Tax Rules',       CURRENT_TIMESTAMP),
    ('INVENTORY_SUPPLIER_VIEW',       'View Suppliers',               'MASTER', 'Suppliers',       CURRENT_TIMESTAMP),
    ('INVENTORY_SUPPLIER_MANAGE',     'Manage Suppliers',             'MASTER', 'Suppliers',       CURRENT_TIMESTAMP),
    ('INVENTORY_SUPPLIER_APPROVE',    'Approve Suppliers',            'MASTER', 'Suppliers',       CURRENT_TIMESTAMP),
    ('INVENTORY_RATE_CONTRACT_VIEW',  'View Rate Contracts',          'MASTER', 'Rate Contracts',  CURRENT_TIMESTAMP),
    ('INVENTORY_RATE_CONTRACT_MANAGE','Manage Rate Contracts',        'MASTER', 'Rate Contracts',  CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_TAX_RULE_VIEW', 'INVENTORY_TAX_RULE_MANAGE',
                 'INVENTORY_SUPPLIER_VIEW', 'INVENTORY_SUPPLIER_MANAGE', 'INVENTORY_SUPPLIER_APPROVE',
                 'INVENTORY_RATE_CONTRACT_VIEW', 'INVENTORY_RATE_CONTRACT_MANAGE')
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
