# Functional Requirements Document — Student Management

**App:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing

## 1. Overview

Covers the `Student` master entity: CRUD, the Student Explorer list screen, roll-number assignment/generation, and program transfer. Backend package `com.cms.{model,dto,controller,service,repository}`, frontend `frontend/src/app/features/student/`.

## 2. Actors & Permissions

Permission codes as seeded in `V88__seed_roles_and_permissions.sql` and used by `StudentController`:

| Permission | Purpose |
|---|---|
| `STUDENT_VIEW` | Base view access (also incorrectly gates the update endpoint — see SRS Known Gaps) |
| `STUDENT_CREATE` | Create student, assign/generate/preview/bulk-assign roll numbers, delete (delete reuses this code rather than a dedicated `STUDENT_DELETE`) |
| `STUDENT_EDIT` | Program-transfer analysis/execution; transfer-history view (alongside `STUDENT_VIEW`) |
| `STUDENT_EXPORT` | Excel/PDF export from the Explorer |
| `STUDENT_DELETE` | Seeded (V88) but not referenced by any controller — orphaned permission |

Roles are assigned to these permissions exclusively through the DB-driven Role Management module (not covered here, per CLAUDE.md's mandatory DB-only role pattern).

## 3. Screens & UI Behavior

### 3.1 Student List / Explorer (`student-list.component`)
- Material table with server-side pagination (`MatPaginator`) and sorting (`MatSort`), column picker, resizable/wrap-text columns.
- Filters: program, course, academic year, status, student type, free-text search (min length enforced — `SEARCH_MIN_LENGTH = 3`, debounced).
- Columns: full name (with avatar initials), admission number, roll number, program name, year of study, admission date, status (badge), phone, email, university registration number, lab batch.
- Sort keys map UI columns to backend sort fields (e.g. `fullName` → `firstName`, `programName` → `program.name`).
- Row actions (view/edit/delete) gated per-row by `PermissionService` checks reflecting the endpoint permissions above.
- Export button offers Excel/PDF, applying the same active filters/sort as the on-screen query.
- Empty state component shown when no results match.

### 3.2 Student Form
- Create/edit form covering: core identity (roll number optional, admission number read-only/system-generated, university reg./UMIS numbers), program/course/speciality selection, semester, admission date, lab batch, status; personal (DOB, gender, Aadhar, admission category, student type); demographics (nationality, religion, community category, caste, blood group, physical disability flag); family (father/mother name/phone/email, parent mobile, first-graduate flag, parent education); embedded address; bank details (holder, account number, IFSC, branch, name, account type).
- Server-side validation via Jakarta Bean Validation on `StudentRequest` (`@NotBlank`/`@Email`/`@Size`/`@NotNull`/`@Positive`) — first name, last name, email, program ID, year of study, admission date are mandatory; most demographic/family/bank fields are optional.

### 3.3 Roll Number Assignment (`roll-number-assignment`)
- Lists students missing a roll number (`GET /students/without-roll-number`, filterable by course/program).
- **Preview** (`POST /students/preview-roll-numbers`) computes the proposed sequence without persisting.
- **Generate** (`POST /students/generate-roll-numbers`) commits the same computation and increments the per-course-per-year counter under a pessimistic lock.
- A single student can also be assigned/corrected manually (`PATCH /students/{id}/roll-number`) or in a batch (`POST /students/bulk-assign-roll-numbers`) bypassing generation.

### 3.4 Program Transfer Dialog
- Step 1: **Analysis** (`GET /students/{id}/program-transfer-analysis?newProgramId=...`) — shows impact before commit.
- Step 2: **Execute** (`POST /students/{id}/program-transfer`) — persists the transfer and creates a `StudentProgramTransfer` history row.
- **History** (`GET /students/{id}/program-transfers`) lists prior transfers.

## 4. Functional Workflows

**Create student → assign roll number:**
1. Admin creates the student via the form (or the student arrives via Admission conversion, out of this module's scope) — `POST /students`.
2. Later, admin opens Roll Number Assignment, selects course/year, clicks Preview to review the sequence, then Generate to commit.

**Program transfer:**
1. Admin opens a student's detail, launches Program Transfer, selects the new program.
2. System returns an analysis (fee/curriculum impact); admin reviews and confirms.
3. System executes the transfer, updates `Student.program`, and appends a `StudentProgramTransfer` record.

**Explorer search/export:**
1. User applies filters/search on the Explorer.
2. User clicks Export; system re-runs the identical filtered/sorted query server-side and streams an Excel or PDF file (`ExportResponseFactory`), with a metadata header listing the applied filters and sort.

## 5. API Endpoints

See SRS §4.2 for the full table (method, path, purpose, permission). Base path `/students`.

Request/response shapes:
- `StudentRequest` (record) — see FRD §3.2 fields; validated with Jakarta Bean Validation.
- `StudentResponse` — flattened read DTO (not inspected field-by-field here; mirrors `StudentRequest` plus `id`, program/course/speciality display names, timestamps).
- `GenerateRollNumbersRequest` / `RollNumberAssignment` — roll-number generation request/result shapes.
- `ProgramTransferRequest` / `ProgramTransferAnalysis` / `ProgramTransferRecord` — program-transfer request/analysis/history shapes.

## 6. Data Model

**`students` table** (originally V12; extended by ~20 later migrations — see below), entity `Student`:

| Column | Notes |
|---|---|
| `id` | PK, identity |
| `roll_number` | unique, nullable (since V53) |
| `admission_number` | unique, length 20, system-generated (BR-27) |
| `university_registration_number`, `umis_number` | unique, externally issued, added V112 |
| `first_name`, `last_name`, `email` (unique), `phone` | core identity/contact |
| `bio`, `emergency_contact_*` | added V137 (bio), later additions |
| `program_id` (FK, NOT NULL), `course_id` (FK), `speciality_id` (FK) | academic placement |
| `year_of_study` (`semester` in code) | NOT NULL |
| `admission_date` | NOT NULL |
| `lab_batch` | free text |
| `status` | enum `StudentStatus` |
| `date_of_birth`, `gender`, `admission_category`, `student_type` (V297), `aadhar_number` | personal |
| `nationality`, `religion`, `community_category`, `caste`, `blood_group`, `physical_disability` (V178) | demographics |
| `father_name/phone/email`, `mother_name/phone/email`, `parent_mobile` | family |
| `is_first_graduate` (bool), `father_education`, `mother_education` | scholarship-eligibility inputs (BR-21) |
| `cohort_id` (FK, added V215/V68 era) | cohort assignment |
| `expected_graduation_term_instance_id` (FK) | |
| Embedded `Address`: `postal_address`, `street`, `city`, `district`, `state`, `pincode` | |
| `bank_account_holder/number/ifsc_code/branch/name/type` (V227) | OneBook payout details |
| `created_at`, `updated_at` | JPA-audited |

**Enums:** `StudentStatus` (ACTIVE, INACTIVE, GRADUATED, ON_LEAVE, SUSPENDED, WITHDRAWN, EXPELLED); `StudentType` (DAY_SCHOLAR, HOSTELER).

**Related tables:** `roll_number_sequences` (course_id, academic_year, last_sequence — V111); `number_series_definitions`/`number_sequence_counters` (generic sequence engine, V244–V246, replaces the earlier single `application_number_sequences` table); `student_program_transfers` (V128).

## 7. Edge Cases & Validation Rules

- Roll-number bulk generation is protected by a pessimistic lock on the per-course-per-year counter row so two concurrent bulk-generate requests cannot hand out the same number twice.
- Admission number is never reserved on a failed admission attempt (only assigned on successful completion) — not directly enforced within this module since admission-number generation lives in the Admission workflow, but `Student.admissionNumber` is the field that persists it.
- Manual roll-number assignment bypasses generation entirely — only basic validation (format/uniqueness at DB level) applies, no sequence-consistency check against the auto-generated series.
- `GET /students` supports several mutually exclusive filter combinations (program+status dual filter, labBatch, academicYearId/feeStatus "explorer mode", activeOnly) resolved by if/else branching in the controller — combining filters outside these explicit combinations (e.g. labBatch + status together) is not supported by the plain list endpoint; use `/students/explorer` for combined filtering instead.

## 8. Known Gaps / Deferred

See SRS §6 (permission-gating inconsistencies on update/delete/several GET endpoints) and BRD §7. No `uniqueFieldValidator`-style live async uniqueness check is wired into the Student Form.
