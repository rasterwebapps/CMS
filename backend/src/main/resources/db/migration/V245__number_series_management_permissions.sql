-- V245: Permissions for Number Series management (Phase 3)
INSERT INTO permissions (code, display_name, category, created_at)
VALUES
    ('NUMBER_SERIES_VIEW',   'View Number Series Definitions',   'SETTINGS', CURRENT_TIMESTAMP),
    ('NUMBER_SERIES_MANAGE', 'Manage Number Series Definitions', 'SETTINGS', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE p.code IN ('NUMBER_SERIES_VIEW', 'NUMBER_SERIES_MANAGE')
  AND r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'ADMIN', 'COLLEGE_ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DEV_ADMIN / SUPPORT_ADMIN catch-all
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;
