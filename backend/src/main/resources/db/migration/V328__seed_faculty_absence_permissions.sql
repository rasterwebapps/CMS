-- Permissions for faculty absence marking + substitute application (Timetable planner Round 2,
-- Phase 6). Kept as two distinct operations per the operation-wise mapping gate: a department
-- coordinator might mark absences without being the one who commits a substitute, and vice versa.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('FACULTY_ABSENCE_MARK',             'Mark Faculty Absence',       'CURRICULUM', 'Faculty Absence', CURRENT_TIMESTAMP),
    ('FACULTY_ABSENCE_SUBSTITUTE_APPLY', 'Apply Faculty Substitute',   'CURRICULUM', 'Faculty Absence', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'TIMETABLE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code IN
    ('FACULTY_ABSENCE_MARK', 'FACULTY_ABSENCE_SUBSTITUTE_APPLY')) new_p
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
