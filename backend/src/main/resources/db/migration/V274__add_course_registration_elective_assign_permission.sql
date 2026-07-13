-- New dedicated permission for the admin single-pick elective assignment action — deliberately
-- NOT reusing ADMISSION_CREATE (which the rest of this controller still relies on) since this is
-- a genuinely new, distinct operation on a different screen (Elective Assignment), per the
-- operation-wise permission mapping hard gate. Auto-assigned to whoever already holds
-- ADMISSION_CREATE, since bulk registration and elective assignment are performed by the same
-- admissions staff today.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('COURSE_REGISTRATION_ELECTIVE_ASSIGN', 'Assign Student Electives', 'CURRICULUM', 'Elective Assignment', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'ADMISSION_CREATE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'COURSE_REGISTRATION_ELECTIVE_ASSIGN') new_p
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
