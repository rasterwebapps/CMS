#!/usr/bin/env bash
# =============================================================================
# go_live_wipe.sh — CMS Go-Live Data Wipe Script
# =============================================================================
#
# PURPOSE:
#   Wipes all testing/demo data from the PostgreSQL database while preserving
#   the master and system data needed to run the live system.
#
# PRESERVED (NOT touched):
#   location_countries, india_states, india_districts  (geographic reference)
#   communities, blood_groups                          (demographics lookup)
#   referral_types                                     (enquiry source types)
#   scholarship_types                                  (scholarship definitions)
#   fee_states                                         (fee-segment lookup)
#   system_configurations  college.*, receipt.*, trust.*, BRANDING/RECEIPT
#   app_roles, permissions, role_permissions           (RBAC system config)
#   role_dashboard_widget_configs                      (role dashboard defaults)
#   flyway_schema_history                              (migration tracking)
#   Development Admin, Support Admin, College Admin    (recreated after wipe)
#
# WIPED (all rows deleted, sequences reset to 1):
#   Students, faculty, agents, non-bootstrap app_users
#   Enquiries, admissions, documents
#   Fee structures, payments, receipts
#   Specialities, programs, courses, academic years
#   Labs, equipment, inventory, maintenance
#   Exam, attendance and result records
#   Scholarship applications and disbursements
#   Safety guidelines, incidents, PPE, audits
#   Audit logs and user dashboard customizations
#   Number sequences (counters reset for live)
#   system_configurations penalty and attendance policy keys
#   Extra app roles outside DEV_ADMIN, SUPPORT_ADMIN and collegeadmin
#
# USAGE:
#   ./go_live_wipe.sh [OPTIONS]
#
# OPTIONS:
#   -h HOST       PostgreSQL host         (default: localhost)
#   -p PORT       PostgreSQL port         (default: 5432)
#   -d DATABASE   Database name           (default: cmsdb)
#   -u USER       PostgreSQL username     (default: cms)
#   -w PASSWORD   PostgreSQL password     (default: cms)
#   --dry-run     Print the SQL without executing it
#   --help        Show this help message
#
# EXAMPLES:
#   # Run with defaults (local Docker Compose on port 5432)
#   ./go_live_wipe.sh
#
#   # Remote server
#   ./go_live_wipe.sh -h 192.168.1.100 -p 5432 -d cmsdb -u cms -w secret
#
#   # Local Docker Compose (deploy/production-209 default port 5435)
#   ./go_live_wipe.sh -h localhost -p 5435 -d cmsdb -u cms -w cms
#
#   # Preview SQL only
#   ./go_live_wipe.sh --dry-run
#
# REQUIREMENTS:
#   psql (PostgreSQL client) must be installed.
#   The CMS backend must have been started at least once so all Flyway
#   migrations are applied before running this script.
#
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Defaults
# ---------------------------------------------------------------------------
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="cmsdb"
DB_USER="cms"
DB_PASS="cms"
DRY_RUN=false

# ---------------------------------------------------------------------------
# Colours
# ---------------------------------------------------------------------------
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# ---------------------------------------------------------------------------
# Parse arguments
# ---------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
    case "$1" in
        -h) DB_HOST="$2";  shift 2 ;;
        -p) DB_PORT="$2";  shift 2 ;;
        -d) DB_NAME="$2";  shift 2 ;;
        -u) DB_USER="$2";  shift 2 ;;
        -w) DB_PASS="$2";  shift 2 ;;
        --dry-run) DRY_RUN=true; shift ;;
        --help)
            grep "^#" "$0" | sed 's/^# \{0,1\}//'
            exit 0 ;;
        *)  echo -e "${RED}Unknown option: $1${NC}"; echo "Run with --help for usage."; exit 1 ;;
    esac
done

# ---------------------------------------------------------------------------
# Build the SQL string
# The heredoc avoids quoting nightmares when passing to psql --command.
# We write it to a temp file and pass via --file instead.
# ---------------------------------------------------------------------------
SQL_FILE="$(mktemp /tmp/cms_go_live_wipe_XXXXXX.sql)"
trap 'rm -f "$SQL_FILE"' EXIT

