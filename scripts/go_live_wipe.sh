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
#   system_configurations  college.*, receipt.*, trust.*, BRANDING/RECEIPT
#   app_roles, permissions, role_permissions           (RBAC system config)
#   role_dashboard_widget_configs                      (role dashboard defaults)
#   flyway_schema_history                              (migration tracking)
#
# WIPED (all rows deleted, sequences reset to 1):
#   Students, faculty, agents, app_users
#   Enquiries, admissions, documents
#   Fee structures, payments, receipts
#   Departments, programs, courses, academic years
#   Labs, equipment, inventory, maintenance
#   Exam, attendance and result records
#   Scholarship applications and disbursements
#   Safety guidelines, incidents, PPE, audits
#   Audit logs and user dashboard customizations
#   Number sequences (counters reset for live)
#   system_configurations penalty and attendance policy keys
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
END $$;

-- ── WIPE: All transactional and structural data ──────────────────────────────
--
--   Listed roughly leaf-to-root; PostgreSQL resolves the internal ordering
--   automatically when multiple tables appear in one TRUNCATE statement.
--   RESTART IDENTITY resets PK sequences to 1.
--   CASCADE handles any FK-dependent child tables not explicitly listed.
--
--   Master reference tables (referral_types, communities, blood_groups,
--   india_states, india_districts, location_countries, scholarship_types,
--   app_roles, permissions, role_permissions, role_dashboard_widget_configs)
--   are intentionally ABSENT from this list — they will not be cascaded to.
-- ----------------------------------------------------------------------------

TRUNCATE TABLE

    -- Audit / activity logs
    audit_log,
    user_dashboard_widget_configs,

    -- Document audit history
    faculty_document_history,
    enquiry_document_history,

    -- Scholarship transactional
    scholarship_disbursements,
    student_scholarships,
    student_scholarship_eligibility,

    -- Finance transactional
    agent_commission_payouts,
    term_fee_payments,
    fee_demands,
    payment_receipts,
    receipt_number_sequence,
    enquiry_payments,
    fee_installments,
    penalties,
    semester_fees,
    student_fee_allocations,
    fee_payments,

    -- Fee structures (set up fresh at go-live)
    fee_structure_year_amounts,
    fee_structures,

    -- Exams and results
    exam_results,
    student_marks,
    semester_results,
    exam_events,
    exam_sessions,
    examinations,

    -- Lab operations
    lab_continuous_evaluations,
    lab_attendances,
    attendances,
    course_registrations,
    course_offerings,
    lab_schedules,
    lab_slots,
    student_term_enrollments,
    term_billing_schedules,
    lab_curriculum_mappings,

    -- Safety and maintenance
    safety_training_records,
    safety_audits,
    incident_reports,
    ppe_items,
    safety_guidelines,
    maintenance_requests,
    inventory_items,
    equipment,
    lab_incharge_assignments,

    -- Admissions and enquiries
    enquiry_status_history,
    enquiry_documents,
    admission_documents,
    applicant_documents,
    admissions,
    enquiries,

    -- People
    academic_qualifications,
    student_program_transfers,
    faculty_documents,
    faculty_document_type_requirements,
    app_users,
    students,
    faculty,
    agent_commission_guidelines,
    agents,

    -- Academic structure (configured fresh after go-live)
    term_instances,
    curriculum_semester_courses,
    curriculum_versions,
    calendar_events,
    intake_rules,
    cohorts,
    experiments,
    syllabi,
    subjects,
    program_document_types,
    courses,
    programs,
    labs,
    departments,
    academic_years,

    -- Number sequences (reset for live)
    application_number_sequences

RESTART IDENTITY CASCADE;

-- ── DELETE: Operational policy keys from system_configurations ───────────────
--   Keeps college.*, receipt.*, trust.* and other BRANDING / RECEIPT keys.
-- ---------------------------------------------------------------------------
DELETE FROM system_configurations
WHERE config_key IN (
    'penalty.daily_rate',
    'fee.late_penalty_percentage',
    'attendance.minimum_percentage'
);

-- ── SAFETY CHECK: Master data must still be present ─────────────────────────
DO $$
DECLARE
    v_referral  INTEGER;
    v_community INTEGER;
    v_blood     INTEGER;
    v_state     INTEGER;
    v_role      INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_referral  FROM referral_types;
    SELECT COUNT(*) INTO v_community FROM communities;
    SELECT COUNT(*) INTO v_blood     FROM blood_groups;
    SELECT COUNT(*) INTO v_state     FROM india_states;
    SELECT COUNT(*) INTO v_role      FROM app_roles;

    RAISE NOTICE '';
    RAISE NOTICE '=== Post-Wipe Master Data Verification ===';
    RAISE NOTICE 'referral_types  : % rows', v_referral;
    RAISE NOTICE 'communities     : % rows', v_community;
    RAISE NOTICE 'blood_groups    : % rows', v_blood;
    RAISE NOTICE 'india_states    : % rows', v_state;
    RAISE NOTICE 'app_roles       : % rows', v_role;
    RAISE NOTICE '';

    IF v_referral  = 0 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: referral_types is empty!'; END IF;
    IF v_community = 0 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: communities is empty!';    END IF;
    IF v_blood     = 0 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: blood_groups is empty!';   END IF;
    IF v_role      = 0 THEN RAISE EXCEPTION 'SAFETY CHECK FAILED: app_roles is empty!';      END IF;
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
    SELECT 'system_configurations',                     (SELECT COUNT(*) FROM system_configurations)                     UNION ALL
    SELECT 'app_roles',                                 (SELECT COUNT(*) FROM app_roles)                                 UNION ALL
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
echo "    • All students, faculty, agents and user accounts"
echo "    • All enquiries, admissions and documents"
echo "    • All departments, programs, courses and academic years"
echo "    • All fee structures, payments and receipts"
echo "    • All labs, equipment, inventory and maintenance records"
echo "    • All exam, attendance and result records"
echo "    • All scholarship applications and disbursements"
echo "    • All audit logs and activity history"
echo "    • All number sequences (reset to 0)"
echo ""
echo -e "${GREEN}  The following will be KEPT:${NC}"
echo "    • India states and districts"
echo "    • Community categories and blood groups"
echo "    • Referral types and scholarship type definitions"
echo "    • College branding system configuration"
echo "    • App roles, permissions and dashboard defaults"
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
    echo "  2.  Departments     →  Add your college's actual departments"
    echo "  3.  Programs        →  Add offered programs (B.Sc Nursing, M.Sc, GNM…)"
    echo "  4.  Courses         →  Add specialisations under each program"
    echo "  5.  Academic Year   →  Create 2025-2026, mark it as current"
    echo "  6.  Fee Structures  →  Configure fees per program per academic year"
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

