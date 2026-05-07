# Scholarship Details Module — Implementation Plan

## Overview

A full-stack scholarship management module: structured eligibility tracking, application/approval workflow, and automatic fee integration — all permission-controlled through the existing DB-based `permissions` / `role_permissions` / `app_roles` tables (not Keycloak).

---

## Current State Analysis

### ✅ Existing Features
1. **Community Category** — Already captured in Student model (`communityCategory`: SC, ST, OBC, BC, MBC, EWS, etc.)
2. **Caste** — Text field in Student model
3. **Discount System** — Exists in `StudentFeeAllocation` with `discountAmount` and `discountReason`
4. **Payment Mode** — Already includes `SCHOLARSHIP` as a payment mode
5. **Document Types** — Includes `FIRST_GRADUATE_CERTIFICATE` as a document type
6. **Permission System** — DB-driven via `permissions`, `app_roles`, `role_permissions` tables; enforced via `@PreAuthorize("@perm.has('...')")` using `PermSecurityBean`

### ❌ Missing Features
1. No structured scholarship eligibility tracking (currently just free-form text in `discountReason`)
2. No "First Graduate" flag in Student model
3. No scholarship master table with pre-defined categories
4. No scholarship application workflow
5. No scholarship document linkage
6. No scholarship approval process
7. No scholarship amount calculation rules
8. No scholarship-specific permission codes in `permissions` table

---

## Scholarship Categories Supported

| Code | Name | Basis |
|------|------|-------|
| `FIRST_GRAD` | First Graduate Scholarship | Student is first in family to attend college |
| `SC_GOVT` | SC Government Scholarship | `communityCategory = SC` |
| `ST_GOVT` | ST Government Scholarship | `communityCategory = ST` |
| `OBC_GOVT` | OBC Government Scholarship | `communityCategory = OBC` |
| `BC_STATE` | BC/MBC State Scholarship | `communityCategory = BC or MBC` |
| `EWS` | Economically Weaker Section | Annual family income < ₹3,00,000 |
| `MERIT` | Merit Scholarship | 95%+ in qualifying exam |
| SNA | Scholarship Not Applicable | All others (default) |

---

## Phase 1 — Database Migrations (V100–V103)

### V100 — Create Scholarship Tables

Four new tables. Full PostgreSQL syntax is permitted (PostgreSQL is the only supported database — H2 is no longer used; see Decisions Log item 3):

**`scholarship_types`**
- `id`, `code` (unique), `name`, `description`
- `govt_scheme` (boolean) — Is this a government scholarship?
- `scheme_code` — Government portal reference
- `discount_type` — `PERCENTAGE`, `FIXED_AMOUNT`, `FULL_WAIVER`
- `discount_value` — 25.00 for 25% or 10000 for ₹10,000
- `max_amount_per_year`
- `renewal_required` (boolean) — Annual renewal needed? (stackable removed — one scholarship per student per year)
- `is_active` (boolean)
- `created_at`, `updated_at`

**`student_scholarship_eligibility`**
- `id`, `student_id` (FK → students)
- Eligibility flags: `is_first_graduate`, `is_merit_based`, `is_sports_quota`, `is_economically_weaker`
- Income details: `annual_family_income`, `income_certificate_number`, `income_cert_issuing_authority`, `income_cert_issue_date`
- Community details: `community_certificate_number`, `comm_cert_issuing_authority`, `comm_cert_issue_date`
- First Graduate details: `first_graduate_certificate_number`, `first_grad_cert_issuing_authority`, `first_grad_cert_issue_date`
- Parent education: `father_education`, `mother_education`
- Verification: `verified_by`, `verified_at`, `verification_remarks`
- `created_at`, `updated_at`

**`student_scholarships`** (application/allocation — one row per application)
- `id`, `student_id` (FK → students), `scholarship_type_id` (FK → scholarship_types)
- `academic_year_id` (FK → academic_years) — the year this application belongs to
- `application_date`, `application_remarks`
- `status` — `PENDING`, `APPROVED`, `REJECTED`, `ON_HOLD`, `CANCELLED`
- `approved_by`, `approved_at`, `rejection_reason`
- `approved_amount`, `disbursement_frequency` (`ANNUAL`, `SEMESTER`, `ONE_TIME`)
- `valid_from`, `valid_till`
- `renewed_from_id` (FK → student_scholarships, nullable) — set when this record is a renewal of a previous year's approved application
- `created_by`, `created_at`, `updated_at`
- Unique constraint: `(student_id, academic_year_id)` — one scholarship per student per academic year

