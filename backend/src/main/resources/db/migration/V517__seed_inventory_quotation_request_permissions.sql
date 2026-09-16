-- Permissions for the new Quotation Request (RFQ) workflow. Award is its own permission, separate
-- from Manage, per the operation-wise permission mapping rule — awarding commits a real spend
-- decision, same posture as Purchase Requisition's own Approve/Manage split (V435) and Cycle
-- Count's Approve/Manage split (V429). Tier defaulted to whichever role already holds the
-- closest-match Purchase Requisition permission (View->View, Manage->Manage, Approve->Award).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('INVENTORY_QUOTATION_VIEW',   'View Quotation Requests',   'MASTER', 'Quotation Requests', CURRENT_TIMESTAMP),
    ('INVENTORY_QUOTATION_MANAGE', 'Manage Quotation Requests', 'MASTER', 'Quotation Requests', CURRENT_TIMESTAMP),
    ('INVENTORY_QUOTATION_AWARD',  'Award Quotation Requests',  'MASTER', 'Quotation Requests', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Auto-assign View to any role that already holds Purchase Requisition View
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'INVENTORY_PURCHASE_REQUISITION_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'INVENTORY_QUOTATION_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- Auto-assign Manage to any role that already holds Purchase Requisition Manage
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'INVENTORY_PURCHASE_REQUISITION_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'INVENTORY_QUOTATION_MANAGE') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- Auto-assign Award to any role that already holds Purchase Requisition Approve
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'INVENTORY_PURCHASE_REQUISITION_APPROVE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'INVENTORY_QUOTATION_AWARD') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
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
