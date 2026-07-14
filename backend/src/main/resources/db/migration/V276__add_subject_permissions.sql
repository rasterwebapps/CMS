-- Permissions for the new standalone Subject Master screen (create/edit/list subjects
-- and map each to a Course + optional Speciality). Previously subjects could only be
-- created via direct API calls or seed migrations, with no admin screen.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('SUBJECT_VIEW',   'View Subjects',   'MASTER', 'Subjects', CURRENT_TIMESTAMP),
    ('SUBJECT_MANAGE', 'Manage Subjects', 'MASTER', 'Subjects', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

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