**`scholarship_disbursements`**
- `id`, `student_scholarship_id` (FK → student_scholarships)
- `academic_year_id` (FK → academic_years), `semester_number`
- `amount`, `disbursement_date`, `disbursement_mode` (`DIRECT_CREDIT`, `FEE_WAIVER`, `CHEQUE`)
- `transaction_reference`, `cheque_number`, `bank_name`
- `remarks`, `disbursed_by`, `created_at`

---

### V101 — Seed Default Scholarship Types

```sql
INSERT INTO scholarship_types (code, name, govt_scheme, discount_type, discount_value, renewal_required, is_active) VALUES
('FIRST_GRAD', 'First Graduate Scholarship',  FALSE, 'FIXED_AMOUNT',  20000.00, FALSE, TRUE),
('SC_GOVT',    'SC Government Scholarship',   TRUE,  'PERCENTAGE',    100.00,   TRUE,  TRUE),
('ST_GOVT',    'ST Government Scholarship',   TRUE,  'PERCENTAGE',    100.00,   TRUE,  TRUE),
('OBC_GOVT',   'OBC Government Scholarship',  TRUE,  'FIXED_AMOUNT',  30000.00, TRUE,  TRUE),
('BC_STATE',   'BC/MBC State Scholarship',    TRUE,  'FIXED_AMOUNT',  25000.00, TRUE,  TRUE),
('EWS',        'Economically Weaker Section', FALSE, 'PERCENTAGE',    50.00,    TRUE,  TRUE),
('MERIT',      'Merit Scholarship',           FALSE, 'FIXED_AMOUNT',  30000.00, FALSE, TRUE);
```

---

### V102 — Add Scholarship Fields to Students Table

```sql
ALTER TABLE students
  ADD COLUMN is_first_graduate BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN father_education   VARCHAR(100),
  ADD COLUMN mother_education   VARCHAR(100);
```

---

### V103 — Add Scholarship Permission Codes

Using the same idempotent `WHERE NOT EXISTS` pattern as `V95__add_community_blood_group_permissions.sql`:

**Permission codes to insert into `permissions` table:**

| Code | Display Name | Category |
|------|-------------|----------|
| `SCHOLARSHIP_VIEW` | View Scholarships | `SCHOLARSHIP` |
| `SCHOLARSHIP_MANAGE` | Manage Scholarship Types | `SCHOLARSHIP` |
| `SCHOLARSHIP_APPLY` | Apply for Scholarship | `SCHOLARSHIP` |
| `SCHOLARSHIP_APPROVE` | Approve/Reject Scholarship Applications | `SCHOLARSHIP` |
| `SCHOLARSHIP_DISBURSE` | Record Scholarship Disbursements | `SCHOLARSHIP` |

**Role grants via `role_permissions` table:**

| Role | Permissions Granted |
|------|-------------------|
| `DEV_ADMIN`, `SUPPORT_ADMIN`, `ADMIN` | All 5 permissions |
| `COLLEGE_ADMIN` | `SCHOLARSHIP_VIEW`, `SCHOLARSHIP_MANAGE`, `SCHOLARSHIP_APPLY`, `SCHOLARSHIP_APPROVE`, `SCHOLARSHIP_DISBURSE` |
| `FRONT_OFFICE` | `SCHOLARSHIP_VIEW`, `SCHOLARSHIP_APPLY` |
| `CASHIER` | `SCHOLARSHIP_VIEW`, `SCHOLARSHIP_DISBURSE` |
| `FACULTY` | `SCHOLARSHIP_VIEW` |
| `STUDENT` | `SCHOLARSHIP_VIEW` (own records only — enforced in service layer) |

All grants use `WHERE NOT EXISTS` to be idempotent (safe to re-run).

---

