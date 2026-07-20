-- Permissions for the new one-click timetable generator. TIMETABLE_VIEW is a real, dedicated
-- permission -- unlike the notification-feed precedent, viewing even your own timetable is not
-- self-service, per explicit user decision during specialist review. It is therefore NOT
-- auto-granted to STUDENT/FACULTY roles here; whoever provisions those roles must grant it
-- explicitly via Role Management once this ships.
--
-- TIMETABLE_VIEW/GENERATE/MANAGE are auto-granted to roles already holding the equivalent
-- LAB_SCHEDULE_* permissions (the existing manual scheduling screen this feature extends).

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('TIMETABLE_VIEW',     'View Timetable',     'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP),
    ('TIMETABLE_GENERATE', 'Generate Timetable',  'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP),
    ('TIMETABLE_MANAGE',   'Manage Timetable',    'CURRICULUM', 'Timetable', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LAB_SCHEDULE_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LAB_SCHEDULE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_GENERATE') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LAB_SCHEDULE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'TIMETABLE_MANAGE') new_p
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
