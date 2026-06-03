# Plan: Scholarship Details Module

## Overview

Full-stack scholarship management: structured eligibility tracking, application/approval workflow, and automatic fee integration. All permission-controlled through the existing DB-based `permissions` / `role_permissions` / `app_roles` tables.

---

## Scholarship Categories

| Code | Name | Basis |
|------|------|-------|
| `FIRST_GRAD` | First Graduate Scholarship | Student is first in family to attend college |
| `SC_GOVT` | SC Government Scholarship | `communityCategory = SC` |
| `ST_GOVT` | ST Government Scholarship | `communityCategory = ST` |
| `OBC_GOVT` | OBC Government Scholarship | `communityCategory = OBC` |
| `BC_STATE` | BC/MBC State Scholarship | `communityCategory = BC or MBC` |
| `EWS` | Economically Weaker Section | Annual family income < ₹3,00,000 |
| `MERIT` | Merit Scholarship | 95%+ in qualifying exam |

---

## Phase 1 — Database Migrations (V100–V103)

### V100 — Create Scholarship Tables

**`scholarship_types`** — code, name, govtScheme, schemeCode, discountType (`PERCENTAGE`/`FIXED_AMOUNT`/`FULL_WAIVER`), discountValue, maxAmountPerYear, renewalRequired, isActive

**`student_scholarship_eligibility`** — per-student: isFirstGraduate, isMeritBased, isSportsQuota, isEconomicallyWeaker, annualFamilyIncome, income certificate fields, community certificate fields, first graduate certificate fields, parentEducation, verifiedBy/verifiedAt

**`student_scholarships`** — student_id, scholarship_type_id, academic_year_id, status (`PENDING`/`APPROVED`/`REJECTED`/`ON_HOLD`/`CANCELLED`), approvedAmount, disbursementFrequency, validFrom, validTill, renewedFromId (FK to self)
- Unique constraint: `(student_id, academic_year_id)` — one scholarship per student per year

**`scholarship_disbursements`** — studentScholarshipId, academicYearId, semesterNumber, amount, disbursementDate, disbursementMode (`DIRECT_CREDIT`/`FEE_WAIVER`/`CHEQUE`), transactionReference, remarks, disbursedBy

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

### V102 — Add Scholarship Fields to Students Table
```sql
ALTER TABLE students
  ADD COLUMN is_first_graduate BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN father_education   VARCHAR(100),
  ADD COLUMN mother_education   VARCHAR(100);
```

### V103 — Add Scholarship Permission Codes

| Code | Category |
|------|----------|
| `SCHOLARSHIP_VIEW` | SCHOLARSHIP |
| `SCHOLARSHIP_MANAGE` | SCHOLARSHIP |
| `SCHOLARSHIP_APPLY` | SCHOLARSHIP |
| `SCHOLARSHIP_APPROVE` | SCHOLARSHIP |
| `SCHOLARSHIP_DISBURSE` | SCHOLARSHIP |

Role grants (using `WHERE NOT EXISTS` — idempotent):
- `DEV_ADMIN`, `SUPPORT_ADMIN`, `ADMIN`, `COLLEGE_ADMIN` — all 5
- `FRONT_OFFICE` — VIEW + APPLY
- `CASHIER` — VIEW + DISBURSE
- `FACULTY` — VIEW only
- `STUDENT` — VIEW (own records — enforced in service layer)

---

## Phase 2 — Backend Entities & Repositories

### New Enums
- `ScholarshipStatus` — PENDING, APPROVED, REJECTED, ON_HOLD, CANCELLED
- `DiscountType` — PERCENTAGE, FIXED_AMOUNT, FULL_WAIVER
- `DisbursementFrequency` — ANNUAL, SEMESTER, ONE_TIME
- `DisbursementMode` — DIRECT_CREDIT, FEE_WAIVER, CHEQUE

### New Entities
- `ScholarshipType`, `StudentScholarshipEligibility`, `StudentScholarship`, `ScholarshipDisbursement`

### Updated Entity
- `Student` — add `isFirstGraduate`, `fatherEducation`, `motherEducation`

---

## Phase 3 — Backend DTOs

- `ScholarshipTypeRequest` / `ScholarshipTypeResponse`
- `ScholarshipEligibilityRequest` / `ScholarshipEligibilityResponse` (includes auto-detected eligible types)
- `ScholarshipApplicationRequest` / `ScholarshipApplicationResponse`
- `ScholarshipApprovalRequest` / `ScholarshipRejectionRequest`
- `DisbursementRequest` / `DisbursementResponse`

---

## Phase 4 — Backend Services

### `ScholarshipTypeService`
CRUD for scholarship type master.

### `StudentScholarshipService`
- `getEligibleScholarships(studentId)` — auto-detect based on communityCategory, isFirstGraduate, annualFamilyIncome
- `applyForScholarship()` — rejects with 400 if student already has active scholarship for the current academic year
- `approveScholarship()` / `rejectScholarship()` / `cancelScholarship()`
- `renewScholarship()` — creates a new PENDING record for next academic year; fails if scholarship already exists for that year; source must have `renewalRequired = TRUE`

### `StudentScholarshipEligibilityService`
- `getEligibility(studentId)` — fetch or auto-create
- `updateEligibility()` / `verifyEligibility()`