## Phase 2 — Backend: Entities, Enums & Repositories

### New Enums (`com.cms.model.enums`)
- `ScholarshipStatus` — `PENDING`, `APPROVED`, `REJECTED`, `ON_HOLD`, `CANCELLED`
- `DiscountType` — `PERCENTAGE`, `FIXED_AMOUNT`, `FULL_WAIVER`
- `DisbursementFrequency` — `ANNUAL`, `SEMESTER`, `ONE_TIME`
- `DisbursementMode` — `DIRECT_CREDIT`, `FEE_WAIVER`, `CHEQUE`

### New Entities (`com.cms.model`)
- `ScholarshipType` — mirrors `scholarship_types` table
- `StudentScholarshipEligibility` — mirrors `student_scholarship_eligibility` table; `@OneToOne` with `Student`
- `StudentScholarship` — mirrors `student_scholarships` table; `@ManyToOne` to `Student` + `ScholarshipType`
- `ScholarshipDisbursement` — mirrors `scholarship_disbursements` table; `@ManyToOne` to `StudentScholarship`

### Updated Entity
- `Student` — add `isFirstGraduate` (boolean), `fatherEducation` (String), `motherEducation` (String)

### New Repositories (`com.cms.repository`)
- `ScholarshipTypeRepository` — `findByIsActiveTrue()`, `findByCode(String)`
- `StudentScholarshipEligibilityRepository` — `findByStudentId(Long)`
- `StudentScholarshipRepository` — `findByStudentId(Long)`, `findByStudentIdAndStatus(Long, ScholarshipStatus)`, `findByStudentIdAndAcademicYearId(Long, Long)`, `findByStatusAndScholarshipTypeId(...)`, `existsByStudentIdAndAcademicYearId(Long, Long)`
- `ScholarshipDisbursementRepository` — `findByStudentScholarshipId(Long)`, `findByStudentScholarshipStudentId(Long)`

---

## Phase 3 — Backend: DTOs

All DTOs as Java records in `com.cms.dto`:

- `ScholarshipTypeRequest` — create/update scholarship type master
- `ScholarshipTypeResponse` — full detail including discount info
- `ScholarshipEligibilityRequest` — update student eligibility details (income, certificates, flags)
- `ScholarshipEligibilityResponse` — eligibility details + auto-detected eligible scholarships
- `ScholarshipApplicationRequest` — apply for a scholarship (studentId, scholarshipTypeId, remarks)
- `ScholarshipApplicationResponse` — application detail with status
- `ScholarshipApprovalRequest` — approve (approvedAmount, validFrom, validTill, disbursementFrequency, remarks)
- `ScholarshipRejectionRequest` — reject (reason)
- `DisbursementRequest` — record a disbursement (amount, date, mode, reference, remarks)
- `DisbursementResponse` — disbursement history item

---

## Phase 4 — Backend: Services

### `ScholarshipTypeService` (`com.cms.service`)
- `getAllActive()` — list active scholarship types
- `getById(Long id)` — get by ID
- `create(ScholarshipTypeRequest request, String actor)` — create new type (admin)
- `update(Long id, ScholarshipTypeRequest request, String actor)` — update master
- `deactivate(Long id, String actor)` — soft-delete

### `StudentScholarshipService` (`com.cms.service`)
- `getEligibleScholarships(Long studentId)` — auto-detect based on: `communityCategory`, `isFirstGraduate`, `annualFamilyIncome`, query marks for merit; returns list of matching scholarship types
- `getStudentScholarships(Long studentId)` — all applications with status
- `applyForScholarship(Long studentId, ScholarshipApplicationRequest request, String actor)` — rejects with `400 Bad Request` if student already has an active/approved scholarship for the current academic year (enforcing one-per-year rule)
- `approveScholarship(Long scholarshipApplicationId, ScholarshipApprovalRequest request, String actor)`
- `rejectScholarship(Long scholarshipApplicationId, ScholarshipRejectionRequest request, String actor)`
- `cancelScholarship(Long scholarshipApplicationId, String actor)`
- `renewScholarship(Long scholarshipApplicationId, String actor)` — creates a new `StudentScholarship` record for the next academic year with `status = PENDING` and `renewedFromId` pointing to the source; source application's `renewal_required` must be `TRUE`; rejects if a scholarship already exists for the student in the target year

