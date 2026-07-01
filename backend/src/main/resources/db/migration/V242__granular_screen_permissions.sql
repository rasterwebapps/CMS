-- ============================================================
-- V242 — Granular Screen-Level Permissions
--
-- Splits every _MANAGE permission into physical button operations:
--   _CREATE  → Add button
--   _EDIT    → Edit button
--   _DELETE  → Delete button
--   _EXPORT  → Download / Export button (Excel, PDF, CSV)
--
-- Also adds _EXPORT to screens that already had separate _CREATE/_EDIT/_DELETE
-- (Enquiries, Admissions, Students, etc.).
--
-- Backfill: any role that held X_MANAGE is automatically granted the four
-- granular codes so no existing user loses access.
--
-- Tier: all new codes inherit the same tier as their parent _MANAGE permission
-- (resolved at insert time via a join). New _EXPORT codes default to tier 4.
-- ============================================================

-- ── ADMISSION ────────────────────────────────────────────────
INSERT INTO permissions (code, display_name, category, tier, created_at) VALUES
    ('ENQUIRY_EXPORT',             'Export Enquiries',              'ADMISSION', 4, NOW()),
    ('DOCUMENT_SUBMISSION_CREATE', 'Submit Documents',              'ADMISSION', 4, NOW()),
    ('DOCUMENT_SUBMISSION_EDIT',   'Edit Document Submissions',     'ADMISSION', 4, NOW()),
    ('DOCUMENT_SUBMISSION_DELETE', 'Delete Document Submissions',   'ADMISSION', 4, NOW()),
    ('ADMISSION_EXPORT',           'Export Admissions',             'ADMISSION', 4, NOW()),
    ('STUDENT_EXPORT',             'Export Students',               'ADMISSION', 4, NOW())
ON CONFLICT (code) DO NOTHING;

-- ── CURRICULUM ───────────────────────────────────────────────
INSERT INTO permissions (code, display_name, category, tier, created_at) VALUES
    ('SYLLABUS_CREATE',       'Add Syllabus',             'CURRICULUM', 4, NOW()),
    ('SYLLABUS_EDIT',         'Edit Syllabus',            'CURRICULUM', 4, NOW()),
    ('SYLLABUS_DELETE',       'Delete Syllabus',          'CURRICULUM', 4, NOW()),
    ('SYLLABUS_EXPORT',       'Export Syllabus',          'CURRICULUM', 4, NOW()),
    ('EXPERIMENT_CREATE',     'Add Experiment',           'CURRICULUM', 4, NOW()),
    ('EXPERIMENT_EDIT',       'Edit Experiment',          'CURRICULUM', 4, NOW()),
    ('EXPERIMENT_DELETE',     'Delete Experiment',        'CURRICULUM', 4, NOW()),
    ('EXPERIMENT_EXPORT',     'Export Experiments',       'CURRICULUM', 4, NOW()),
    ('COPO_CREATE',           'Add CO/PO Mapping',        'CURRICULUM', 4, NOW()),
    ('COPO_EDIT',             'Edit CO/PO Mapping',       'CURRICULUM', 4, NOW()),
    ('COPO_DELETE',           'Delete CO/PO Mapping',     'CURRICULUM', 4, NOW()),
    ('CURRICULUM_CREATE',     'Add Curriculum Version',   'CURRICULUM', 4, NOW()),
    ('CURRICULUM_EDIT',       'Edit Curriculum Version',  'CURRICULUM', 4, NOW()),
    ('CURRICULUM_DELETE',     'Delete Curriculum Version','CURRICULUM', 4, NOW()),
    ('LAB_SCHEDULE_CREATE',   'Add Lab Schedule',         'CURRICULUM', 4, NOW()),
    ('LAB_SCHEDULE_EDIT',     'Edit Lab Schedule',        'CURRICULUM', 4, NOW()),
    ('LAB_SCHEDULE_DELETE',   'Delete Lab Schedule',      'CURRICULUM', 4, NOW()),
    ('LAB_SCHEDULE_EXPORT',   'Export Lab Schedules',     'CURRICULUM', 4, NOW()),
    ('ATTENDANCE_CREATE',     'Record Attendance',        'CURRICULUM', 4, NOW()),
    ('ATTENDANCE_EDIT',       'Edit Attendance',          'CURRICULUM', 4, NOW()),
    ('ATTENDANCE_DELETE',     'Delete Attendance Record', 'CURRICULUM', 4, NOW()),
    ('ATTENDANCE_EXPORT',     'Export Attendance',        'CURRICULUM', 4, NOW())