cat > "$SQL_FILE" << 'ENDSQL'
-- ==========================================================================
-- CMS Go-Live Wipe
-- ==========================================================================

BEGIN;

-- ── GUARD: Confirm this is the CMS database with migrations applied ─────────
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = 'public' AND table_name = 'flyway_schema_history'
    ) THEN
        RAISE EXCEPTION 'flyway_schema_history not found — wrong database? Aborting.';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = 'public' AND table_name = 'referral_types'
    ) THEN
        RAISE EXCEPTION 'referral_types not found — run the backend first to apply all migrations. Aborting.';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = 'public' AND table_name = 'fee_states'
    ) THEN
        RAISE EXCEPTION 'fee_states not found — run the backend first to apply BR-30 migrations. Aborting.';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = 'public' AND table_name = 'fee_structure_groups'
    ) THEN
        RAISE EXCEPTION 'fee_structure_groups not found — run the backend first to apply BR-30 migrations. Aborting.';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = 'public' AND table_name = 'scholarship_types'
    ) THEN
        RAISE EXCEPTION 'scholarship_types not found — run the backend first to apply scholarship migrations. Aborting.';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = 'public' AND table_name = 'location_countries'
    ) THEN
        RAISE EXCEPTION 'location_countries not found — run the backend first to apply location master migrations. Aborting.';
    END IF;
END $$;

-- ── WIPE: All transactional and structural data ──────────────────────────────
--
--   Listed roughly leaf-to-root; PostgreSQL resolves the internal ordering
--   automatically when multiple tables appear in one TRUNCATE statement.
--   RESTART IDENTITY resets PK sequences to 1.
--   CASCADE handles any FK-dependent child tables not explicitly listed.
--
--   Master reference tables (referral_types, communities, blood_groups,
--   india_states, india_districts, location_countries, scholarship_types, fee_states,
--   app_roles, permissions, role_permissions, role_dashboard_widget_configs)
--   are intentionally ABSENT from this list — they will not be cascaded to.
-- ----------------------------------------------------------------------------

DO $$
DECLARE
    v_requested_tables TEXT[] := ARRAY[
        -- Audit / activity logs
        'audit_log', 'user_dashboard_widget_configs',

        -- Document audit history
        'faculty_document_history', 'enquiry_document_history',

        -- Scholarship transactional
        'scholarship_disbursements', 'student_scholarships', 'student_scholarship_eligibility',

        -- Finance transactional
        'agent_commission_payouts', 'term_fee_payments', 'fee_demands', 'payment_receipts',
        'receipt_number_sequence', 'enquiry_payments', 'fee_installments', 'penalties',
        'semester_fees', 'student_fee_allocations', 'fee_payments',

        -- Fee structures (set up fresh at go-live)
        'fee_structure_year_amounts', 'fee_structures', 'fee_structure_groups',

        -- Exams and results
        'exam_results', 'student_marks', 'semester_results', 'exam_events', 'exam_sessions', 'examinations',

        -- Lab operations
        'lab_continuous_evaluations', 'lab_attendances', 'attendances', 'course_registrations',
        'course_offerings', 'lab_schedules', 'lab_slots', 'student_term_enrollments',
        'term_billing_schedules', 'lab_curriculum_mappings',

        -- Safety and maintenance
        'safety_training_records', 'safety_audits', 'incident_reports', 'ppe_items', 'safety_guidelines',
        'maintenance_requests', 'inventory_items', 'equipment', 'lab_incharge_assignments',

        -- Admissions and enquiries
        'enquiry_status_history', 'enquiry_documents', 'admission_documents', 'applicant_documents',
        'admissions', 'enquiries',

        -- People
        'academic_qualifications', 'student_program_transfers', 'faculty_documents',
        'faculty_document_type_requirements', 'app_users', 'students', 'faculty',
        'agent_commission_guidelines', 'agents',

        -- Academic structure (configured fresh after go-live)
        'term_instances', 'curriculum_semester_courses', 'curriculum_versions', 'calendar_events',
        'intake_rules', 'cohorts', 'experiments', 'syllabi', 'subjects', 'program_document_types',
        'courses', 'programs', 'labs', 'specialities', 'academic_years',

        -- Number sequences (reset for live)
        'application_number_sequences'
    ];
    v_existing_tables TEXT;
    v_missing_tables  TEXT;