### `StudentScholarshipEligibilityService` (`com.cms.service`)
- `getEligibility(Long studentId)` — fetch or auto-create eligibility record
- `updateEligibility(Long studentId, ScholarshipEligibilityRequest request, String actor)`
- `verifyEligibility(Long studentId, String verifiedBy, String remarks)`

### `ScholarshipDisbursementService` (`com.cms.service`)
- `disburse(Long studentScholarshipId, DisbursementRequest request, String actor)`
- `getDisbursementHistory(Long studentId)` — all disbursements for a student

### `FinanceService` — updates
- `setupStudentFee()` / fee finalization: query `student_scholarships` for `status = APPROVED` for the student's current `academic_year_id`; auto-populate `discountAmount` and `discountReason` on `StudentFeeAllocation`
- One scholarship per year: no stacking logic needed — simply apply the single approved scholarship's `approved_amount` as the discount

---

## Phase 5 — Backend: Controllers

All controllers in `com.cms.controller`, secured via `@PreAuthorize("@perm.has('...')")`:

### `ScholarshipController` — `/api/v1/scholarships`
| Method | Path | Permission | Description |
|--------|------|-----------|-------------|
| GET | `/api/v1/scholarships` | `SCHOLARSHIP_VIEW` | List all active scholarship types |
| GET | `/api/v1/scholarships/{id}` | `SCHOLARSHIP_VIEW` | Get scholarship type detail |
| POST | `/api/v1/scholarships` | `SCHOLARSHIP_MANAGE` | Create new scholarship type |
| PUT | `/api/v1/scholarships/{id}` | `SCHOLARSHIP_MANAGE` | Update scholarship type |
| DELETE | `/api/v1/scholarships/{id}` | `SCHOLARSHIP_MANAGE` | Deactivate scholarship type |

### `StudentScholarshipController` — `/api/v1/students/{studentId}/scholarships`
| Method | Path | Permission | Description |
|--------|------|-----------|-------------|
| GET | `/{studentId}/scholarships/eligible` | `SCHOLARSHIP_VIEW` | Auto-detect eligible scholarships |
| GET | `/{studentId}/scholarships` | `SCHOLARSHIP_VIEW` | Get all student scholarship applications |
| POST | `/{studentId}/scholarships/apply` | `SCHOLARSHIP_APPLY` | Submit a scholarship application |
| GET | `/{studentId}/eligibility` | `SCHOLARSHIP_VIEW` | Get eligibility record |
| PUT | `/{studentId}/eligibility` | `SCHOLARSHIP_MANAGE` | Update eligibility |
| PUT | `/{studentId}/eligibility/verify` | `SCHOLARSHIP_APPROVE` | Mark eligibility as verified |

### `ScholarshipApplicationController` — `/api/v1/scholarship-applications`
| Method | Path | Permission | Description |
|--------|------|-----------|-------------|
| GET | `/` | `SCHOLARSHIP_APPROVE` | List all pending applications (for approval queue) |
| PUT | `/{id}/approve` | `SCHOLARSHIP_APPROVE` | Approve application |
| PUT | `/{id}/reject` | `SCHOLARSHIP_APPROVE` | Reject application |
| PUT | `/{id}/cancel` | `SCHOLARSHIP_MANAGE` | Cancel application |
| POST | `/{id}/renew` | `SCHOLARSHIP_APPLY` | Renew into next academic year (creates new PENDING application) |
| POST | `/{id}/disburse` | `SCHOLARSHIP_DISBURSE` | Record disbursement |
| GET | `/{id}/disbursements` | `SCHOLARSHIP_VIEW` | Get disbursement history for application |

---

## Phase 6 — Frontend

### Student Form Enhancement
Add a **"Scholarship Eligibility"** section to `student-form.component` after the Family Details section:

