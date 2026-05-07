-- Seed sample users for different roles so User Management screen shows data
-- These users should exist in Keycloak with matching usernames

-- College Admin
INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'college.admin', 'college.admin@cms.local', 'College Administrator', r.id, TRUE, 'system', current_timestamp, current_timestamp
FROM app_roles r
WHERE r.name = 'COLLEGE_ADMIN';

-- Front Office Staff
INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'front.office', 'front.office@cms.local', 'Front Office Staff', r.id, TRUE, 'system', current_timestamp, current_timestamp
FROM app_roles r
WHERE r.name = 'FRONT_OFFICE';

-- Cashier
INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'cashier', 'cashier@cms.local', 'Accountant / Cashier', r.id, TRUE, 'system', current_timestamp, current_timestamp
FROM app_roles r
WHERE r.name = 'CASHIER';

-- Sample Faculty
INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'faculty.cs', 'faculty.cs@cms.local', 'Dr. Computer Science Faculty', r.id, TRUE, 'system', current_timestamp, current_timestamp
FROM app_roles r
WHERE r.name = 'FACULTY';

-- Lab Incharge
INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'lab.incharge', 'lab.incharge@cms.local', 'Lab In-charge', r.id, TRUE, 'system', current_timestamp, current_timestamp
FROM app_roles r
WHERE r.name = 'LAB_INCHARGE';

-- Sample Student (for demonstration - normally created through admission process)
INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'student.demo', 'student.demo@cms.local', 'Demo Student', r.id, TRUE, 'system', current_timestamp, current_timestamp
FROM app_roles r
WHERE r.name = 'STUDENT';