BEGIN
    SELECT string_agg(format('%I', table_name), ', ')
    INTO v_existing_tables
    FROM unnest(v_requested_tables) AS table_name
    WHERE to_regclass(format('public.%I', table_name)) IS NOT NULL;

    SELECT string_agg(table_name, ', ')
    INTO v_missing_tables
    FROM unnest(v_requested_tables) AS table_name
    WHERE to_regclass(format('public.%I', table_name)) IS NULL;

    IF v_existing_tables IS NULL THEN
        RAISE EXCEPTION 'No wipe target tables exist — wrong database? Aborting.';
    END IF;

    IF v_missing_tables IS NOT NULL THEN
        RAISE NOTICE 'Skipping non-existent legacy/optional tables: %', v_missing_tables;
    END IF;

    EXECUTE 'TRUNCATE TABLE ' || v_existing_tables || ' RESTART IDENTITY CASCADE';
END $$;

-- ── STRICT RBAC: Go-live admin roles and users only ─────────────────────────
--   The wipe removes every app_user. Recreate only the three bootstrap users
--   that exist in Keycloak and keep only their DB roles for go-live.
-- ---------------------------------------------------------------------------

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

DELETE FROM app_roles
WHERE name NOT IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'collegeadmin');

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
), college_role AS (
    SELECT id
    FROM app_roles
    WHERE name = 'collegeadmin'
)
DELETE FROM role_permissions rp
USING college_role cr
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
SELECT u.keycloak_username, u.email, u.full_name, r.id, TRUE, 'go_live_wipe', NOW(), NOW()
FROM (
    VALUES
        ('devadmin', 'devadmin@cms.local', 'Developer Administrator', 'DEV_ADMIN'),
        ('supportadmin', 'supportadmin@cms.local', 'Support Administrator', 'SUPPORT_ADMIN'),
        ('collegeadmin', 'collegeadmin@cms.local', 'College Administrator', 'collegeadmin')
) AS u(keycloak_username, email, full_name, role_name)
JOIN app_roles r ON r.name = u.role_name
ON CONFLICT (keycloak_username) DO UPDATE
SET keycloak_user_id = NULL,
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    app_role_id = EXCLUDED.app_role_id,
    student_id = NULL,
    faculty_id = NULL,
    is_active = TRUE,
    updated_at = NOW();

-- ── DELETE: Operational policy keys from system_configurations ───────────────
--   Keeps college.*, receipt.*, trust.* and other BRANDING / RECEIPT keys.
-- ---------------------------------------------------------------------------
DELETE FROM system_configurations
WHERE config_key IN (
    'penalty.daily_rate',
    'fee.late_penalty_percentage',
    'attendance.minimum_percentage'
);

-- ── REFRESH: Required default masters for upgraded business rules ───────────
--   These tables are preserved, but production databases can be missing rows if
--   older seed data was edited or migrations pre-date newer defaults. Upserts are
--   idempotent and intentionally do not touch transactional/college structure data.
-- ---------------------------------------------------------------------------

INSERT INTO location_countries (id, name, iso_code, is_active, created_at, updated_at)
VALUES (1, 'India', 'IN', TRUE, NOW(), NOW())
ON CONFLICT (iso_code) DO UPDATE
SET name = EXCLUDED.name,
    is_active = TRUE,
    updated_at = NOW();

SELECT setval(
    pg_get_serial_sequence('location_countries', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM location_countries), 1),
    TRUE
);

INSERT INTO communities (name, code, description, is_active, created_at, updated_at)
VALUES
    ('Scheduled Caste', 'SC', 'Scheduled Caste', TRUE, NOW(), NOW()),
    ('Scheduled Tribe', 'ST', 'Scheduled Tribe', TRUE, NOW(), NOW()),
    ('Backward Caste', 'BC', 'Backward Caste', TRUE, NOW(), NOW()),
    ('Most Backward Caste', 'MBC', 'Most Backward Caste', TRUE, NOW(), NOW()),
    ('Denotified Communities', 'DNC', 'Denotified Communities', TRUE, NOW(), NOW()),
    ('Open Category', 'OC', 'Open / General Category', TRUE, NOW(), NOW()),
    ('Economically Weaker Section', 'EWS', 'Economically Weaker Section', TRUE, NOW(), NOW()),
    ('Others', 'OTHERS', 'Others', TRUE, NOW(), NOW())
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    is_active = TRUE,
    updated_at = NOW();

