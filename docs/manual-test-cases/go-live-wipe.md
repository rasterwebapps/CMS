# Manual Test Cases — Go-Live Wipe and Master Refresh

## TC-GOLIVE-001: Dry-run shows upgraded master refresh SQL

**Preconditions:**
- Working copy contains `scripts/go_live_wipe.sh`
- No database connection is required

**Steps:**
1. Run `bash scripts/go_live_wipe.sh --dry-run` from the project root.
2. Verify the generated SQL includes `fee_structure_groups` in the `TRUNCATE TABLE` list.
3. Verify the generated SQL includes idempotent inserts/upserts for `location_countries`, `communities`, `blood_groups`, `referral_types`, `scholarship_types`, and `fee_states`.
4. Verify the generated SQL includes the `SPORTS` scholarship type.
5. Verify the generated SQL recreates only `devadmin`, `supportadmin`, and `collegeadmin` in `app_users`.
6. Verify the generated SQL deletes app roles outside `DEV_ADMIN`, `SUPPORT_ADMIN`, and `collegeadmin`.
7. Verify the generated report includes `fee_states`, `app_roles`, and `app_users`.

**Expected Result:**
- Dry-run completes without prompting for confirmation.
- The SQL reflects the upgraded BR-30 fee model, required default masters, and strict go-live RBAC.

**Actual Result:**

**Status:** NOT TESTED

## TC-GOLIVE-002: Go-live wipe preserves and repairs master data

**Preconditions:**
- PostgreSQL database has all Flyway migrations applied through `V172`.
- A database backup exists.
- User has confirmed this is not a production database unless intentionally executing the go-live wipe.

**Steps:**
1. Remove or deactivate one default master row in a safe test database, for example the `SPORTS` scholarship type or `TAMIL_NADU` fee state.
2. Run `scripts/go_live_wipe.sh` with the correct database connection arguments.
3. Type `WIPE` at the confirmation prompt.
4. After completion, query preserved masters: `referral_types`, `scholarship_types`, `fee_states`, `communities`, `blood_groups`, `location_countries`, `india_states`, and RBAC tables.
5. Confirm transactional and structural tables such as `students`, `enquiries`, `fee_structure_groups`, `fee_structures`, `specialities`, `programs`, `courses`, and `academic_years` are empty.
6. Query `app_users` and `app_roles`.

**Expected Result:**
- Default masters are present and active after the wipe.
- `SPORTS` scholarship exists.
- `TAMIL_NADU` is the default fee state and `OTHER_STATE` is the fallback fee state.
- Fee structure groups and fee structures are cleared for fresh go-live configuration.
- `app_users` contains exactly `devadmin`, `supportadmin`, and `collegeadmin`.
- `app_roles` contains exactly `DEV_ADMIN`, `SUPPORT_ADMIN`, and `collegeadmin`.

**Actual Result:**

**Status:** NOT TESTED

## TC-GOLIVE-003: College Admin can complete admission setup but cannot see platform roles

**Preconditions:**
- Go-live wipe has completed successfully.
- Keycloak has matching `devadmin`, `supportadmin`, and `collegeadmin` users.
- Frontend and backend are running against the wiped database.

**Steps:**
1. Log in as `collegeadmin`.
2. Verify Development Admin and Support Admin roles are not visible/assignable in role or user management flows.
3. Verify College Admin can create or manage required admission setup masters:
   - Specialities
   - Programs
   - Courses
   - Academic Year
   - Fee Structures
   - Faculty
   - Agents
   - Referral Types
   - Communities and Blood Groups
   - Countries / States / Districts
   - Settings and Number Sequences
4. Verify College Admin can complete the admission flow: enquiry creation, document submission/verification, admission creation/editing, student creation/editing, roll-number assignment, fee finalization, fee collection, and receipt viewing.
5. Verify College Admin cannot access unrelated platform/support functions such as Development Admin or Support Admin role assignment.

**Expected Result:**
- College Admin has the permissions needed to configure masters and complete admissions.
- Development Admin and Support Admin roles are hidden from College Admin.
- No unrelated platform/admin permissions are available to College Admin.

**Actual Result:**

**Status:** NOT TESTED
