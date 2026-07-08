-- V257: Export permissions for the 4 Library list screens (Book Catalogue, Issue Register,
-- Fines, Journals & Periodicals). Each screen gets its own dedicated export permission
-- (never shared) and is auto-assigned to whichever roles already hold that screen's
-- own MANAGE permission — export is manage-tier, not granted to FACULTY/STUDENT view-only access.

INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('LIBRARY_CATALOGUE_EXPORT',  'Export Book Catalogue',           'LIBRARY', 'Book Catalogue', 4, CURRENT_TIMESTAMP),
    ('LIBRARY_ISSUE_EXPORT',      'Export Issue Register',           'LIBRARY', 'Issue Desk',      4, CURRENT_TIMESTAMP),
    ('LIBRARY_FINE_EXPORT',       'Export Fine Register',            'LIBRARY', 'Fines',           4, CURRENT_TIMESTAMP),
    ('LIBRARY_PERIODICAL_EXPORT', 'Export Journals & Periodicals',   'LIBRARY', 'Journals',        4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Auto-assign each export permission to roles that already hold that screen's own MANAGE permission
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LIBRARY_CATALOGUE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'LIBRARY_CATALOGUE_EXPORT') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LIBRARY_ISSUE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'LIBRARY_ISSUE_EXPORT') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LIBRARY_FINE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'LIBRARY_FINE_EXPORT') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LIBRARY_PERIODICAL_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'LIBRARY_PERIODICAL_EXPORT') new_p
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