ON CONFLICT (code) DO NOTHING;

-- ── EXAMINATION ──────────────────────────────────────────────
INSERT INTO permissions (code, display_name, category, tier, created_at) VALUES
    ('EXAMINATION_CREATE',   'Add Examination',         'EXAMINATION', 4, NOW()),
    ('EXAMINATION_EDIT',     'Edit Examination',        'EXAMINATION', 4, NOW()),
    ('EXAMINATION_DELETE',   'Delete Examination',      'EXAMINATION', 4, NOW()),
    ('EXAM_RESULT_CREATE',   'Enter Exam Result',       'EXAMINATION', 4, NOW()),
    ('EXAM_RESULT_EDIT',     'Edit Exam Result',        'EXAMINATION', 4, NOW()),
    ('EXAM_RESULT_DELETE',   'Delete Exam Result',      'EXAMINATION', 4, NOW()),
    ('EXAM_RESULT_EXPORT',   'Export Exam Results',     'EXAMINATION', 4, NOW())
ON CONFLICT (code) DO NOTHING;

-- ── FINANCE ──────────────────────────────────────────────────
INSERT INTO permissions (code, display_name, category, tier, created_at) VALUES
    ('STUDENT_FEE_CREATE',   'Add Fee Entry',           'FINANCE', 4, NOW()),
    ('STUDENT_FEE_EDIT',     'Edit Fee Entry',          'FINANCE', 4, NOW()),
    ('STUDENT_FEE_DELETE',   'Delete Fee Entry',        'FINANCE', 4, NOW()),
    ('STUDENT_FEE_EXPORT',   'Export Fee Records',      'FINANCE', 4, NOW()),
    ('RECEIPT_EXPORT',       'Export Receipts',         'FINANCE', 4, NOW()),
    ('COMMISSION_CREATE',    'Add Commission Payout',   'FINANCE', 4, NOW()),
    ('COMMISSION_EDIT',      'Edit Commission Payout',  'FINANCE', 4, NOW()),
    ('COMMISSION_DELETE',    'Delete Commission Payout','FINANCE', 4, NOW()),
    ('COMMISSION_EXPORT',    'Export Commissions',      'FINANCE', 4, NOW())
ON CONFLICT (code) DO NOTHING;

-- ── INFRASTRUCTURE ───────────────────────────────────────────
INSERT INTO permissions (code, display_name, category, tier, created_at) VALUES
    ('INVENTORY_CREATE',     'Add Inventory Item',      'INFRASTRUCTURE', 4, NOW()),
    ('INVENTORY_EDIT',       'Edit Inventory Item',     'INFRASTRUCTURE', 4, NOW()),
    ('INVENTORY_DELETE',     'Delete Inventory Item',   'INFRASTRUCTURE', 4, NOW()),
    ('INVENTORY_EXPORT',     'Export Inventory',        'INFRASTRUCTURE', 4, NOW()),
    ('MAINTENANCE_CREATE',   'Raise Maintenance Request','INFRASTRUCTURE',4, NOW()),
    ('MAINTENANCE_EDIT',     'Edit Maintenance Request','INFRASTRUCTURE', 4, NOW()),
    ('MAINTENANCE_DELETE',   'Delete Maintenance Request','INFRASTRUCTURE',4,NOW()),
    ('MAINTENANCE_EXPORT',   'Export Maintenance Log',  'INFRASTRUCTURE', 4, NOW())
ON CONFLICT (code) DO NOTHING;

