-- Permission for the Faculty Detail "Courses" tab's real workload view (term-scoped assignment
-- breakdown + capacity comparison). Auto-assigned to any role that already holds FACULTY_VIEW,
-- mirroring how V392 granted SECTION_FACULTY_VIEW/MANAGE off COURSE_VIEW/COURSE_MANAGE.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('FACULTY_WORKLOAD_VIEW', 'View Faculty Workload', 'MASTER', 'Faculty', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'FACULTY_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'FACULTY_WORKLOAD_VIEW') new_p
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
