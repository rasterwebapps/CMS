-- Align application users with usernames present in the Keycloak cms realm.
-- Earlier sample data used dotted usernames (college.admin, front.office, etc.)
-- while the realm exports non-dotted usernames (collegeadmin, frontoffice, etc.).
-- Missing app_users rows make /permissions/my return 404 after login, causing
-- the frontend app initializer to repeatedly re-login.

INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'collegeadmin', 'collegeadmin@cms.local', 'College Administrator', r.id, TRUE, 'system', current_timestamp, current_timestamp
FROM app_roles r
WHERE r.name = 'COLLEGE_ADMIN'
ON CONFLICT (keycloak_username) DO UPDATE
SET app_role_id = EXCLUDED.app_role_id,
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    is_active = TRUE,
    updated_at = current_timestamp;

INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'frontoffice', 'frontoffice@cms.local', 'Front Office Staff', r.id, TRUE, 'system', current_timestamp, current_timestamp
FROM app_roles r
WHERE r.name = 'FRONT_OFFICE'
ON CONFLICT (keycloak_username) DO UPDATE
SET app_role_id = EXCLUDED.app_role_id,
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    is_active = TRUE,
    updated_at = current_timestamp;

INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'faculty1', 'faculty1@cms.local', 'Faculty User', r.id, TRUE, 'system', current_timestamp, current_timestamp
FROM app_roles r
WHERE r.name = 'FACULTY'
ON CONFLICT (keycloak_username) DO UPDATE
SET app_role_id = EXCLUDED.app_role_id,
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    is_active = TRUE,
    updated_at = current_timestamp;

INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'labincharge1', 'labincharge1@cms.local', 'Lab In-charge User', r.id, TRUE, 'system', current_timestamp, current_timestamp
FROM app_roles r
WHERE r.name = 'LAB_INCHARGE'
ON CONFLICT (keycloak_username) DO UPDATE
SET app_role_id = EXCLUDED.app_role_id,
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    is_active = TRUE,
    updated_at = current_timestamp;

INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'student1', 'student1@cms.local', 'Student User', r.id, TRUE, 'system', current_timestamp, current_timestamp
FROM app_roles r
WHERE r.name = 'STUDENT'
ON CONFLICT (keycloak_username) DO UPDATE
SET app_role_id = EXCLUDED.app_role_id,
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    is_active = TRUE,
    updated_at = current_timestamp;

INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'parent1', 'parent1@cms.local', 'Parent User', r.id, TRUE, 'system', current_timestamp, current_timestamp
FROM app_roles r
WHERE r.name = 'PARENT'
ON CONFLICT (keycloak_username) DO UPDATE
SET app_role_id = EXCLUDED.app_role_id,
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    is_active = TRUE,
    updated_at = current_timestamp;