INSERT INTO blood_groups (name, code, is_active, created_at, updated_at)
VALUES
    ('A Positive', 'A+', TRUE, NOW(), NOW()),
    ('A Negative', 'A-', TRUE, NOW(), NOW()),
    ('B Positive', 'B+', TRUE, NOW(), NOW()),
    ('B Negative', 'B-', TRUE, NOW(), NOW()),
    ('O Positive', 'O+', TRUE, NOW(), NOW()),
    ('O Negative', 'O-', TRUE, NOW(), NOW()),
    ('AB Positive', 'AB+', TRUE, NOW(), NOW()),
    ('AB Negative', 'AB-', TRUE, NOW(), NOW())
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    is_active = TRUE,
    updated_at = NOW();

INSERT INTO referral_types (
    name, code, commission_amount, has_commission, description, is_active,
    is_system_defined, created_at, updated_at
)
VALUES
    ('Walk-In', 'WALK_IN', 0, FALSE, 'Direct walk-in enquiry', TRUE, FALSE, NOW(), NOW()),
    ('Phone', 'PHONE', 0, FALSE, 'Phone enquiry', TRUE, FALSE, NOW(), NOW()),
    ('Online', 'ONLINE', 0, FALSE, 'Online enquiry', TRUE, FALSE, NOW(), NOW()),
    ('Agent Referral', 'AGENT_REFERRAL', 0, TRUE, 'Referred by external agent', TRUE, TRUE, NOW(), NOW()),
    ('Staff', 'STAFF', 0, FALSE, 'Referred by staff member', TRUE, TRUE, NOW(), NOW()),
    ('Alumni', 'ALUMNI', 0, FALSE, 'Referred by alumni', TRUE, TRUE, NOW(), NOW()),
    ('Parent', 'PARENT', 0, FALSE, 'Referred by parent', TRUE, FALSE, NOW(), NOW()),
    ('Advertisement', 'ADVERTISEMENT', 0, FALSE, 'Through advertisement', TRUE, FALSE, NOW(), NOW()),
    ('Student Referral', 'STUDENT', 500, TRUE, 'Referred by a current student', TRUE, TRUE, NOW(), NOW()),
    ('Faculty Referral', 'FACULTY', 500, TRUE, 'Referred by a faculty member', TRUE, TRUE, NOW(), NOW())
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    commission_amount = EXCLUDED.commission_amount,
    has_commission = EXCLUDED.has_commission,
    description = EXCLUDED.description,
    is_active = TRUE,
    is_system_defined = referral_types.is_system_defined OR EXCLUDED.is_system_defined,
    updated_at = NOW();

