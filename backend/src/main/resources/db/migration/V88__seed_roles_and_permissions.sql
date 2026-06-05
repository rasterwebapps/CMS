-- ============================================================
-- 1. ROLES
-- ============================================================
INSERT INTO app_roles (name, display_name, hierarchy_level, is_system_role, description, created_at, updated_at) VALUES
    ('DEV_ADMIN',      'Developer Admin',    1, TRUE,  'Full system access – development and infrastructure team only', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SUPPORT_ADMIN',  'Support Admin',      2, TRUE,  'Platform support access – Raster support team only', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ADMIN',          'Admin',              3, FALSE, 'College-level administrator; manages all college staff and operations', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('COLLEGE_ADMIN',  'College Admin',      4, FALSE, 'Manages day-to-day college operations across all specialities', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FRONT_OFFICE',   'Front Office',       5, FALSE, 'Handles enquiries, admissions and document submission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CASHIER',        'Cashier',            5, FALSE, 'Processes fee collection and views financial summaries', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FACULTY',        'Faculty',            5, FALSE, 'Academic staff; manages curriculum, attendance and exam results', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('LAB_INCHARGE',   'Lab In-charge',      5, FALSE, 'Manages laboratory resources and schedules', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TECHNICIAN',     'Technician',         5, FALSE, 'Maintains lab equipment and inventory', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('STUDENT',        'Student',            6, FALSE, 'Enrolled student – read-only access to own data', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PARENT',         'Parent',             6, FALSE, 'Guardian – read-only access to ward data', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================
-- 2. PERMISSIONS
-- ============================================================

-- SYSTEM
INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('USER_VIEW',         'View Users',                'SYSTEM', CURRENT_TIMESTAMP),
    ('USER_CREATE',       'Create Users',              'SYSTEM', CURRENT_TIMESTAMP),
    ('USER_EDIT',         'Edit Users',                'SYSTEM', CURRENT_TIMESTAMP),
    ('USER_DEACTIVATE',   'Deactivate Users',          'SYSTEM', CURRENT_TIMESTAMP),
    ('ROLE_VIEW',         'View Roles',                'SYSTEM', CURRENT_TIMESTAMP),
    ('ROLE_CREATE',       'Create Roles',              'SYSTEM', CURRENT_TIMESTAMP),
    ('ROLE_EDIT',         'Edit Roles',                'SYSTEM', CURRENT_TIMESTAMP),
    ('PERMISSION_ASSIGN', 'Assign Permissions',        'SYSTEM', CURRENT_TIMESTAMP);

-- MASTER
INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('DEPT_VIEW',               'View Specialities',       'MASTER', CURRENT_TIMESTAMP),
    ('DEPT_MANAGE',             'Manage Specialities',     'MASTER', CURRENT_TIMESTAMP),
    ('PROGRAM_VIEW',            'View Programs',          'MASTER', CURRENT_TIMESTAMP),
    ('PROGRAM_MANAGE',          'Manage Programs',        'MASTER', CURRENT_TIMESTAMP),
    ('COURSE_VIEW',             'View Courses',           'MASTER', CURRENT_TIMESTAMP),
    ('COURSE_MANAGE',           'Manage Courses',         'MASTER', CURRENT_TIMESTAMP),
    ('ACADEMIC_YEAR_VIEW',      'View Academic Years',    'MASTER', CURRENT_TIMESTAMP),
    ('ACADEMIC_YEAR_MANAGE',    'Manage Academic Years',  'MASTER', CURRENT_TIMESTAMP),
    ('SEMESTER_VIEW',           'View Semesters',         'MASTER', CURRENT_TIMESTAMP),
    ('SEMESTER_MANAGE',         'Manage Semesters',       'MASTER', CURRENT_TIMESTAMP),
    ('LAB_VIEW',                'View Labs',              'MASTER', CURRENT_TIMESTAMP),
    ('LAB_MANAGE',              'Manage Labs',            'MASTER', CURRENT_TIMESTAMP),
    ('FEE_STRUCTURE_VIEW',      'View Fee Structures',    'MASTER', CURRENT_TIMESTAMP),
    ('FEE_STRUCTURE_MANAGE',    'Manage Fee Structures',  'MASTER', CURRENT_TIMESTAMP),
    ('EQUIPMENT_VIEW',          'View Equipment',         'MASTER', CURRENT_TIMESTAMP),
    ('EQUIPMENT_MANAGE',        'Manage Equipment',       'MASTER', CURRENT_TIMESTAMP),
    ('FACULTY_VIEW',            'View Faculty',           'MASTER', CURRENT_TIMESTAMP),
    ('FACULTY_MANAGE',          'Manage Faculty',         'MASTER', CURRENT_TIMESTAMP),
    ('AGENT_VIEW',              'View Agents',            'MASTER', CURRENT_TIMESTAMP),
    ('AGENT_MANAGE',            'Manage Agents',          'MASTER', CURRENT_TIMESTAMP),
    ('REFERRAL_TYPE_VIEW',      'View Referral Types',    'MASTER', CURRENT_TIMESTAMP),
    ('REFERRAL_TYPE_MANAGE',    'Manage Referral Types',  'MASTER', CURRENT_TIMESTAMP),
    ('COMMUNITY_VIEW',          'View Communities',       'MASTER', CURRENT_TIMESTAMP),
    ('COMMUNITY_MANAGE',        'Manage Communities',     'MASTER', CURRENT_TIMESTAMP),
    ('BLOOD_GROUP_VIEW',        'View Blood Groups',      'MASTER', CURRENT_TIMESTAMP),
    ('BLOOD_GROUP_MANAGE',      'Manage Blood Groups',    'MASTER', CURRENT_TIMESTAMP),
    ('SETTINGS_VIEW',           'View Settings',          'MASTER', CURRENT_TIMESTAMP),
    ('SETTINGS_MANAGE',         'Manage Settings',        'MASTER', CURRENT_TIMESTAMP);

-- ADMISSION
INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('ENQUIRY_VIEW',                'View Enquiries',              'ADMISSION', CURRENT_TIMESTAMP),
    ('ENQUIRY_CREATE',              'Create Enquiries',            'ADMISSION', CURRENT_TIMESTAMP),
    ('ENQUIRY_EDIT',                'Edit Enquiries',              'ADMISSION', CURRENT_TIMESTAMP),
    ('ENQUIRY_DELETE',              'Delete Enquiries',            'ADMISSION', CURRENT_TIMESTAMP),
    ('DOCUMENT_SUBMISSION_VIEW',    'View Document Submissions',   'ADMISSION', CURRENT_TIMESTAMP),
    ('DOCUMENT_SUBMISSION_MANAGE',  'Manage Document Submissions', 'ADMISSION', CURRENT_TIMESTAMP),
    ('ADMISSION_VIEW',              'View Admissions',             'ADMISSION', CURRENT_TIMESTAMP),
    ('ADMISSION_CREATE',            'Create Admissions',           'ADMISSION', CURRENT_TIMESTAMP),
    ('ADMISSION_EDIT',              'Edit Admissions',             'ADMISSION', CURRENT_TIMESTAMP),
    ('ADMISSION_DELETE',            'Delete Admissions',           'ADMISSION', CURRENT_TIMESTAMP),
    ('STUDENT_VIEW',                'View Students',               'ADMISSION', CURRENT_TIMESTAMP),
    ('STUDENT_CREATE',              'Create Students',             'ADMISSION', CURRENT_TIMESTAMP),
    ('STUDENT_EDIT',                'Edit Students',               'ADMISSION', CURRENT_TIMESTAMP),
    ('STUDENT_DELETE',              'Delete Students',             'ADMISSION', CURRENT_TIMESTAMP),
    ('ROLL_NUMBER_ASSIGN',          'Assign Roll Numbers',         'ADMISSION', CURRENT_TIMESTAMP),
    ('IMPORT_DATA',                 'Import Data',                 'ADMISSION', CURRENT_TIMESTAMP);

-- CURRICULUM
INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('SYLLABUS_VIEW',          'View Syllabi',            'CURRICULUM', CURRENT_TIMESTAMP),
    ('SYLLABUS_MANAGE',        'Manage Syllabi',          'CURRICULUM', CURRENT_TIMESTAMP),
    ('EXPERIMENT_VIEW',        'View Experiments',        'CURRICULUM', CURRENT_TIMESTAMP),
    ('EXPERIMENT_MANAGE',      'Manage Experiments',      'CURRICULUM', CURRENT_TIMESTAMP),
    ('COPO_VIEW',              'View CO/PO Mappings',     'CURRICULUM', CURRENT_TIMESTAMP),
    ('COPO_MANAGE',            'Manage CO/PO Mappings',   'CURRICULUM', CURRENT_TIMESTAMP),
    ('CURRICULUM_VIEW',        'View Curriculum',         'CURRICULUM', CURRENT_TIMESTAMP),
    ('CURRICULUM_MANAGE',      'Manage Curriculum',       'CURRICULUM', CURRENT_TIMESTAMP),
    ('LAB_SCHEDULE_VIEW',      'View Lab Schedules',      'CURRICULUM', CURRENT_TIMESTAMP),
    ('LAB_SCHEDULE_MANAGE',    'Manage Lab Schedules',    'CURRICULUM', CURRENT_TIMESTAMP),
    ('ATTENDANCE_VIEW',        'View Attendance',         'CURRICULUM', CURRENT_TIMESTAMP),
    ('ATTENDANCE_MANAGE',      'Manage Attendance',       'CURRICULUM', CURRENT_TIMESTAMP);

-- EXAMINATION
INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('EXAMINATION_VIEW',    'View Examinations',   'EXAMINATION', CURRENT_TIMESTAMP),
    ('EXAMINATION_MANAGE',  'Manage Examinations', 'EXAMINATION', CURRENT_TIMESTAMP),
    ('EXAM_RESULT_VIEW',    'View Exam Results',   'EXAMINATION', CURRENT_TIMESTAMP),
    ('EXAM_RESULT_MANAGE',  'Manage Exam Results', 'EXAMINATION', CURRENT_TIMESTAMP);

-- FINANCE
INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('STUDENT_FEE_VIEW',    'View Student Fees',    'FINANCE', CURRENT_TIMESTAMP),
    ('STUDENT_FEE_MANAGE',  'Manage Student Fees',  'FINANCE', CURRENT_TIMESTAMP),
    ('FEE_COLLECT',         'Collect Fees',         'FINANCE', CURRENT_TIMESTAMP),
    ('FEE_FINALIZE',        'Finalize Fees',        'FINANCE', CURRENT_TIMESTAMP);

-- INFRASTRUCTURE
INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('INVENTORY_VIEW',      'View Inventory',         'INFRASTRUCTURE', CURRENT_TIMESTAMP),
    ('INVENTORY_MANAGE',    'Manage Inventory',       'INFRASTRUCTURE', CURRENT_TIMESTAMP),
    ('MAINTENANCE_VIEW',    'View Maintenance',       'INFRASTRUCTURE', CURRENT_TIMESTAMP),
    ('MAINTENANCE_MANAGE',  'Manage Maintenance',     'INFRASTRUCTURE', CURRENT_TIMESTAMP);

-- REPORTS
INSERT INTO permissions (code, display_name, category, created_at) VALUES
    ('REPORT_VIEW',      'View Reports',     'REPORTS', CURRENT_TIMESTAMP),
    ('FEE_REPORT_VIEW',  'View Fee Reports', 'REPORTS', CURRENT_TIMESTAMP);

-- ============================================================
-- 3. ROLE PERMISSIONS
-- ============================================================

-- Helper: assign all permissions to DEV_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'DEV_ADMIN';

-- SUPPORT_ADMIN: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'SUPPORT_ADMIN';

-- ADMIN: all permissions (manages all client-side users including college_admin and below)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'ADMIN';

-- COLLEGE_ADMIN: VIEW on all MASTER + full ADMISSION + CURRICULUM + EXAMINATION + FINANCE + INFRASTRUCTURE + REPORTS
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'COLLEGE_ADMIN'
  AND (
      (p.category = 'MASTER' AND p.code LIKE '%_VIEW')
   OR p.category IN ('ADMISSION', 'CURRICULUM', 'EXAMINATION', 'FINANCE', 'INFRASTRUCTURE', 'REPORTS')
  );

-- COLLEGE_ADMIN: allow managing community and blood-group masters (used in admission + student flows).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'COLLEGE_ADMIN'
  AND p.code IN ('COMMUNITY_MANAGE', 'BLOOD_GROUP_MANAGE');

-- FRONT_OFFICE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'FRONT_OFFICE'
  AND p.code IN (
      'ENQUIRY_VIEW', 'ENQUIRY_CREATE', 'ENQUIRY_EDIT', 'ENQUIRY_DELETE',
      'DOCUMENT_SUBMISSION_VIEW', 'DOCUMENT_SUBMISSION_MANAGE',
      'ADMISSION_VIEW',
      'STUDENT_VIEW',
      'REPORT_VIEW'
  );

-- CASHIER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'CASHIER'
  AND p.code IN (
      'FEE_COLLECT', 'STUDENT_FEE_VIEW', 'FEE_REPORT_VIEW', 'ENQUIRY_VIEW'
  );

-- FACULTY
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'FACULTY'
  AND p.code IN (
      'CURRICULUM_VIEW', 'ATTENDANCE_VIEW', 'ATTENDANCE_MANAGE',
      'EXAMINATION_VIEW', 'EXAM_RESULT_VIEW', 'STUDENT_VIEW'
  );

-- LAB_INCHARGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'LAB_INCHARGE'
  AND p.code IN (
      'LAB_VIEW', 'LAB_MANAGE', 'LAB_SCHEDULE_VIEW', 'LAB_SCHEDULE_MANAGE', 'STUDENT_VIEW'
  );

-- TECHNICIAN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'TECHNICIAN'
  AND p.code IN (
      'EQUIPMENT_VIEW', 'INVENTORY_VIEW', 'MAINTENANCE_VIEW', 'LAB_VIEW'
  );

-- STUDENT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'STUDENT'
  AND p.code IN (
      'STUDENT_VIEW', 'ATTENDANCE_VIEW', 'EXAM_RESULT_VIEW'
  );

-- PARENT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name = 'PARENT'
  AND p.code IN (
      'STUDENT_VIEW', 'REPORT_VIEW'
  );

-- ============================================================
-- 4. SEED ADMIN APP USER (links to existing Keycloak 'admin')
-- ============================================================
INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by, created_at, updated_at)
SELECT 'admin', 'admin@cms.local', 'System Administrator', r.id, TRUE, 'system', current_timestamp, current_timestamp
FROM app_roles r
WHERE r.name = 'ADMIN';
