-- ============================================================
-- 1. ROLES
-- ============================================================
INSERT INTO app_roles (name, display_name, hierarchy_level, is_system_role, description) VALUES
    ('DEV_ADMIN',      'Developer Admin',    1, TRUE,  'Full system access – development and infrastructure team only'),
    ('SUPPORT_ADMIN',  'Support Admin',      2, TRUE,  'Platform support access – Raster support team only'),
    ('ADMIN',          'Admin',              3, FALSE, 'College-level administrator; manages all college staff and operations'),
    ('COLLEGE_ADMIN',  'College Admin',      4, FALSE, 'Manages day-to-day college operations across all departments'),
    ('FRONT_OFFICE',   'Front Office',       5, FALSE, 'Handles enquiries, admissions and document submission'),
    ('CASHIER',        'Cashier',            5, FALSE, 'Processes fee collection and views financial summaries'),
    ('FACULTY',        'Faculty',            5, FALSE, 'Academic staff; manages curriculum, attendance and exam results'),
    ('LAB_INCHARGE',   'Lab In-charge',      5, FALSE, 'Manages laboratory resources and schedules'),
    ('TECHNICIAN',     'Technician',         5, FALSE, 'Maintains lab equipment and inventory'),
    ('STUDENT',        'Student',            6, FALSE, 'Enrolled student – read-only access to own data'),
    ('PARENT',         'Parent',             6, FALSE, 'Guardian – read-only access to ward data');

-- ============================================================
-- 2. PERMISSIONS
-- ============================================================

-- SYSTEM
INSERT INTO permissions (code, display_name, category) VALUES
    ('USER_VIEW',         'View Users',                'SYSTEM'),
    ('USER_CREATE',       'Create Users',              'SYSTEM'),
    ('USER_EDIT',         'Edit Users',                'SYSTEM'),
    ('USER_DEACTIVATE',   'Deactivate Users',          'SYSTEM'),
    ('ROLE_VIEW',         'View Roles',                'SYSTEM'),
    ('ROLE_CREATE',       'Create Roles',              'SYSTEM'),
    ('ROLE_EDIT',         'Edit Roles',                'SYSTEM'),
    ('PERMISSION_ASSIGN', 'Assign Permissions',        'SYSTEM');

-- MASTER
INSERT INTO permissions (code, display_name, category) VALUES
    ('DEPT_VIEW',               'View Departments',       'MASTER'),
    ('DEPT_MANAGE',             'Manage Departments',     'MASTER'),
    ('PROGRAM_VIEW',            'View Programs',          'MASTER'),
    ('PROGRAM_MANAGE',          'Manage Programs',        'MASTER'),
    ('COURSE_VIEW',             'View Courses',           'MASTER'),
    ('COURSE_MANAGE',           'Manage Courses',         'MASTER'),
    ('ACADEMIC_YEAR_VIEW',      'View Academic Years',    'MASTER'),
    ('ACADEMIC_YEAR_MANAGE',    'Manage Academic Years',  'MASTER'),
    ('SEMESTER_VIEW',           'View Semesters',         'MASTER'),
    ('SEMESTER_MANAGE',         'Manage Semesters',       'MASTER'),
    ('LAB_VIEW',                'View Labs',              'MASTER'),
    ('LAB_MANAGE',              'Manage Labs',            'MASTER'),
    ('FEE_STRUCTURE_VIEW',      'View Fee Structures',    'MASTER'),
    ('FEE_STRUCTURE_MANAGE',    'Manage Fee Structures',  'MASTER'),
    ('EQUIPMENT_VIEW',          'View Equipment',         'MASTER'),
    ('EQUIPMENT_MANAGE',        'Manage Equipment',       'MASTER'),
    ('FACULTY_VIEW',            'View Faculty',           'MASTER'),
    ('FACULTY_MANAGE',          'Manage Faculty',         'MASTER'),
    ('AGENT_VIEW',              'View Agents',            'MASTER'),
    ('AGENT_MANAGE',            'Manage Agents',          'MASTER'),
    ('REFERRAL_TYPE_VIEW',      'View Referral Types',    'MASTER'),
    ('REFERRAL_TYPE_MANAGE',    'Manage Referral Types',  'MASTER'),
    ('SETTINGS_VIEW',           'View Settings',          'MASTER'),
    ('SETTINGS_MANAGE',         'Manage Settings',        'MASTER');