-- ── LIBRARY ──────────────────────────────────────────────────
INSERT INTO permissions (code, display_name, category, tier, created_at) VALUES
    ('LIBRARY_CATALOGUE_CREATE', 'Add Book',               'LIBRARY', 4, NOW()),
    ('LIBRARY_CATALOGUE_EDIT',   'Edit Book',              'LIBRARY', 4, NOW()),
    ('LIBRARY_CATALOGUE_DELETE', 'Delete Book',            'LIBRARY', 4, NOW()),
    ('LIBRARY_CATALOGUE_EXPORT', 'Export Book Catalogue',  'LIBRARY', 4, NOW()),
    ('LIBRARY_ISSUE_CREATE',     'Issue Book',             'LIBRARY', 4, NOW()),
    ('LIBRARY_ISSUE_EDIT',       'Edit Issue Record',      'LIBRARY', 4, NOW()),
    ('LIBRARY_ISSUE_DELETE',     'Cancel Book Issue',      'LIBRARY', 4, NOW()),
    ('LIBRARY_FINE_CREATE',      'Add Library Fine',       'LIBRARY', 4, NOW()),
    ('LIBRARY_FINE_EDIT',        'Edit Library Fine',      'LIBRARY', 4, NOW()),
    ('LIBRARY_FINE_DELETE',      'Waive Library Fine',     'LIBRARY', 4, NOW()),
    ('LIBRARY_FINE_EXPORT',      'Export Library Fines',   'LIBRARY', 4, NOW()),
    ('LIBRARY_PERIODICAL_CREATE','Add Periodical',         'LIBRARY', 4, NOW()),
    ('LIBRARY_PERIODICAL_EDIT',  'Edit Periodical',        'LIBRARY', 4, NOW()),
    ('LIBRARY_PERIODICAL_DELETE','Delete Periodical',      'LIBRARY', 4, NOW()),
    ('LIBRARY_PERIODICAL_EXPORT','Export Periodicals',     'LIBRARY', 4, NOW()),
    ('LIBRARY_SETTINGS_EDIT',    'Edit Library Settings',  'LIBRARY', 4, NOW()),
    ('LIBRARY_ISSUE_EXPORT',     'Export Issue Records',   'LIBRARY', 4, NOW())
ON CONFLICT (code) DO NOTHING;

