-- Final RBAC cleanup: Keycloak is identity-only; application authorization is DB-driven.

-- Only dev/support platform roles are immutable system roles.
UPDATE app_roles
SET is_system_role = CASE WHEN name IN ('DEV_ADMIN', 'SUPPORT_ADMIN') THEN TRUE ELSE FALSE END,
    updated_at = CURRENT_TIMESTAMP;

-- Ensure immutable platform identities have matching DB users.
INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'devadmin', 'devadmin@cms.local', 'Developer Administrator', r.id, TRUE, 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM app_roles r
WHERE r.name = 'DEV_ADMIN'
ON CONFLICT (keycloak_username) DO UPDATE
SET email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    app_role_id = EXCLUDED.app_role_id,
    is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'supportadmin', 'supportadmin@cms.local', 'Support Administrator', r.id, TRUE, 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM app_roles r
WHERE r.name = 'SUPPORT_ADMIN'
ON CONFLICT (keycloak_username) DO UPDATE
SET email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    app_role_id = EXCLUDED.app_role_id,
    is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

-- Remove seeded/demo users that should not exist by default in DB-driven RBAC.
DELETE FROM app_users
WHERE keycloak_username IN (
    'admin',
    'frontoffice',
    'cashier',
    'faculty1',
    'student1',
    'labincharge1',
    'parent1',
    'college.admin',
    'front.office',
    'faculty.cs',
    'lab.incharge',
    'student.demo'
);

-- Remove obsolete seeded business roles only when no real users still reference them.
DELETE FROM app_roles r
WHERE r.name IN (
    'ADMIN',
    'COLLEGE_ADMIN',
    'FRONT_OFFICE',
    'CASHIER',
    'FACULTY',
    'LAB_INCHARGE',
    'TECHNICIAN',
    'STUDENT',
    'PARENT'
)
AND NOT EXISTS (
    SELECT 1
    FROM app_users u
    WHERE u.app_role_id = r.id
);
