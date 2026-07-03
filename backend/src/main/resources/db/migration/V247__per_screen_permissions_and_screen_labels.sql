-- ============================================================
-- V247: Per-screen permission isolation + screen_label column
-- ============================================================
-- 1. Adds screen_label column so each permission declares which screen it belongs to.
-- 2. Populates screen_label for all existing permissions.
-- 3. Inserts 7 new permissions so every nav screen has its own exclusive rights.
-- 4. Auto-assigns new permissions to roles that held the old shared codes.
-- 5. DEV_ADMIN / SUPPORT_ADMIN catch-all sync.
-- ============================================================

-- ── 1. Schema ─────────────────────────────────────────────────────────────────
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS screen_label VARCHAR(100);

-- ── 2. Screen labels for existing permissions ─────────────────────────────────

-- SYSTEM
UPDATE permissions SET screen_label = 'Users'               WHERE code LIKE 'USER_%';
UPDATE permissions SET screen_label = 'Roles & Permissions' WHERE code IN ('ROLE_VIEW','ROLE_CREATE','ROLE_EDIT','PERMISSION_ASSIGN');
UPDATE permissions SET screen_label = 'Permission Tiers'    WHERE code = 'PERMISSION_TIER_MANAGE';
UPDATE permissions SET screen_label = 'Dashboard'           WHERE code IN ('DASHBOARD','DASHBOARD_CUSTOMIZE');

-- MASTER — order matters: more specific prefixes first
UPDATE permissions SET screen_label = 'Specialities'    WHERE code LIKE 'DEPT_%';
UPDATE permissions SET screen_label = 'Programs'        WHERE code LIKE 'PROGRAM_%';
UPDATE permissions SET screen_label = 'Courses'         WHERE code LIKE 'COURSE_%';
UPDATE permissions SET screen_label = 'Academic Years'  WHERE code LIKE 'ACADEMIC_YEAR_%';
UPDATE permissions SET screen_label = 'Semesters'       WHERE code LIKE 'SEMESTER_%';
UPDATE permissions SET screen_label = 'Lab Schedules'   WHERE code LIKE 'LAB_SCHEDULE_%';
UPDATE permissions SET screen_label = 'Labs'            WHERE code LIKE 'LAB_%'
                                                          AND code NOT LIKE 'LAB_SCHEDULE_%';
UPDATE permissions SET screen_label = 'Fee Structures'  WHERE code LIKE 'FEE_STRUCTURE_%';
UPDATE permissions SET screen_label = 'Equipment'       WHERE code LIKE 'EQUIPMENT_%';
UPDATE permissions SET screen_label = 'Faculty'         WHERE code IN (
    'FACULTY_VIEW','FACULTY_CREATE','FACULTY_EDIT','FACULTY_DELETE','FACULTY_EXPORT','FACULTY_MANAGE');
UPDATE permissions SET screen_label = 'Agents'          WHERE code LIKE 'AGENT_%';
UPDATE permissions SET screen_label = 'Referral Types'  WHERE code LIKE 'REFERRAL_TYPE_%';
UPDATE permissions SET screen_label = 'Communities'     WHERE code LIKE 'COMMUNITY_%';
UPDATE permissions SET screen_label = 'Blood Groups'    WHERE code LIKE 'BLOOD_GROUP_%';
UPDATE permissions SET screen_label = 'Settings'        WHERE code LIKE 'SETTINGS_%';
UPDATE permissions SET screen_label = 'Location Master' WHERE code LIKE 'INDIA_LOCATION_%';
UPDATE permissions SET screen_label = 'Designations'   WHERE code LIKE 'DESIGNATION_%';
UPDATE permissions SET screen_label = 'Institutions'   WHERE code LIKE 'INSTITUTION_%';
UPDATE permissions SET screen_label = 'Number Sequences' WHERE code = 'NUMBER_SEQUENCE_VIEW';
UPDATE permissions SET screen_label = 'Number Series'  WHERE code IN ('NUMBER_SERIES_VIEW','NUMBER_SERIES_MANAGE');
UPDATE permissions SET screen_label = 'Countries'      WHERE code = 'COUNTRY_MANAGE';
UPDATE permissions SET screen_label = 'Staff Referrers' WHERE code LIKE 'STAFF_REFERRER_%';

