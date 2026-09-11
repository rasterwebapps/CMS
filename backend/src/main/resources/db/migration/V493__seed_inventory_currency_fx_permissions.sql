-- Permissions for the two new Currency FX screens (Currency Settings, Currency Exchange Rates).
-- Codes checked against existing INVENTORY_* permission codes for collision — none. Follows the
-- same DEV_ADMIN/SUPPORT_ADMIN/ADMIN/COLLEGE_ADMIN grant + catch-all sync pattern as V423/V477.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_CURRENCY_SETTINGS_VIEW',       'View Currency Settings',        'MASTER', 'Currency Settings',        CURRENT_TIMESTAMP),
    ('INVENTORY_CURRENCY_SETTINGS_MANAGE',     'Manage Currency Settings',      'MASTER', 'Currency Settings',        CURRENT_TIMESTAMP),
    ('INVENTORY_CURRENCY_EXCHANGE_RATE_VIEW',   'View Currency Exchange Rates',  'MASTER', 'Currency Exchange Rates', CURRENT_TIMESTAMP),
    ('INVENTORY_CURRENCY_EXCHANGE_RATE_MANAGE', 'Manage Currency Exchange Rates', 'MASTER', 'Currency Exchange Rates', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_CURRENCY_SETTINGS_VIEW', 'INVENTORY_CURRENCY_SETTINGS_MANAGE',
                 'INVENTORY_CURRENCY_EXCHANGE_RATE_VIEW', 'INVENTORY_CURRENCY_EXCHANGE_RATE_MANAGE')
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
