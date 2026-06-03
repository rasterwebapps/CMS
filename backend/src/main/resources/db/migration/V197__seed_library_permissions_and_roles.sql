-- ============================================================
-- V101: Library Module — permissions, LIBRARIAN role,
--       and role-permission assignments
-- ============================================================

-- ------------------------------------------------------------
-- 1. NEW ROLE: LIBRARIAN
-- ------------------------------------------------------------
INSERT INTO app_roles (name, display_name, hierarchy_level, is_system_role, description, created_at, updated_at)
VALUES (
    'LIBRARIAN', 'Librarian', 5, FALSE,
    'Manages the college library — catalogue, circulation, periodicals, and fines',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 2. LIBRARY PERMISSIONS
-- ------------------------------------------------------------
INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('LIBRARY_CATALOGUE_VIEW',      'View Library Catalogue',       'LIBRARY', CURRENT_TIMESTAMP),
    ('LIBRARY_CATALOGUE_MANAGE',    'Manage Library Catalogue',     'LIBRARY', CURRENT_TIMESTAMP),
    ('LIBRARY_ISSUE_VIEW',          'View Library Issues',          'LIBRARY', CURRENT_TIMESTAMP),
    ('LIBRARY_ISSUE_MANAGE',        'Manage Library Issues',        'LIBRARY', CURRENT_TIMESTAMP),
    ('LIBRARY_FINE_VIEW',           'View Library Fines',           'LIBRARY', CURRENT_TIMESTAMP),
    ('LIBRARY_FINE_MANAGE',         'Manage Library Fines',         'LIBRARY', CURRENT_TIMESTAMP),
    ('LIBRARY_PERIODICAL_VIEW',     'View Library Periodicals',     'LIBRARY', CURRENT_TIMESTAMP),
    ('LIBRARY_PERIODICAL_MANAGE',   'Manage Library Periodicals',   'LIBRARY', CURRENT_TIMESTAMP),
    ('LIBRARY_SETTINGS_MANAGE',     'Manage Library Settings',      'LIBRARY', CURRENT_TIMESTAMP),
    ('LIBRARY_REPORT_VIEW',         'View Library Reports',         'LIBRARY', CURRENT_TIMESTAMP),
    ('LIBRARY_IMPORT',              'Import Library Books',         'LIBRARY', CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- 3. ROLE-PERMISSION ASSIGNMENTS
-- ------------------------------------------------------------

-- LIBRARIAN: full access to all library permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'LIBRARIAN'
  AND p.category = 'LIBRARY';

-- DEV_ADMIN, SUPPORT_ADMIN, ADMIN: full library access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN')
  AND p.category = 'LIBRARY'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- COLLEGE_ADMIN: full library access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'COLLEGE_ADMIN'
  AND p.category = 'LIBRARY'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- FACULTY: read-only catalogue + own issue history (service layer enforces "own only")
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'FACULTY'
  AND p.code IN ('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_ISSUE_VIEW')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- STUDENT: read-only catalogue + own issue history (service layer enforces "own only")
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'STUDENT'
  AND p.code IN ('LIBRARY_CATALOGUE_VIEW', 'LIBRARY_ISSUE_VIEW')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
