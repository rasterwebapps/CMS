-- V256: Permissions for the new Library Rack/Shelf masters and Book Transfer feature.
-- One permission pair covers both the Rack master and its nested Shelf-tier master (one screen).
-- Rack/Shelf/Transfer are staff-only actions, so they're auto-assigned to whichever roles
-- already hold LIBRARY_CATALOGUE_MANAGE (LIBRARIAN, COLLEGE_ADMIN, ADMIN per V197) —
-- not to FACULTY/STUDENT, unlike the broader LIBRARY_CATALOGUE_VIEW.

INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('LIBRARY_SHELF_VIEW',   'View Library Racks & Shelves',   'LIBRARY', 'Library Racks & Shelves', 4, CURRENT_TIMESTAMP),
    ('LIBRARY_SHELF_MANAGE', 'Manage Library Racks & Shelves', 'LIBRARY', 'Library Racks & Shelves', 4, CURRENT_TIMESTAMP),
    ('LIBRARY_TRANSFER',     'Transfer Books',                 'LIBRARY', 'Book Catalogue',          4, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Auto-assign all three to roles that already hold LIBRARY_CATALOGUE_MANAGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LIBRARY_CATALOGUE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code IN ('LIBRARY_SHELF_VIEW', 'LIBRARY_SHELF_MANAGE', 'LIBRARY_TRANSFER')) new_p
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
