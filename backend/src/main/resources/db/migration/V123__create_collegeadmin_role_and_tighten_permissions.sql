-- Create DB-driven collegeadmin role and align seeded roles with immutable-system-role policy.

-- Only dev/support are immutable system roles.
UPDATE app_roles
SET is_system_role = CASE WHEN name IN ('DEV_ADMIN', 'SUPPORT_ADMIN') THEN TRUE ELSE FALSE END,
    updated_at = CURRENT_TIMESTAMP;

-- Ensure the collegeadmin role exists.
INSERT INTO app_roles (name, display_name, hierarchy_level, is_system_role, description, created_at, updated_at)
VALUES (
    'collegeadmin',
    'College Admin',
    4,
    FALSE,
    'Admission workflow operations with admission-related master access',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (name) DO UPDATE
SET display_name = EXCLUDED.display_name,
    hierarchy_level = EXCLUDED.hierarchy_level,
    is_system_role = FALSE,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

-- Tighten collegeadmin permissions to only the rights needed for admissions,
-- student conversion/completion, fee completion, and required master screens.
WITH allowed_codes(code) AS (
    VALUES
        ('DEPT_VIEW'),
        ('PROGRAM_VIEW'),
        ('COURSE_VIEW'),
        ('ACADEMIC_YEAR_VIEW'),
        ('SEMESTER_VIEW'),
        ('FEE_STRUCTURE_VIEW'),
        ('AGENT_VIEW'),
        ('REFERRAL_TYPE_VIEW'),
        ('COMMUNITY_VIEW'),
        ('BLOOD_GROUP_VIEW'),
        ('ENQUIRY_VIEW'),
        ('ENQUIRY_CREATE'),
        ('ENQUIRY_EDIT'),
        ('DOCUMENT_SUBMISSION_VIEW'),
        ('DOCUMENT_SUBMISSION_MANAGE'),
        ('ADMISSION_VIEW'),
        ('ADMISSION_CREATE'),
        ('ADMISSION_EDIT'),
        ('STUDENT_VIEW'),
        ('STUDENT_CREATE'),
        ('STUDENT_EDIT'),
        ('ROLL_NUMBER_ASSIGN'),
        ('STUDENT_FEE_VIEW'),
        ('STUDENT_FEE_MANAGE'),
        ('FEE_COLLECT'),
        ('FEE_FINALIZE')
),
allowed_permissions AS (
    SELECT p.id
    FROM permissions p
    JOIN allowed_codes a ON a.code = p.code
),
target_roles AS (
    SELECT id
    FROM app_roles
    WHERE name IN ('collegeadmin', 'COLLEGE_ADMIN')
)
DELETE FROM role_permissions rp
USING target_roles tr
WHERE rp.role_id = tr.id
  AND rp.permission_id NOT IN (SELECT id FROM allowed_permissions);

WITH allowed_codes(code) AS (
    VALUES
        ('DEPT_VIEW'), ('PROGRAM_VIEW'), ('COURSE_VIEW'), ('ACADEMIC_YEAR_VIEW'), ('SEMESTER_VIEW'),
        ('FEE_STRUCTURE_VIEW'), ('AGENT_VIEW'), ('REFERRAL_TYPE_VIEW'), ('COMMUNITY_VIEW'), ('BLOOD_GROUP_VIEW'),
        ('ENQUIRY_VIEW'), ('ENQUIRY_CREATE'), ('ENQUIRY_EDIT'),
        ('DOCUMENT_SUBMISSION_VIEW'), ('DOCUMENT_SUBMISSION_MANAGE'),
        ('ADMISSION_VIEW'), ('ADMISSION_CREATE'), ('ADMISSION_EDIT'),
        ('STUDENT_VIEW'), ('STUDENT_CREATE'), ('STUDENT_EDIT'), ('ROLL_NUMBER_ASSIGN'),
        ('STUDENT_FEE_VIEW'), ('STUDENT_FEE_MANAGE'), ('FEE_COLLECT'), ('FEE_FINALIZE')
),
allowed_permissions AS (
    SELECT p.id
    FROM permissions p
    JOIN allowed_codes a ON a.code = p.code
),
target_roles AS (
    SELECT id
    FROM app_roles
    WHERE name IN ('collegeadmin', 'COLLEGE_ADMIN')
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT tr.id, ap.id
FROM target_roles tr
CROSS JOIN allowed_permissions ap
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = tr.id
      AND rp.permission_id = ap.id
);

-- Move existing COLLEGE_ADMIN users to the new DB role name.
UPDATE app_users u
SET app_role_id = new_role.id,
    updated_at = CURRENT_TIMESTAMP
FROM app_roles old_role
JOIN app_roles new_role ON new_role.name = 'collegeadmin'
WHERE u.app_role_id = old_role.id
  AND old_role.name = 'COLLEGE_ADMIN';

-- Ensure the expected collegeadmin app user exists and points to the DB role.
INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'collegeadmin', 'collegeadmin@cms.local', 'College Administrator', r.id, TRUE, 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM app_roles r
WHERE r.name = 'collegeadmin'
ON CONFLICT (keycloak_username) DO UPDATE
SET email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    app_role_id = EXCLUDED.app_role_id,
    is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

