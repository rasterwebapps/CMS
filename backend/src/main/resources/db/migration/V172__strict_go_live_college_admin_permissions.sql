-- Strict go-live RBAC alignment.
-- Platform roles keep all permissions; College Admin gets only the permissions
-- required to configure masters and complete admissions/fee finalization.

INSERT INTO app_roles (name, display_name, hierarchy_level, is_system_role, description, created_at, updated_at)
VALUES
    ('DEV_ADMIN', 'Developer Admin', 1, TRUE, 'Full system access - development and infrastructure team only', NOW(), NOW()),
    ('SUPPORT_ADMIN', 'Support Admin', 2, TRUE, 'Platform support access - Raster support team only', NOW(), NOW()),
    ('collegeadmin', 'College Admin', 4, FALSE, 'College administrator for admission setup and operations', NOW(), NOW())
ON CONFLICT (name) DO UPDATE
SET display_name = EXCLUDED.display_name,
    hierarchy_level = EXCLUDED.hierarchy_level,
    is_system_role = EXCLUDED.is_system_role,
    description = EXCLUDED.description,
    updated_at = NOW();

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r
CROSS JOIN permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

WITH allowed_codes(code) AS (
    VALUES
        ('DEPT_VIEW'), ('DEPT_MANAGE'),
        ('PROGRAM_VIEW'), ('PROGRAM_MANAGE'),
        ('COURSE_VIEW'), ('COURSE_MANAGE'),
        ('ACADEMIC_YEAR_VIEW'), ('ACADEMIC_YEAR_MANAGE'),
        ('SEMESTER_VIEW'), ('SEMESTER_MANAGE'),
        ('FEE_STRUCTURE_VIEW'), ('FEE_STRUCTURE_MANAGE'),
        ('FACULTY_VIEW'), ('FACULTY_MANAGE'),
        ('AGENT_VIEW'), ('AGENT_MANAGE'),
        ('REFERRAL_TYPE_VIEW'), ('REFERRAL_TYPE_MANAGE'),
        ('COMMUNITY_VIEW'), ('COMMUNITY_MANAGE'),
        ('BLOOD_GROUP_VIEW'), ('BLOOD_GROUP_MANAGE'),
        ('INDIA_LOCATION_VIEW'), ('INDIA_LOCATION_MANAGE'),
        ('SETTINGS_VIEW'), ('SETTINGS_MANAGE'),
        ('NUMBER_SEQUENCE_VIEW'),
        ('ENQUIRY_VIEW'), ('ENQUIRY_CREATE'), ('ENQUIRY_EDIT'),
        ('DOCUMENT_SUBMISSION_VIEW'), ('DOCUMENT_SUBMISSION_MANAGE'), ('DOCUMENT_VERIFICATION_MANAGE'),
        ('ADMISSION_VIEW'), ('ADMISSION_CREATE'), ('ADMISSION_EDIT'),
        ('STUDENT_VIEW'), ('STUDENT_CREATE'), ('STUDENT_EDIT'), ('ROLL_NUMBER_ASSIGN'),
        ('STUDENT_FEE_VIEW'), ('STUDENT_FEE_MANAGE'),
        ('FEE_COLLECT'), ('FEE_FINALIZE'), ('RECEIPT_VIEW')
), allowed_permissions AS (
    SELECT p.id
    FROM permissions p
    JOIN allowed_codes a ON a.code = p.code
), college_roles AS (
    SELECT id
    FROM app_roles
    WHERE name IN ('collegeadmin', 'COLLEGE_ADMIN')
)
DELETE FROM role_permissions rp
USING college_roles cr
WHERE rp.role_id = cr.id
  AND rp.permission_id NOT IN (SELECT id FROM allowed_permissions);

WITH allowed_codes(code) AS (
    VALUES
        ('DEPT_VIEW'), ('DEPT_MANAGE'),
        ('PROGRAM_VIEW'), ('PROGRAM_MANAGE'),
        ('COURSE_VIEW'), ('COURSE_MANAGE'),
        ('ACADEMIC_YEAR_VIEW'), ('ACADEMIC_YEAR_MANAGE'),
        ('SEMESTER_VIEW'), ('SEMESTER_MANAGE'),
        ('FEE_STRUCTURE_VIEW'), ('FEE_STRUCTURE_MANAGE'),
        ('FACULTY_VIEW'), ('FACULTY_MANAGE'),
        ('AGENT_VIEW'), ('AGENT_MANAGE'),
        ('REFERRAL_TYPE_VIEW'), ('REFERRAL_TYPE_MANAGE'),
        ('COMMUNITY_VIEW'), ('COMMUNITY_MANAGE'),
        ('BLOOD_GROUP_VIEW'), ('BLOOD_GROUP_MANAGE'),
        ('INDIA_LOCATION_VIEW'), ('INDIA_LOCATION_MANAGE'),
        ('SETTINGS_VIEW'), ('SETTINGS_MANAGE'),
        ('NUMBER_SEQUENCE_VIEW'),
        ('ENQUIRY_VIEW'), ('ENQUIRY_CREATE'), ('ENQUIRY_EDIT'),
        ('DOCUMENT_SUBMISSION_VIEW'), ('DOCUMENT_SUBMISSION_MANAGE'), ('DOCUMENT_VERIFICATION_MANAGE'),
        ('ADMISSION_VIEW'), ('ADMISSION_CREATE'), ('ADMISSION_EDIT'),
        ('STUDENT_VIEW'), ('STUDENT_CREATE'), ('STUDENT_EDIT'), ('ROLL_NUMBER_ASSIGN'),
        ('STUDENT_FEE_VIEW'), ('STUDENT_FEE_MANAGE'),
        ('FEE_COLLECT'), ('FEE_FINALIZE'), ('RECEIPT_VIEW')
), allowed_permissions AS (
    SELECT p.id
    FROM permissions p
    JOIN allowed_codes a ON a.code = p.code
), college_role AS (
    SELECT id
    FROM app_roles
    WHERE name = 'collegeadmin'
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT cr.id, ap.id
FROM college_role cr
CROSS JOIN allowed_permissions ap
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = cr.id
      AND rp.permission_id = ap.id
);

INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT u.keycloak_username, u.email, u.full_name, r.id, TRUE, 'system', NOW(), NOW()
FROM (
    VALUES
        ('devadmin', 'devadmin@cms.local', 'Developer Administrator', 'DEV_ADMIN'),
        ('supportadmin', 'supportadmin@cms.local', 'Support Administrator', 'SUPPORT_ADMIN'),
        ('collegeadmin', 'collegeadmin@cms.local', 'College Administrator', 'collegeadmin')
) AS u(keycloak_username, email, full_name, role_name)
JOIN app_roles r ON r.name = u.role_name
ON CONFLICT (keycloak_username) DO UPDATE
SET email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    app_role_id = EXCLUDED.app_role_id,
    is_active = TRUE,
    updated_at = NOW();