```
┌─────────────────────────────────────────┐
│ SCHOLARSHIP ELIGIBILITY                 │
├─────────────────────────────────────────┤
│ ☐ First Graduate in Family              │
│   └─ Father's Education: [Dropdown]     │
│   └─ Mother's Education: [Dropdown]     │
│                                         │
│ Annual Family Income (₹): [_______]     │
│ Income Certificate No.:   [_______]     │
│ Issuing Authority:        [_______]     │
│ Issue Date:               [_______]     │
└─────────────────────────────────────────┘
```

Wired to reactive form group; `fatherEducation`/`motherEducation` fields hidden unless `isFirstGraduate = true` (using `@if`).

### New Module: `features/scholarship/`

**Components:**
- `scholarship-type-list` — MLP list of scholarship masters (Admin); table with sortable columns; `| inr` for amounts
- `scholarship-type-form` — Create/edit scholarship type; reactive form
- `scholarship-applications-list` — Approval queue for COLLEGE_ADMIN; filterable by status; action buttons Approve / Reject
- `scholarship-application-dialog` — Apply for a scholarship (student/staff); select eligible type, add remarks, upload document references
- `scholarship-approval-dialog` — Set approved amount, valid dates, disbursement frequency
- `scholarship-disbursement-dialog` — Record a disbursement

**Routes:**
```
/scholarships                        → scholarship-type-list (SCHOLARSHIP_MANAGE)
/scholarships/new                    → scholarship-type-form (SCHOLARSHIP_MANAGE)
/scholarships/:id/edit               → scholarship-type-form (SCHOLARSHIP_MANAGE)
/scholarship-applications            → scholarship-applications-list (SCHOLARSHIP_APPROVE)
```

### Student Detail Page — New "Scholarships" Tab

Add a **"Scholarships"** tab alongside existing tabs in the student detail page:

```
[Personal] [Academic] [Documents] [Fees] [Scholarships] ← NEW
```

Tab content:
1. **Eligibility** — Show/edit income details, certificate numbers, First Graduate flag; "Verify Eligibility" button for COLLEGE_ADMIN
2. **Eligible Scholarships** — Auto-detected list with "Apply" button per entry; if student already has a scholarship for the current year, the "Apply" button is disabled with a tooltip "One scholarship already active for this year"
3. **Applications** — Table of applied scholarships grouped by academic year; columns: Academic Year, Scholarship Name, Status badge, Approved Amount (`| inr:false:false`, `(₹)` in header), Applied Date (`| appDate`); if an approved scholarship has `renewal_required = TRUE` and the next academic year has no existing scholarship, a **"Renew"** action button is shown alongside an amber ⚠ badge "Renewal due"
4. **Disbursements** — History table of all disbursements

### Fee Finalization Integration

Update `finalize-fee-dialog.component` to show a "Scholarships Detected" section:

```
┌──────────────────────────────────────────┐
│ Fee Finalization                         │
├──────────────────────────────────────────┤
│ Base Fee: ₹ 4,08,000                     │
│                                          │
│ 🎓 Approved Scholarships:               │
│ ☑ First Graduate Scholarship — ₹20,000  │
│ ☑ SC Government Scheme — ₹30,000        │
│                                          │
│ Total Scholarship Discount: ₹50,000      │
│ ─────────────────────────────────────── │
│ Additional Discount:   ₹ [_____]         │
│ Reason:                [_____________]   │
│ ─────────────────────────────────────── │
│ FINAL NET FEE:         ₹ 3,58,000        │
└──────────────────────────────────────────┘
```

Scholarship discount is read-only (from approved applications); additional discount is manual (existing flow).

---

## Phase 7 — Tests (Backend)

- `ScholarshipTypeServiceTest` — `@ExtendWith(MockitoExtension.class)` — CRUD + validation
- `StudentScholarshipServiceTest` — eligibility detection logic (community/income/merit), apply/approve/reject state machine
- `ScholarshipDisbursementServiceTest` — disbursement recording + history
- `ScholarshipControllerTest` — `@WebMvcTest` for all endpoints with permission checks
- `StudentScholarshipControllerTest` — `@WebMvcTest` for all endpoints
- `ScholarshipApplicationControllerTest` — `@WebMvcTest` approve/reject/disburse flows
- Coverage target: ≥ 95% (enforced by JaCoCo via `./gradlew check`)

---

