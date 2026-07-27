-- Permissions for Faculty Availability (new admin screen backing faculty_availability, V313) and
-- Timetable Swap (moving/exchanging a draft session's day+period based on staff availability).
-- Kept as their own rows per the operation-wise permission mapping gate -- swapping is a distinct
-- operation from TIMETABLE_GENERATE/TIMETABLE_MANAGE, and Faculty Availability is a distinct
-- screen from the Faculty master itself.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('FACULTY_AVAILABILITY_VIEW',   'View Faculty Availability',   'CURRICULUM', 'Faculty Availability', CURRENT_TIMESTAMP),
    ('FACULTY_AVAILABILITY_MANAGE', 'Manage Faculty Availability', 'CURRICULUM', 'Faculty Availability', CURRENT_TIMESTAMP),
    ('TIMETABLE_SWAP',              'Swap Timetable Periods',      'CURRICULUM', 'Timetable',             CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Grant to roles that already hold TIMETABLE_MANAGE (swap + availability naturally travel with it)
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code IN
    ('FACULTY_AVAILABILITY_VIEW', 'FACULTY_AVAILABILITY_MANAGE', 'TIMETABLE_SWAP')) new_p
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