-- ADMISSION
UPDATE permissions SET screen_label = 'Enquiries'          WHERE code LIKE 'ENQUIRY_%';
UPDATE permissions SET screen_label = 'Submit Documents'    WHERE code LIKE 'DOCUMENT_SUBMISSION_%';
UPDATE permissions SET screen_label = 'Verify Documents'    WHERE code IN ('DOCUMENT_VERIFICATION_MANAGE','DOCUMENT_VERIFIED_OVERRIDE');
UPDATE permissions SET screen_label = 'Admission Explorer'  WHERE code LIKE 'ADMISSION_%';
UPDATE permissions SET screen_label = 'Student Explorer'    WHERE code LIKE 'STUDENT_%'
                                                              AND code NOT LIKE 'STUDENT_FEE_%';
UPDATE permissions SET screen_label = 'Fee Explorer'        WHERE code LIKE 'STUDENT_FEE_%';
UPDATE permissions SET screen_label = 'Assign Roll Numbers' WHERE code = 'ROLL_NUMBER_ASSIGN';
UPDATE permissions SET screen_label = 'Retro Admit'         WHERE code IN ('RETRO_ADMIT','LEGACY_ADMIT');
UPDATE permissions SET screen_label = 'Data Import'         WHERE code = 'IMPORT_DATA';

-- SCHOLARSHIP
UPDATE permissions SET screen_label = 'Scholarship Types'        WHERE code IN (
    'SCHOLARSHIP_VIEW','SCHOLARSHIP_MANAGE','SCHOLARSHIP_CREATE','SCHOLARSHIP_EDIT',
    'SCHOLARSHIP_DELETE','SCHOLARSHIP_EXPORT');
UPDATE permissions SET screen_label = 'Scholarship Applications' WHERE code IN (
    'SCHOLARSHIP_APPLY','SCHOLARSHIP_APPROVE','SCHOLARSHIP_DISBURSE');

-- FINANCE
UPDATE permissions SET screen_label = 'Collect Payment' WHERE code = 'FEE_COLLECT';
UPDATE permissions SET screen_label = 'Finalize Fee'    WHERE code = 'FEE_FINALIZE';
UPDATE permissions SET screen_label = 'Receipts'        WHERE code LIKE 'RECEIPT_%';
UPDATE permissions SET screen_label = 'Refunds'         WHERE code IN ('FEE_REFUND','FEE_REFUND_APPROVE');
UPDATE permissions SET screen_label = 'Commissions'     WHERE code LIKE 'COMMISSION_%';

-- CURRICULUM
UPDATE permissions SET screen_label = 'Syllabus'             WHERE code LIKE 'SYLLABUS_%';
UPDATE permissions SET screen_label = 'Experiments'          WHERE code LIKE 'EXPERIMENT_%';
UPDATE permissions SET screen_label = 'CO/PO Mapping'        WHERE code LIKE 'COPO_%';
UPDATE permissions SET screen_label = 'Curriculum Versions'  WHERE code LIKE 'CURRICULUM_%';
UPDATE permissions SET screen_label = 'Attendance'           WHERE code LIKE 'ATTENDANCE_%';

-- EXAMINATION
UPDATE permissions SET screen_label = 'Manage Exams' WHERE code LIKE 'EXAMINATION_%';
UPDATE permissions SET screen_label = 'Exam Results' WHERE code LIKE 'EXAM_RESULT_%';

-- LIBRARY
UPDATE permissions SET screen_label = 'Book Catalogue'   WHERE code LIKE 'LIBRARY_CATALOGUE_%';
UPDATE permissions SET screen_label = 'Issue Desk'       WHERE code LIKE 'LIBRARY_ISSUE_%';
UPDATE permissions SET screen_label = 'Journals'         WHERE code LIKE 'LIBRARY_PERIODICAL_%';
UPDATE permissions SET screen_label = 'Fines'            WHERE code LIKE 'LIBRARY_FINE_%';
UPDATE permissions SET screen_label = 'Library Reports'  WHERE code = 'LIBRARY_REPORT_VIEW';
UPDATE permissions SET screen_label = 'Import Books'     WHERE code = 'LIBRARY_IMPORT';
UPDATE permissions SET screen_label = 'Library Settings' WHERE code LIKE 'LIBRARY_SETTINGS_%';