-- ── MASTER ───────────────────────────────────────────────────
INSERT INTO permissions (code, display_name, category, tier, created_at) VALUES
    ('DEPT_CREATE',              'Add Speciality',             'MASTER', 4, NOW()),
    ('DEPT_EDIT',                'Edit Speciality',            'MASTER', 4, NOW()),
    ('DEPT_DELETE',              'Delete Speciality',          'MASTER', 4, NOW()),
    ('DEPT_EXPORT',              'Export Specialities',        'MASTER', 4, NOW()),
    ('PROGRAM_CREATE',           'Add Program',                'MASTER', 4, NOW()),
    ('PROGRAM_EDIT',             'Edit Program',               'MASTER', 4, NOW()),
    ('PROGRAM_DELETE',           'Delete Program',             'MASTER', 4, NOW()),
    ('PROGRAM_EXPORT',           'Export Programs',            'MASTER', 4, NOW()),
    ('COURSE_CREATE',            'Add Course',                 'MASTER', 4, NOW()),
    ('COURSE_EDIT',              'Edit Course',                'MASTER', 4, NOW()),
    ('COURSE_DELETE',            'Delete Course',              'MASTER', 4, NOW()),
    ('COURSE_EXPORT',            'Export Courses',             'MASTER', 4, NOW()),
    ('ACADEMIC_YEAR_CREATE',     'Add Academic Year',          'MASTER', 4, NOW()),
    ('ACADEMIC_YEAR_EDIT',       'Edit Academic Year',         'MASTER', 4, NOW()),
    ('ACADEMIC_YEAR_DELETE',     'Delete Academic Year',       'MASTER', 4, NOW()),
    ('ACADEMIC_YEAR_EXPORT',     'Export Academic Years',      'MASTER', 4, NOW()),
    ('SEMESTER_CREATE',          'Add Semester',               'MASTER', 4, NOW()),
    ('SEMESTER_EDIT',            'Edit Semester',              'MASTER', 4, NOW()),
    ('SEMESTER_DELETE',          'Delete Semester',            'MASTER', 4, NOW()),
    ('LAB_CREATE',               'Add Lab',                    'MASTER', 4, NOW()),
    ('LAB_EDIT',                 'Edit Lab',                   'MASTER', 4, NOW()),
    ('LAB_DELETE',               'Delete Lab',                 'MASTER', 4, NOW()),
    ('LAB_EXPORT',               'Export Labs',                'MASTER', 4, NOW()),
    ('FEE_STRUCTURE_CREATE',     'Add Fee Structure',          'MASTER', 3, NOW()),
    ('FEE_STRUCTURE_EDIT',       'Edit Fee Structure',         'MASTER', 3, NOW()),
    ('FEE_STRUCTURE_DELETE',     'Delete Fee Structure',       'MASTER', 3, NOW()),
    ('FEE_STRUCTURE_EXPORT',     'Export Fee Structures',      'MASTER', 4, NOW()),
    ('EQUIPMENT_CREATE',         'Add Equipment',              'MASTER', 4, NOW()),
    ('EQUIPMENT_EDIT',           'Edit Equipment',             'MASTER', 4, NOW()),
    ('EQUIPMENT_DELETE',         'Delete Equipment',           'MASTER', 4, NOW()),
    ('EQUIPMENT_EXPORT',         'Export Equipment',           'MASTER', 4, NOW()),
    ('FACULTY_CREATE',           'Add Faculty',                'MASTER', 4, NOW()),
    ('FACULTY_EDIT',             'Edit Faculty',               'MASTER', 4, NOW()),
    ('FACULTY_DELETE',           'Delete Faculty',             'MASTER', 4, NOW()),
    ('FACULTY_EXPORT',           'Export Faculty List',        'MASTER', 4, NOW()),
    ('AGENT_CREATE',             'Add Agent',                  'MASTER', 4, NOW()),
    ('AGENT_EDIT',               'Edit Agent',                 'MASTER', 4, NOW()),
    ('AGENT_DELETE',             'Delete Agent',               'MASTER', 4, NOW()),
    ('AGENT_EXPORT',             'Export Agents',              'MASTER', 4, NOW()),
    ('REFERRAL_TYPE_CREATE',     'Add Referral Type',          'MASTER', 4, NOW()),
    ('REFERRAL_TYPE_EDIT',       'Edit Referral Type',         'MASTER', 4, NOW()),
    ('REFERRAL_TYPE_DELETE',     'Delete Referral Type',       'MASTER', 4, NOW()),
    ('REFERRAL_TYPE_EXPORT',     'Export Referral Types',      'MASTER', 4, NOW()),
    ('COMMUNITY_CREATE',         'Add Community',              'MASTER', 4, NOW()),
    ('COMMUNITY_EDIT',           'Edit Community',             'MASTER', 4, NOW()),
    ('COMMUNITY_DELETE',         'Delete Community',           'MASTER', 4, NOW()),
    ('COMMUNITY_EXPORT',         'Export Communities',         'MASTER', 4, NOW()),
    ('BLOOD_GROUP_CREATE',       'Add Blood Group',            'MASTER', 4, NOW()),
    ('BLOOD_GROUP_EDIT',         'Edit Blood Group',           'MASTER', 4, NOW()),
    ('BLOOD_GROUP_DELETE',       'Delete Blood Group',         'MASTER', 4, NOW()),
    ('BLOOD_GROUP_EXPORT',       'Export Blood Groups',        'MASTER', 4, NOW()),
    ('SETTINGS_CREATE',          'Add System Setting',         'MASTER', 2, NOW()),
    ('SETTINGS_EDIT',            'Edit System Setting',        'MASTER', 2, NOW()),
    ('SETTINGS_DELETE',          'Delete System Setting',      'MASTER', 2, NOW()),
    ('INSTITUTION_CREATE',       'Add Institution',            'MASTER', 2, NOW()),
    ('INSTITUTION_EDIT',         'Edit Institution',           'MASTER', 2, NOW()),
    ('INSTITUTION_DELETE',       'Delete Institution',         'MASTER', 2, NOW()),
    ('INDIA_LOCATION_CREATE',    'Add Location',               'MASTER', 2, NOW()),
    ('INDIA_LOCATION_EDIT',      'Edit Location',              'MASTER', 2, NOW()),
    ('INDIA_LOCATION_DELETE',    'Delete Location',            'MASTER', 2, NOW()),
    ('INDIA_LOCATION_EXPORT',    'Export Location Data',       'MASTER', 4, NOW()),
    ('DESIGNATION_CREATE',       'Add Designation',            'MASTER', 4, NOW()),
    ('DESIGNATION_EDIT',         'Edit Designation',           'MASTER', 4, NOW()),
    ('DESIGNATION_DELETE',       'Delete Designation',         'MASTER', 4, NOW()),
    ('DESIGNATION_EXPORT',       'Export Designations',        'MASTER', 4, NOW()),
    ('STAFF_REFERRER_CREATE',    'Add Staff Referrer',         'MASTER', 4, NOW()),
    ('STAFF_REFERRER_EDIT',      'Edit Staff Referrer',        'MASTER', 4, NOW()),
    ('STAFF_REFERRER_DELETE',    'Delete Staff Referrer',      'MASTER', 4, NOW()),
    ('STAFF_REFERRER_EXPORT',    'Export Staff Referrers',     'MASTER', 4, NOW())