INSERT INTO scholarship_types (
    code, name, description, govt_scheme, scheme_code, discount_type, discount_value,
    max_amount_per_year, renewal_required, is_active, application_mode, portal_name,
    portal_url, eligible_from_year, eligible_to_year, created_at, updated_at
)
VALUES
    ('FIRST_GRAD', 'First Graduate Scholarship', 'For students who are first in family to attend college', FALSE, NULL, 'FIXED_AMOUNT', 20000.00, 20000.00, FALSE, TRUE, 'INSTITUTION', NULL, NULL, 1, 1, NOW(), NOW()),
    ('SC_GOVT', 'SC Government Scholarship', 'Government scholarship for SC category students', TRUE, NULL, 'PERCENTAGE', 100.00, NULL, TRUE, TRUE, 'GOVT_PORTAL', 'ePass Tamil Nadu', 'https://tnepass.tn.gov.in', 1, NULL, NOW(), NOW()),
    ('ST_GOVT', 'ST Government Scholarship', 'Government scholarship for ST category students', TRUE, NULL, 'PERCENTAGE', 100.00, NULL, TRUE, TRUE, 'GOVT_PORTAL', 'ePass Tamil Nadu', 'https://tnepass.tn.gov.in', 1, NULL, NOW(), NOW()),
    ('OBC_GOVT', 'OBC Government Scholarship', 'Government scholarship for OBC category students', TRUE, NULL, 'FIXED_AMOUNT', 30000.00, 30000.00, TRUE, TRUE, 'GOVT_PORTAL', 'NSP', 'https://scholarships.gov.in', 1, NULL, NOW(), NOW()),
    ('BC_STATE', 'BC/MBC State Scholarship', 'State scholarship for BC/MBC category students', TRUE, NULL, 'FIXED_AMOUNT', 25000.00, 25000.00, TRUE, TRUE, 'GOVT_PORTAL', 'ePass Tamil Nadu', 'https://tnepass.tn.gov.in', 1, NULL, NOW(), NOW()),
    ('EWS', 'Economically Weaker Section', 'For students with annual family income below ₹3,00,000', FALSE, NULL, 'PERCENTAGE', 50.00, NULL, TRUE, TRUE, 'INSTITUTION', NULL, NULL, NULL, NULL, NOW(), NOW()),
    ('MERIT', 'Merit Scholarship', 'For students with high merit in qualifying examination', FALSE, NULL, 'FIXED_AMOUNT', 30000.00, 30000.00, FALSE, TRUE, 'INSTITUTION', NULL, NULL, NULL, NULL, NOW(), NOW()),
    ('SPORTS', 'Sports Scholarship', 'Institution scholarship for students admitted under sports quota', FALSE, NULL, 'FIXED_AMOUNT', 25000.00, 25000.00, FALSE, TRUE, 'INSTITUTION', NULL, NULL, NULL, NULL, NOW(), NOW())
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    govt_scheme = EXCLUDED.govt_scheme,
    scheme_code = EXCLUDED.scheme_code,
    discount_type = EXCLUDED.discount_type,
    discount_value = EXCLUDED.discount_value,
    max_amount_per_year = EXCLUDED.max_amount_per_year,
    renewal_required = EXCLUDED.renewal_required,
    is_active = TRUE,
    application_mode = EXCLUDED.application_mode,
    portal_name = EXCLUDED.portal_name,
    portal_url = EXCLUDED.portal_url,
    eligible_from_year = EXCLUDED.eligible_from_year,
    eligible_to_year = EXCLUDED.eligible_to_year,
    updated_at = NOW();

INSERT INTO fee_states (name, code, is_default, is_fallback, sort_order, is_active, created_at, updated_at)
VALUES
    ('Tamil Nadu', 'TAMIL_NADU', TRUE, FALSE, 1, TRUE, NOW(), NOW()),
    ('Other State', 'OTHER_STATE', FALSE, TRUE, 2, TRUE, NOW(), NOW())
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    is_default = EXCLUDED.is_default,
    is_fallback = EXCLUDED.is_fallback,
    sort_order = EXCLUDED.sort_order,
    is_active = TRUE,
    updated_at = NOW();

UPDATE fee_states SET is_default = FALSE WHERE code <> 'TAMIL_NADU' AND is_default = TRUE;
UPDATE fee_states SET is_fallback = FALSE WHERE code <> 'OTHER_STATE' AND is_fallback = TRUE;

