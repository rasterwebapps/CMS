-- Permission for the Legacy Direct Admit feature — creates a student + admission atomically,
-- bypassing the normal enquiry pipeline (for entering historical/legacy students).
INSERT INTO permissions (code, display_name, category, created_at)
VALUES ('LEGACY_ADMIT', 'Legacy Direct Admit', 'ADMISSION', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Grant to COLLEGE_ADMIN and FRONT_OFFICE roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('COLLEGE_ADMIN', 'FRONT_OFFICE')
  AND p.code = 'LEGACY_ADMIT'
ON CONFLICT DO NOTHING;
