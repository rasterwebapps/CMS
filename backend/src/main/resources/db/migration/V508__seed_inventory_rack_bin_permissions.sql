-- Permissions for the new Inventory Rack and Bin masters (operation-wise: each screen gets its
-- own View/Manage pair, per the codebase's dedicated-permission-per-screen convention). Default
-- tier matches the closest existing screen (INVENTORY_LOCATION_MANAGE's tier, via auto-assign
-- below) rather than a hardcoded tier value.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_RACK_VIEW',   'View Inventory Racks',   'MASTER', 'Inventory Racks', CURRENT_TIMESTAMP),
    ('INVENTORY_RACK_MANAGE', 'Manage Inventory Racks', 'MASTER', 'Inventory Racks', CURRENT_TIMESTAMP),
    ('INVENTORY_BIN_VIEW',    'View Inventory Bins',    'MASTER', 'Inventory Bins',  CURRENT_TIMESTAMP),
    ('INVENTORY_BIN_MANAGE',  'Manage Inventory Bins',  'MASTER', 'Inventory Bins',  CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Auto-assign Rack/Bin MANAGE to any role that already holds INVENTORY_LOCATION_MANAGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'INVENTORY_LOCATION_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code IN ('INVENTORY_RACK_VIEW', 'INVENTORY_RACK_MANAGE', 'INVENTORY_BIN_VIEW', 'INVENTORY_BIN_MANAGE')) new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- Auto-assign Rack/Bin VIEW (only) to any role that holds INVENTORY_LOCATION_VIEW but not MANAGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'INVENTORY_LOCATION_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code IN ('INVENTORY_RACK_VIEW', 'INVENTORY_BIN_VIEW')) new_p
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
