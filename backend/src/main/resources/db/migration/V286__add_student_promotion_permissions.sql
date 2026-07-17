-- New dedicated permissions for the Student Promotion feature, per the operation-wise permission
-- mapping hard gate — separate VIEW/MANAGE codes, not reused from EXAM_RESULT_*. Auto-granted to
-- whoever already holds EXAM_RESULT_VIEW/EXAM_RESULT_MANAGE, since promotion decisions are driven
-- by the same exam-result and attendance data those roles already work with.

INSERT INTO permissions (code, display_name, category, screen_label, created_at) VALUES
    ('STUDENT_PROMOTION_VIEW',   'View Student Promotion',   'EXAMINATION', 'Student Promotion', CURRENT_TIMESTAMP),
    ('STUDENT_PROMOTION_MANAGE', 'Manage Student Promotion', 'EXAMINATION', 'Student Promotion', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'EXAM_RESULT_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'STUDENT_PROMOTION_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'EXAM_RESULT_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'STUDENT_PROMOTION_MANAGE') new_p
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