### `ScholarshipDisbursementService`
- `disburse()` / `getDisbursementHistory(studentId)`

### `FinanceService` — updates
Auto-apply approved scholarship's `approvedAmount` as `discountAmount` on `StudentFeeAllocation` during fee finalization.

---

## Phase 5 — Backend Controllers

### `ScholarshipController` — `/api/v1/scholarships`
| Method | Path | Permission |
|--------|------|-----------|
| GET | `/` | SCHOLARSHIP_VIEW |
| GET | `/{id}` | SCHOLARSHIP_VIEW |
| POST | `/` | SCHOLARSHIP_MANAGE |
| PUT | `/{id}` | SCHOLARSHIP_MANAGE |
| DELETE | `/{id}` | SCHOLARSHIP_MANAGE |

### `StudentScholarshipController` — `/api/v1/students/{studentId}/scholarships`
| Method | Path | Permission |
|--------|------|-----------|
| GET | `/eligible` | SCHOLARSHIP_VIEW |
| GET | `/` | SCHOLARSHIP_VIEW |
| POST | `/apply` | SCHOLARSHIP_APPLY |
| GET | `../eligibility` | SCHOLARSHIP_VIEW |
| PUT | `../eligibility` | SCHOLARSHIP_MANAGE |
| PUT | `../eligibility/verify` | SCHOLARSHIP_APPROVE |

### `ScholarshipApplicationController` — `/api/v1/scholarship-applications`
| Method | Path | Permission |
|--------|------|-----------|
| GET | `/` | SCHOLARSHIP_APPROVE (approval queue) |
| PUT | `/{id}/approve` | SCHOLARSHIP_APPROVE |
| PUT | `/{id}/reject` | SCHOLARSHIP_APPROVE |
| PUT | `/{id}/cancel` | SCHOLARSHIP_MANAGE |
| POST | `/{id}/renew` | SCHOLARSHIP_APPLY |
| POST | `/{id}/disburse` | SCHOLARSHIP_DISBURSE |
| GET | `/{id}/disbursements` | SCHOLARSHIP_VIEW |

---

## Phase 6 — Frontend

### Student Form — New "Scholarship Eligibility" Section
After Family Details section:
- First Graduate checkbox → reveals Father's/Mother's Education dropdowns
- Annual Family Income field
- Income Certificate No., Issuing Authority, Issue Date

### New Module: `features/scholarship/`
Components:
- `scholarship-type-list` — master list (Admin)
- `scholarship-type-form` — create/edit scholarship type
- `scholarship-applications-list` — approval queue (College Admin)
- `scholarship-application-dialog` — apply for scholarship
- `scholarship-approval-dialog` — set approved amount, dates, frequency
- `scholarship-disbursement-dialog` — record a disbursement

Routes:
```
/scholarships                   → scholarship-type-list (SCHOLARSHIP_MANAGE)
/scholarships/new               → scholarship-type-form
/scholarships/:id/edit          → scholarship-type-form
/scholarship-applications       → scholarship-applications-list (SCHOLARSHIP_APPROVE)
```

### Student Detail — New "Scholarships" Tab
Tabs: `[Personal] [Academic] [Documents] [Fees] [Scholarships]`

Tab sections:
1. **Eligibility** — income/certificate details; "Verify Eligibility" for COLLEGE_ADMIN
2. **Eligible Scholarships** — auto-detected list with "Apply" button (disabled if year's scholarship already active)
3. **Applications** — grouped by academic year; "Renew" button for `renewalRequired = TRUE` entries approaching year-end
4. **Disbursements** — history table

### Fee Finalization Integration
Show "Scholarships Detected" section in `finalize-fee-dialog` — scholarship discount read-only, additional discount manual.

---

## Phase 7 — Backend Tests
- `ScholarshipTypeServiceTest`
- `StudentScholarshipServiceTest` (eligibility detection, state machine)
- `ScholarshipDisbursementServiceTest`
- Controller tests for all 3 controllers
- JaCoCo ≥ 95% via `./gradlew check`

---

## Phase 8 — Manual Test Cases
`docs/manual-test-cases/scholarship-management.md` — TC-SCHOL-001 through TC-SCHOL-015 covering full workflow.

---

## Decisions Log

1. **One scholarship per student per year** — unique constraint on `(student_id, academic_year_id)`. No stacking.
2. **Annual renewal workflow** — required, not deferred. Backend `/renew` endpoint + frontend "Renew" button.
3. **PostgreSQL only** — local profile switched to PostgreSQL; H2 dependency removed from `build.gradle.kts`; `ddl-auto` set to `validate` for local.
4. **Document linkage** — references existing documents from Document Management module; no new upload UI needed.
5. **Government portal integration** — NSP API integration explicitly deferred; manual tracking only.

---

## Implementation Order

| Week | Tasks |
|------|-------|
| 0 | Switch local profile to PostgreSQL; remove H2 dependency |
| 1 | V100–V103 migrations, entities, enums, repositories |
| 2 | Services (CRUD + eligibility detection + one-per-year guard + renewal) + FinanceService integration |
| 3 | Controllers + backend tests (JaCoCo ≥ 95%) |
| 4 | Frontend — student form enhancement + scholarship module |
| 5 | Frontend — student detail Scholarships tab + fee finalization integration |
| 6 | End-to-end testing, manual test cases, documentation |