-- ADMISSION
INSERT INTO permissions (code, display_name, category) VALUES
    ('ENQUIRY_VIEW',                'View Enquiries',              'ADMISSION'),
    ('ENQUIRY_CREATE',              'Create Enquiries',            'ADMISSION'),
    ('ENQUIRY_EDIT',                'Edit Enquiries',              'ADMISSION'),
    ('ENQUIRY_DELETE',              'Delete Enquiries',            'ADMISSION'),
    ('DOCUMENT_SUBMISSION_VIEW',    'View Document Submissions',   'ADMISSION'),
    ('DOCUMENT_SUBMISSION_MANAGE',  'Manage Document Submissions', 'ADMISSION'),
    ('ADMISSION_VIEW',              'View Admissions',             'ADMISSION'),
    ('ADMISSION_CREATE',            'Create Admissions',           'ADMISSION'),
    ('ADMISSION_EDIT',              'Edit Admissions',             'ADMISSION'),
    ('ADMISSION_DELETE',            'Delete Admissions',           'ADMISSION'),
    ('STUDENT_VIEW',                'View Students',               'ADMISSION'),
    ('STUDENT_CREATE',              'Create Students',             'ADMISSION'),
    ('STUDENT_EDIT',                'Edit Students',               'ADMISSION'),
    ('STUDENT_DELETE',              'Delete Students',             'ADMISSION'),
    ('ROLL_NUMBER_ASSIGN',          'Assign Roll Numbers',         'ADMISSION'),
    ('IMPORT_DATA',                 'Import Data',                 'ADMISSION');

-- CURRICULUM
INSERT INTO permissions (code, display_name, category) VALUES
    ('SYLLABUS_VIEW',          'View Syllabi',            'CURRICULUM'),
    ('SYLLABUS_MANAGE',        'Manage Syllabi',          'CURRICULUM'),
    ('EXPERIMENT_VIEW',        'View Experiments',        'CURRICULUM'),
    ('EXPERIMENT_MANAGE',      'Manage Experiments',      'CURRICULUM'),
    ('COPO_VIEW',              'View CO/PO Mappings',     'CURRICULUM'),
    ('COPO_MANAGE',            'Manage CO/PO Mappings',   'CURRICULUM'),
    ('CURRICULUM_VIEW',        'View Curriculum',         'CURRICULUM'),
    ('CURRICULUM_MANAGE',      'Manage Curriculum',       'CURRICULUM'),
    ('LAB_SCHEDULE_VIEW',      'View Lab Schedules',      'CURRICULUM'),
    ('LAB_SCHEDULE_MANAGE',    'Manage Lab Schedules',    'CURRICULUM'),
    ('ATTENDANCE_VIEW',        'View Attendance',         'CURRICULUM'),
    ('ATTENDANCE_MANAGE',      'Manage Attendance',       'CURRICULUM');

-- EXAMINATION
INSERT INTO permissions (code, display_name, category) VALUES
    ('EXAMINATION_VIEW',    'View Examinations',   'EXAMINATION'),
    ('EXAMINATION_MANAGE',  'Manage Examinations', 'EXAMINATION'),
    ('EXAM_RESULT_VIEW',    'View Exam Results',   'EXAMINATION'),
    ('EXAM_RESULT_MANAGE',  'Manage Exam Results', 'EXAMINATION');

-- FINANCE
INSERT INTO permissions (code, display_name, category) VALUES
    ('STUDENT_FEE_VIEW',    'View Student Fees',    'FINANCE'),
    ('STUDENT_FEE_MANAGE',  'Manage Student Fees',  'FINANCE'),
    ('FEE_COLLECT',         'Collect Fees',         'FINANCE'),
    ('FEE_FINALIZE',        'Finalize Fees',        'FINANCE');

-- INFRASTRUCTURE
INSERT INTO permissions (code, display_name, category) VALUES
    ('INVENTORY_VIEW',      'View Inventory',         'INFRASTRUCTURE'),
    ('INVENTORY_MANAGE',    'Manage Inventory',       'INFRASTRUCTURE'),
    ('MAINTENANCE_VIEW',    'View Maintenance',       'INFRASTRUCTURE'),
    ('MAINTENANCE_MANAGE',  'Manage Maintenance',     'INFRASTRUCTURE');

-- REPORTS
INSERT INTO permissions (code, display_name, category) VALUES
    ('REPORT_VIEW',      'View Reports',     'REPORTS'),
    ('FEE_REPORT_VIEW',  'View Fee Reports', 'REPORTS');

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
INSERT INTO app_users (keycloak_username, email, full_name, app_role_id, is_active, created_by)
SELECT 'admin', 'admin@cms.local', 'System Administrator', r.id, TRUE, 'system'
FROM app_roles r
WHERE r.name = 'ADMIN';