-- INFRASTRUCTURE
UPDATE permissions SET screen_label = 'Inventory'   WHERE code LIKE 'INVENTORY_%';
UPDATE permissions SET screen_label = 'Maintenance' WHERE code LIKE 'MAINTENANCE_%';

-- REPORTS
UPDATE permissions SET screen_label = 'General Reports' WHERE code IN ('REPORT_VIEW','REPORT_EXPORT');
UPDATE permissions SET screen_label = 'Fee Reports'     WHERE code IN ('FEE_REPORT_VIEW','FEE_REPORT_EXPORT');

-- ── 3. New screen-exclusive permissions ───────────────────────────────────────
INSERT INTO permissions (code, display_name, category, screen_label, tier, created_at) VALUES
    ('ACADEMIC_CALENDAR_VIEW',    'View Academic Calendar',         'MASTER',    'Academic Calendar',   4, CURRENT_TIMESTAMP),
    ('ACADEMIC_CALENDAR_MANAGE',  'Manage Academic Calendar',       'MASTER',    'Academic Calendar',   3, CURRENT_TIMESTAMP),
    ('ADMISSION_COMPLETE',        'Complete Admission',             'ADMISSION', 'Complete Admission',  3, CURRENT_TIMESTAMP),
    ('FACULTY_DOC_CONFIG_VIEW',   'View Faculty Document Config',   'MASTER',    'Faculty Doc Config',  4, CURRENT_TIMESTAMP),
    ('FACULTY_DOC_CONFIG_MANAGE', 'Manage Faculty Document Config', 'MASTER',    'Faculty Doc Config',  3, CURRENT_TIMESTAMP),
    ('MY_LIBRARY_VIEW',           'View My Library',                'LIBRARY',   'My Library',          4, CURRENT_TIMESTAMP),
    ('LIBRARY_QUICK_ISSUE',       'Issue Book (Quick Access)',      'LIBRARY',   'Issue Book',          3, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- ── 4. Auto-assign new permissions to roles that held the old shared codes ────

-- ACADEMIC_CALENDAR_VIEW → roles that had ACADEMIC_YEAR_VIEW
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'ACADEMIC_YEAR_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'ACADEMIC_CALENDAR_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- ACADEMIC_CALENDAR_MANAGE → roles that had ACADEMIC_YEAR_MANAGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'ACADEMIC_YEAR_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'ACADEMIC_CALENDAR_MANAGE') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- ADMISSION_COMPLETE → roles that had ADMISSION_CREATE
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'ADMISSION_CREATE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'ADMISSION_COMPLETE') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- FACULTY_DOC_CONFIG_VIEW → roles that had FACULTY_VIEW
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'FACULTY_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'FACULTY_DOC_CONFIG_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- FACULTY_DOC_CONFIG_MANAGE → roles that had FACULTY_MANAGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'FACULTY_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'FACULTY_DOC_CONFIG_MANAGE') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- MY_LIBRARY_VIEW → roles that had LIBRARY_ISSUE_VIEW
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LIBRARY_ISSUE_VIEW'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'MY_LIBRARY_VIEW') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- LIBRARY_QUICK_ISSUE → roles that had LIBRARY_ISSUE_MANAGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, new_p.id
FROM role_permissions rp
JOIN permissions old_p ON rp.permission_id = old_p.id AND old_p.code = 'LIBRARY_ISSUE_MANAGE'
CROSS JOIN (SELECT id FROM permissions WHERE code = 'LIBRARY_QUICK_ISSUE') new_p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions x WHERE x.role_id = rp.role_id AND x.permission_id = new_p.id
);

-- ── 5. DEV_ADMIN / SUPPORT_ADMIN catch-all sync ───────────────────────────────
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM app_roles r, permissions p
WHERE r.name IN ('DEV_ADMIN', 'SUPPORT_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