## Phase 8 — Manual Test Cases

Create `docs/manual-test-cases/scholarship-management.md` with test cases covering:
- TC-SCHOL-001: Create a new scholarship type
- TC-SCHOL-002: Mark student as First Graduate → eligible for FIRST_GRAD
- TC-SCHOL-003: Student with SC community → eligible for SC_GOVT
- TC-SCHOL-004: Student with income < ₹3L → eligible for EWS
- TC-SCHOL-005: Apply for scholarship → status = PENDING
- TC-SCHOL-006: Approve scholarship → status = APPROVED, amount set
- TC-SCHOL-007: Reject scholarship → status = REJECTED, reason saved
- TC-SCHOL-008: Student attempts to apply for a second scholarship in same year → 400 rejected
- TC-SCHOL-009: Finalize fee with approved scholarship → discount auto-applied
- TC-SCHOL-010: Record disbursement → entry in disbursement history
- TC-SCHOL-011: View student scholarship history tab grouped by academic year
- TC-SCHOL-012: Renew an approved SC_GOVT scholarship into next academic year → new PENDING record created
- TC-SCHOL-013: Attempt to renew a FIRST_GRAD scholarship (renewal_required = FALSE) → 400 rejected
- TC-SCHOL-014: FRONT_OFFICE cannot access approval queue (403)
- TC-SCHOL-015: STUDENT can view own scholarships but not others (403)

---

## Decisions Log

1. **One scholarship per student per year** ✅ DECIDED — A student may hold only one active scholarship per academic year. The `stackable` boolean and multi-scholarship stacking logic are removed from scope. The unique constraint on `student_scholarships` becomes `(student_id, academic_year_id)`. The fee finalization logic picks the single approved scholarship for the current academic year; no stacking calculation is needed. The `stackable` column is dropped from `scholarship_types`.

2. **Annual renewal workflow** ✅ DECIDED — Required, not deferred. Every scholarship with `renewal_required = TRUE` must be re-applied each academic year. The backend will expose a renewal endpoint (`POST /api/v1/scholarship-applications/{id}/renew`) that clones the approved application into the next academic year with status `PENDING`. The frontend Student Scholarships tab will show a "Renew" action button for eligible applications. A banner/badge should indicate scholarships expiring at year-end.

3. **PostgreSQL only — no H2 dependency** ✅ DECIDED — Local development must also use PostgreSQL (via Docker Compose), not H2. The `local` Spring profile will be updated to point to PostgreSQL just like `prod` (different DB name/credentials if needed, but same engine). Flyway will be enabled for `local` as well. This means:
   - V100–V103 migrations may use full PostgreSQL syntax (`BIGSERIAL`, native date/time types, etc.)
   - `ddl-auto` for `local` will be set to `validate` (not `create-drop`)
   - H2 dependency will be removed from `build.gradle.kts`
   - Docker Compose must be running before starting the backend locally
   - `application-local.yml` will be updated to use PostgreSQL datasource (pointing to the same `cms-postgres` container, a separate `cmsdb_local` database, or the same `cmsdb` as prod — confirm which)

4. **Document linkage** — Scholarship applications reference documents already uploaded via the existing Document Management module. No new document upload UI is needed.

5. **Government portal integration** — SC/ST scholarship schemes have a National Scholarship Portal (NSP). This plan implements manual tracking only. NSP API integration is explicitly deferred.

---

## Implementation Order

| Week | Tasks |
|------|-------|
| 0 (prerequisite) | Switch `local` profile to PostgreSQL: update `application-local.yml` datasource, enable Flyway, remove H2 dependency from `build.gradle.kts`, verify `./gradlew bootRun` works against `cms-postgres` container |
| 1 | V100–V103 migrations, entities, enums, repositories |
| 2 | Services (CRUD + eligibility detection + one-per-year guard + renewal logic), FinanceService integration |
| 3 | Controllers (including `/renew` endpoint) + backend tests (JaCoCo ≥ 95%) |
| 4 | Frontend — student form enhancement + scholarship module |
| 5 | Frontend — student detail tab (renewal badge + Renew button) + fee finalization integration |
| 6 | End-to-end testing, manual test cases, documentation |