ON CONFLICT (code) DO NOTHING;

-- ── SCHOLARSHIP ──────────────────────────────────────────────
INSERT INTO permissions (code, display_name, category, tier, created_at) VALUES
    ('SCHOLARSHIP_CREATE',   'Add Scholarship Type',    'SCHOLARSHIP', 4, NOW()),
    ('SCHOLARSHIP_EDIT',     'Edit Scholarship Type',   'SCHOLARSHIP', 4, NOW()),
    ('SCHOLARSHIP_DELETE',   'Delete Scholarship Type', 'SCHOLARSHIP', 4, NOW()),
    ('SCHOLARSHIP_EXPORT',   'Export Scholarship Data', 'SCHOLARSHIP', 4, NOW())
ON CONFLICT (code) DO NOTHING;

-- ── REPORTS ──────────────────────────────────────────────────
INSERT INTO permissions (code, display_name, category, tier, created_at) VALUES
    ('REPORT_EXPORT',        'Export General Reports',  'REPORTS', 4, NOW()),
    ('FEE_REPORT_EXPORT',    'Export Fee Reports',      'REPORTS', 4, NOW())
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- BACKFILL — any role with X_MANAGE gets all four granular codes
-- ============================================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT rp.role_id, p_new.id
FROM role_permissions rp
JOIN permissions p_old ON p_old.id = rp.permission_id
                       AND p_old.code LIKE '%\_MANAGE' ESCAPE '\'
JOIN permissions p_new ON p_new.code IN (
    REPLACE(p_old.code, '_MANAGE', '_CREATE'),
    REPLACE(p_old.code, '_MANAGE', '_EDIT'),
    REPLACE(p_old.code, '_MANAGE', '_DELETE'),
    REPLACE(p_old.code, '_MANAGE', '_EXPORT')
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Also backfill _EXPORT for roles with existing granular codes
-- (roles that had ENQUIRY_CREATE/EDIT/DELETE but not ENQUIRY_EXPORT)
-- ON CONFLICT required: multiple source codes (CREATE, EDIT, DELETE) can all map to the
-- same _EXPORT permission for the same role, so DISTINCT alone is not enough.
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT rp.role_id, p_exp.id
FROM role_permissions rp
JOIN permissions p_src ON p_src.id = rp.permission_id
                       AND p_src.code ~ '^[A-Z_]+_(CREATE|EDIT|DELETE|VIEW)$'
JOIN permissions p_exp ON p_exp.code = REGEXP_REPLACE(p_src.code, '_(CREATE|EDIT|DELETE|VIEW)$', '_EXPORT')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DEV_ADMIN / SUPPORT_ADMIN catch-all: ensure all new permissions are granted
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;