-- ── SAFETY CHECK: Master data must still be present ─────────────────────────
DO $$
DECLARE
    v_country     INTEGER;
    v_state       INTEGER;
    v_referral    INTEGER;
    v_community   INTEGER;
    v_blood       INTEGER;
    v_scholarship INTEGER;
    v_fee_state   INTEGER;
    v_role        INTEGER;
    v_user        INTEGER;
    v_bad_role    INTEGER;
    v_bad_user    INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_country     FROM location_countries;
    SELECT COUNT(*) INTO v_state       FROM india_states;
    SELECT COUNT(*) INTO v_referral  FROM referral_types;
    SELECT COUNT(*) INTO v_community FROM communities;
    SELECT COUNT(*) INTO v_blood     FROM blood_groups;
    SELECT COUNT(*) INTO v_scholarship FROM scholarship_types;
    SELECT COUNT(*) INTO v_fee_state   FROM fee_states;
    SELECT COUNT(*) INTO v_role        FROM app_roles;
    SELECT COUNT(*) INTO v_user        FROM app_users;
    SELECT COUNT(*) INTO v_bad_role    FROM app_roles WHERE name NOT IN ('DEV_ADMIN', 'SUPPORT_ADMIN', 'collegeadmin');
    SELECT COUNT(*) INTO v_bad_user    FROM app_users WHERE keycloak_username NOT IN ('devadmin', 'supportadmin', 'collegeadmin');

    RAISE NOTICE '';
    RAISE NOTICE '=== Post-Wipe Master Data Verification ===';
    RAISE NOTICE 'location_countries : % rows', v_country;
    RAISE NOTICE 'india_states       : % rows', v_state;
    RAISE NOTICE 'referral_types  : % rows', v_referral;
    RAISE NOTICE 'communities     : % rows', v_community;
    RAISE NOTICE 'blood_groups    : % rows', v_blood;
    RAISE NOTICE 'scholarship_types : % rows', v_scholarship;
    RAISE NOTICE 'fee_states        : % rows', v_fee_state;
    RAISE NOTICE 'app_roles         : % rows', v_role;
    RAISE NOTICE 'app_users         : % rows', v_user;
    RAISE NOTICE '';

    IF v_country     = 0 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: location_countries is empty!'; END IF;
    IF v_state       = 0 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: india_states is empty!';       END IF;
    IF v_referral    = 0 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: referral_types is empty!';     END IF;
    IF v_community   = 0 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: communities is empty!';        END IF;
    IF v_blood       = 0 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: blood_groups is empty!';       END IF;
    IF v_scholarship = 0 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: scholarship_types is empty!';  END IF;
    IF v_fee_state   = 0 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: fee_states is empty!';         END IF;
    IF v_role       <> 3 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: app_roles must contain exactly 3 go-live roles; found %!', v_role; END IF;
    IF v_user       <> 3 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: app_users must contain exactly 3 go-live users; found %!', v_user; END IF;
    IF v_bad_role    > 0 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: extra app_roles found!';       END IF;
    IF v_bad_user    > 0 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: extra app_users found!';       END IF;
END $$;

COMMIT;

-- ── REPORT: Surviving master data row counts ─────────────────────────────────
SELECT table_name AS "Preserved Table", row_count AS "Rows Remaining"
FROM (
    SELECT 'location_countries'          AS table_name, (SELECT COUNT(*) FROM location_countries)          AS row_count UNION ALL
    SELECT 'india_states',                              (SELECT COUNT(*) FROM india_states)                              UNION ALL
    SELECT 'india_districts',                           (SELECT COUNT(*) FROM india_districts)                           UNION ALL
    SELECT 'communities',                               (SELECT COUNT(*) FROM communities)                               UNION ALL
    SELECT 'blood_groups',                              (SELECT COUNT(*) FROM blood_groups)                              UNION ALL
    SELECT 'referral_types',                            (SELECT COUNT(*) FROM referral_types)                            UNION ALL
    SELECT 'scholarship_types',                         (SELECT COUNT(*) FROM scholarship_types)                         UNION ALL
    SELECT 'fee_states',                                (SELECT COUNT(*) FROM fee_states)                                UNION ALL
    SELECT 'system_configurations',                     (SELECT COUNT(*) FROM system_configurations)                     UNION ALL
    SELECT 'app_roles',                                 (SELECT COUNT(*) FROM app_roles)                                 UNION ALL
    SELECT 'app_users',                                 (SELECT COUNT(*) FROM app_users)                                 UNION ALL
    SELECT 'permissions',                               (SELECT COUNT(*) FROM permissions)                               UNION ALL
    SELECT 'role_permissions',                          (SELECT COUNT(*) FROM role_permissions)                          UNION ALL
    SELECT 'role_dashboard_widget_configs',             (SELECT COUNT(*) FROM role_dashboard_widget_configs)
) AS summary
ORDER BY table_name;
ENDSQL

