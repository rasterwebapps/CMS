-- Permissions for the new Clinical Venue master screen (R3 Phase 2). Auto-assigned to any role
-- that already holds CLASSROOM_VIEW/CLASSROOM_MANAGE, since Clinical Venue is the same kind of
-- physical/posting-site master managed by the same admin population.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('CLINICAL_VENUE_VIEW',   'View Clinical Venues',   'MASTER', 'Clinical Venues', CURRENT_TIMESTAMP),
    ('CLINICAL_VENUE_MANAGE', 'Manage Clinical Venues', 'MASTER', 'Clinical Venues', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'CLASSROOM_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'CLINICAL_VENUE_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'CLASSROOM_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'CLINICAL_VENUE_MANAGE') new_p
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
