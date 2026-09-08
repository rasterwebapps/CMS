-- Permissions for the new Service Ticket workflow. Assign/Resolve/Close/Manage(create+cancel)
-- are each their own permission, per the operation-wise permission mapping rule — a coordinator
-- assigns, a technician resolves, and closing (with feedback) is typically a different actor
-- again from either. Category master management is a separate screen/permission pair, same
-- convention as every other simple master in this module (Uom, TaxRule, etc.).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_SERVICE_TICKET_VIEW',            'View Service Tickets',            'MASTER', 'Service Tickets',           CURRENT_TIMESTAMP),
    ('INVENTORY_SERVICE_TICKET_MANAGE',          'Manage Service Tickets',          'MASTER', 'Service Tickets',           CURRENT_TIMESTAMP),
    ('INVENTORY_SERVICE_TICKET_ASSIGN',          'Assign Service Tickets',          'MASTER', 'Service Tickets',           CURRENT_TIMESTAMP),
    ('INVENTORY_SERVICE_TICKET_RESOLVE',         'Resolve Service Tickets',         'MASTER', 'Service Tickets',           CURRENT_TIMESTAMP),
    ('INVENTORY_SERVICE_TICKET_CLOSE',           'Close Service Tickets',           'MASTER', 'Service Tickets',           CURRENT_TIMESTAMP),
    ('INVENTORY_SERVICE_TICKET_CATEGORY_VIEW',   'View Service Ticket Categories',  'MASTER', 'Service Ticket Categories', CURRENT_TIMESTAMP),
    ('INVENTORY_SERVICE_TICKET_CATEGORY_MANAGE', 'Manage Service Ticket Categories','MASTER', 'Service Ticket Categories', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
  AND p.code IN ('INVENTORY_SERVICE_TICKET_VIEW', 'INVENTORY_SERVICE_TICKET_MANAGE', 'INVENTORY_SERVICE_TICKET_ASSIGN',
                 'INVENTORY_SERVICE_TICKET_RESOLVE', 'INVENTORY_SERVICE_TICKET_CLOSE',
                 'INVENTORY_SERVICE_TICKET_CATEGORY_VIEW', 'INVENTORY_SERVICE_TICKET_CATEGORY_MANAGE')
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