# ---------------------------------------------------------------------------
# Dry-run: print the SQL and exit
# ---------------------------------------------------------------------------
if [[ "$DRY_RUN" == true ]]; then
    echo -e "${CYAN}${BOLD}[DRY RUN] SQL that would be executed:${NC}"
    echo ""
    cat "$SQL_FILE"
    echo ""
    echo -e "${YELLOW}Re-run without --dry-run to execute for real.${NC}"
    exit 0
fi

# ---------------------------------------------------------------------------
# Safety confirmation prompt
# ---------------------------------------------------------------------------
echo ""
echo -e "${RED}${BOLD}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${RED}${BOLD}║          ⚠   CMS GO-LIVE WIPE — DESTRUCTIVE  ⚠           ║${NC}"
echo -e "${RED}${BOLD}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  Database  :  ${BOLD}${DB_NAME}${NC}  @  ${BOLD}${DB_HOST}:${DB_PORT}${NC}"
echo -e "  User      :  ${BOLD}${DB_USER}${NC}"
echo ""
echo -e "${YELLOW}  This will permanently DELETE all test data including:${NC}"
echo "    • All students, faculty, agents and non-bootstrap user accounts"
echo "    • All enquiries, admissions and documents"
echo "    • All specialities, programs, courses and academic years"
echo "    • All fee structures, payments and receipts"
echo "    • All labs, equipment, inventory and maintenance records"
echo "    • All exam, attendance and result records"
echo "    • All scholarship applications and disbursements"
echo "    • All audit logs and activity history"
echo "    • All number sequences (reset to 0)"
echo "    • All roles except Development Admin, Support Admin and College Admin"
echo ""
echo -e "${GREEN}  The following will be KEPT:${NC}"
echo "    • India states and districts"
echo "    • Community categories and blood groups"
echo "    • Referral types, scholarship type definitions and fee states"
echo "    • Development Admin, Support Admin and College Admin users/roles"
echo "    • College branding system configuration"
echo "    • Permissions and dashboard defaults for the remaining three roles"
echo ""
echo -e "${RED}${BOLD}  THIS CANNOT BE UNDONE. Take a database backup first if needed.${NC}"
echo ""
read -r -p "  Type WIPE to confirm and proceed: " CONFIRM
echo ""

if [[ "$CONFIRM" != "WIPE" ]]; then
    echo -e "${YELLOW}Aborted. No changes were made.${NC}"
    exit 0
fi

# ---------------------------------------------------------------------------
# Execute
# ---------------------------------------------------------------------------
echo -e "${CYAN}Connecting to database and running wipe...${NC}"
echo ""

export PGPASSWORD="$DB_PASS"

psql \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --dbname="$DB_NAME" \
    --username="$DB_USER" \
    --no-password \
    --echo-errors \
    --set ON_ERROR_STOP=1 \
    --file "$SQL_FILE"

EXIT_CODE=$?

if [[ $EXIT_CODE -eq 0 ]]; then
    echo ""
    echo -e "${GREEN}${BOLD}✅  Go-live wipe completed successfully.${NC}"
    echo ""
    echo -e "${CYAN}Next steps — complete in this order before taking admissions:${NC}"
    echo "  1.  Settings        →  Verify college name, address, email, phone"
    echo "  2.  Specialities     →  Add your college's actual specialities"
    echo "  3.  Programs        →  Add offered programs (B.Sc Nursing, M.Sc, GNM…)"
    echo "  4.  Courses         →  Add specialisations under each program"
    echo "  5.  Academic Year   →  Create 2025-2026, mark it as current"
    echo "  6.  Fee Structures  →  Configure fees per program/course/year/quota/fee state/gender"
    echo "  7.  Labs            →  Add laboratories (if applicable)"
    echo "  8.  Faculty         →  Add actual faculty records"
    echo "  9.  Agents          →  Add recruitment agents (if applicable)"
    echo " 10.  Go live         →  Begin taking real enquiries and admissions"
    echo ""
else
    echo ""
    echo -e "${RED}${BOLD}❌  Wipe FAILED (exit code: $EXIT_CODE).${NC}"
    echo "The transaction was rolled back. No data was changed."
    echo "Check the error output above and retry."
    exit $EXIT_CODE
fi

