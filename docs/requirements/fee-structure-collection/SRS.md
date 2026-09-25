# Software Requirements Specification — Fee Structure & Collection

**Product:** OneCMS / College Management System
**Company:** Raster / Raster Images Pvt. Ltd.
**Client:** SKSCON / SKS College Of Nursing
**Module:** Finance — Fee Structure, Fee Finalization, Payment Collection, Refunds
**Status:** Shipped / in production use. This document was reverse-engineered from the actual
shipped backend (`backend/src/main/java/com/cms/...`), Flyway migrations
(`backend/src/main/resources/db/migration/...`), and the Angular frontend
(`frontend/src/app/features/finance/`, `frontend/src/app/features/fee-reports/`) as of 2026-09-24.

---

## 1. Introduction

### 1.1 Purpose

This SRS documents the Fee Structure & Collection module of OneCMS: how program/course fees are
configured per admission dimension, how the front office and admin turn an enquiry's fee guideline
into a finalized, semester-wise billing schedule, how the accounting team collects payments (including
bank-disbursed amounts in excess of what's owed), and how refunds — manual and system-generated — are
processed.

### 1.2 Scope

In scope: `FeeState` master, `FeeStructureGroup` / `FeeStructure` / `FeeStructureYearAmount` (fee
configuration), enquiry-stage fee guideline lookup and enquiry payments, admin fee finalization
(`StudentFeeAllocation`, `SemesterFee`), student-side payment collection (`FeeInstallment`,
`PaymentReceipt` / unified receipts), pre-admission credit carry-forward (`EnquiryCreditApplication`),
fee refunds (`FeeRefund`, including the auto-generated excess-payment refund), fee reporting
(`FeeDemand`, `FeeReportService`), and guardian/student self-service fee views.

Out of scope (documented elsewhere / other modules): Scholarship module (BR-14 through BR-17),
Referral Type / Commission tracking (BR-4, BR-5 — see `docs/requirements/referral-commission/`),
Enquiry lifecycle/status workflow beyond its fee touchpoints (BR-8 through BR-12, BR-18, BR-22 — see
`docs/requirements/enquiry-admission/`), OneBook payment gateway integration internals (BR-34),
Number Sequence / receipt numbering internals (BR-41), Term/Academic Calendar lifecycle (BR-53, BR-58,
BR-59).

### 1.3 Definitions

- **Fee Structure Group** — the unit of fee configuration: one row per unique combination of
  program + academic year + course (optional) + admission quota + fee state + gender.
- **Fee State** — a fee-segment master category (not a real geopolitical state), seeded with "Tamil
  Nadu" (default) and "Other State" (fallback). A student's free-text address state is matched against
  it for fee lookup.
- **Fee Guideline** — the read-only fee total shown on the enquiry form, computed from the current
  academic year's fee structure group matching the enquiry's dimensions.
- **Fee Finalization** — the admin action that converts an admitted student's guideline fee into a
  `StudentFeeAllocation` with a semester-wise billing schedule.
- **Semester Fee (installment)** — one of two half-year billing rows generated per program year at
  finalization time (entity name `SemesterFee`, table `installment_fees`).
- **Enquiry Credit** — money paid against an enquiry before conversion to student, automatically
  applied against the converted student's earliest open semester fees.
- **AUTO_EXCESS refund** — a system-generated, non-rejectable `FeeRefund` created when a bank-rail
  (DD/Bank Transfer) advance payment exceeds total outstanding.

---

## 2. Overall Description

### 2.1 Product Perspective

The module sits inside OneCMS's Finance area. It is fed by the Enquiry/Admission module (which
supplies quota/state/gender/student-type and pre-admission payments) and feeds the Student module
(fee allocation is created at or shortly after conversion). It also integrates with:
- **OneBook** — an external payment gateway used to push refund payouts (`OneBookIntegrationService`,
  `POST /student-fees/refunds/{refundId}/approve-onebook`).
- **Razorpay** — used for guardian-initiated fee payments (`RazorpayPaymentService`,
  `GuardianFeeController`).
- **Scholarship module** — an approved `StudentScholarship` discount is automatically folded into fee
  finalization (`FeeFinalizationService.finalize`).
- **DB-driven RBAC** (BR-24) — every endpoint is gated by a permission string checked via
  `@perm.has('...')`, resolved against the DB-only Role Management module.

### 2.2 Actors

- **Front Office** — enters enquiries; sees the fee guideline; collects pre-admission (enquiry-stage)
  payments.
- **Admin (Fee Finalization)** — finalizes an admitted student's fee (discount only, never increase).
- **Accounting / Cashier** — collects student-stage fee payments (term-gated and advance/bank-excess),
  issues refunds, views reports.
- **Approver (Fee Refund Approve)** — approves/rejects refund requests.
- **Student (self-service)** — views own fee summary, receipts, penalties (`MY_FEE_VIEW`).
- **Guardian (self-service)** — views/pays a linked ward's fee (`MY_WARD_FEE_VIEW`, `MY_WARD_FEE_PAY`).
- **System** — auto-generates `AUTO_EXCESS` refunds and applies enquiry credit; no human actor.

### 2.3 Operating Environment

Angular frontend (`frontend/src/app/features/finance/`, `.../fee-reports/`), Spring Boot backend
(`backend/src/main/java/com/cms/{controller,service,model,repository,dto}`), PostgreSQL via Flyway
migrations, Keycloak for identity only (BR-24) — authorization is DB-driven via `PermSecurityBean`
(`@perm.has(...)`).

### 2.4 Constraints & Assumptions

- All monetary values are `BigDecimal`, normalized to 2 decimal places; amounts with more than 2
  decimals are rejected rather than rounded (BR-23).
- The backend is the source of truth for every fee total; client-submitted totals are never trusted
  for persistence (BR-23).
- A `FeeStructureGroup` currently has exactly 3 admission dimensions — **quota, fee state, gender** —
  scoped by program/academic year/course. **Student Type (Day Scholar/Hosteler) is NOT a group
  dimension** (see §6 Known Gaps — this deviates from BUSINESS_REQUIREMENTS.md's BR-30 text).
- Fee state ("Tamil Nadu"/"Other State") is a fixed, seed-only master — there is no admin CRUD screen
  for it (`FeeStateController` only exposes `GET`). New fee states would currently require a Flyway
  migration, not an admin UI action.
- General partial refunds and a payment/receipt "void" flow are explicitly out of scope system-wide
  (BR-36) — reversing a payment is always done through the refund request flow.

---

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-FEE-1 | Admin can define a Fee Structure Group (program + academic year + course? + quota + fee state + gender) with per-fee-type, per-program-year amounts. | Must | Program, AcademicYear, Course, FeeState masters |
| FR-FEE-2 | The system generates year-wise fee input boxes per fee type, one per `Program.durationYears`. | Must | FR-FEE-1 |
| FR-FEE-3 | Duplicate fee types within one group submission are rejected; the group's course total (sum of "generic"/course-fee-type amounts) must be > 0 to save. | Must | FR-FEE-1 |
| FR-FEE-4 | A second `FeeStructureGroup` for the same 6-field combination is rejected (unique DB constraint + service-level check). | Must | FR-FEE-1 |
| FR-FEE-5 | Enquiry form fee guideline: given program, course (if applicable), quota, fee state (derived from address), gender (+ student type, used only to filter HOSTEL_FEE), the system returns the matching group's fee items for the current academic year. | Must | FR-FEE-1 |
| FR-FEE-6 | If no exact match, the system retries with the fallback `FeeState` (`isFallback=true`) holding quota/gender fixed. If still no match, enquiry submission is blocked. | Must | FR-FEE-5 |
| FR-FEE-7 | Editing a fee structure group that already has enquiries finalized against it (`FEES_FINALIZED`/`PARTIALLY_PAID`/`ADMITTED`) requires a mandatory change reason, which is audit-logged with before/after amounts. | Should | FR-FEE-1 |
| FR-FEE-8 | Admin Fee Finalization: pre-populates the guideline total (read-only); admin may apply a discount (+ reason) but never increase the fee; an approved scholarship discount is automatically added on top. | Must | FR-FEE-5, Scholarship module |
| FR-FEE-9 | On finalization, each program year's net fee is split into 2 semester fees (floor-round Sem 1, remainder Sem 2), each with its own due date derived from the academic year's `TermBillingSchedule`. | Must | FR-FEE-8 |
| FR-FEE-10 | Term-gated Collect Payment: a payment can only be applied to semesters whose term is currently open for collection (`TermInstanceService.isSemesterFeeCollectibleNow`); payment is capped at currently-collectible outstanding. | Must | FR-FEE-9 |
| FR-FEE-11 | Advance Payment (per-student, not term-gated): can cover any semester up to total outstanding; optionally, with `FEE_COLLECT_EXCESS` + DD/Bank Transfer mode, can exceed total outstanding — the excess is auto-carved into an `AUTO_EXCESS` `FeeRefund`. | Must | FR-FEE-9, FR-FEE-14 |
| FR-FEE-12 | Enquiry-stage pre-admission credit (`enquiry_payments`) is automatically applied — earliest open semester first — against the converted student's semester fees on every subsequent collection call, ahead of the newly tendered amount. | Must | Enquiry conversion |
| FR-FEE-13 | Payment cascades within one collection call across multiple semesters in due order; one receipt number covers the whole call regardless of how many semesters it touches. | Must | FR-FEE-9 |
| FR-FEE-14 | Fee Refund workflow: unified initiation (auto-detects STUDENT vs ENQUIRY payer from the receipt), one active (non-REJECTED) refund per receipt, PENDING → APPROVED/REJECTED by a separate approver, refund-number generated only on approval. | Must | Receipts |
| FR-FEE-15 | `AUTO_EXCESS` refunds cannot be rejected/deleted by staff (only approved/paid) and do not soft-flag any installment/enquiry-payment row on approval (nothing was ever allocated to a fee). | Must | FR-FEE-11, FR-FEE-14 |
| FR-FEE-16 | Approved refunds can optionally be pushed through OneBook for payout; OneBook webhook completion mirrors the manual-approval soft-flag/refund-number logic. | Should | FR-FEE-14, OneBook integration |
| FR-FEE-17 | Transaction reference is mandatory for UPI/Bank Transfer/Cheque payments (any mode except CASH/CARD), enforced by `@TransactionReferenceRequired` on the relevant request DTOs. | Must | — |
| FR-FEE-18 | Fee Explorer: paginated, filterable (program/academic year/year-of-study/allocation status) list of every student's fee summary; exportable (Excel/PDF), including a semester-wise export variant under its own permission. | Should | FR-FEE-9 |
| FR-FEE-19 | Fee Reports: outstanding demands, collection summary, and per-student ledger, scoped by `termInstanceId`/`studentId`. | Should | FeeDemand generation |
| FR-FEE-20 | Student/Guardian self-service: read-only fee summary, receipts, and penalties for one's own record / a verified ward; guardian can additionally initiate a Razorpay payment order. | Should | FR-FEE-9 |
| FR-FEE-21 | Penalty calculation for overdue semester fees, surfaced on both staff and self-service screens. | Should | FR-FEE-9 |

---

## 4. External Interface Requirements

### 4.1 Screens (frontend/src/app/features/finance/, fee-reports/)

| Screen | Component path |
|---|---|
| Fee Structure List | `finance/fee-structure-list/fee-structure-list.component.ts` |
| Fee Structure Form (Combination Picker → Fee Grid) | `finance/fee-structure-form/fee-structure-form.component.ts` |
| Fee Finalization | `finance/fee-finalization/fee-finalization.component.ts` |
| Fee Collection (term-gated bulk list) | `finance/fee-collection/fee-collection.component.ts` |
| Student Fee Detail (semester status, Advance Payment incl. bank-excess) | `finance/student-fee-detail/student-fee-detail.component.ts` |
| Fee Explorer | `finance/fee-explorer/fee-explorer.component.ts` |
| Receipts List (unified) | `finance/receipts-list/receipts-list.component.ts` |
| Fee Refund List | `finance/fee-refund-list/fee-refund-list.component.ts` |
| Fee Reports Dashboard | `fee-reports/fee-reports-dashboard/fee-reports-dashboard.component.ts` |

### 4.2 Key API Endpoints (see FRD.md §5 for the full list with permissions)

`/fee-states`, `/fee-structures` (+ `/bulk`, `/guideline`, `/grouped`, `/group`, `/finalized-count`),
`/student-fees/*` (`finalize`, `.../collect`, `.../collect-advance`, `.../receipts`, `.../refunds/*`,
`.../credit-applications`, `explorer*`, `my/*`), `/enquiries/{id}/payments`,
`/enquiries/{id}/credit-applications`, `/receipts`, `/fee-reports/*`, `/fee-demands/*`,
`/guardian/wards/{studentId}/fee-*`.

### 4.3 Key DB Entities

`FeeState`, `FeeStructureGroup`, `FeeStructure`, `FeeStructureYearAmount`, `StudentFeeAllocation`,
`SemesterFee` (table `installment_fees`), `FeeInstallment` (table `fee_installments`), `FeeRefund`,
`FeeDemand`, `EnquiryCreditApplication`, `PaymentReceipt` (unified receipts), `EnquiryPayment`.

---

## 5. Non-Functional Requirements

- **Numeric integrity:** `BigDecimal` throughout with `RoundingMode.HALF_UP`/`FLOOR` used explicitly
  for the semester split; server-side normalization to 2 decimals; server never trusts client totals
  (BR-23).
- **Concurrency safety:** `StudentFeeAllocation` is fetched with `findByStudentIdForUpdate` (pessimistic
  lock) during both collection paths to prevent double-spend of outstanding balance/credit under
  concurrent requests.
- **Auditability:** fee-structure edits against already-finalized enquiries are logged via
  `AuditLogService` with before/after amounts and a mandatory reason; refunds carry
  requestedBy/approvedBy/rejectedBy + timestamps.
- **Authorization:** every mutating and most read endpoints are gated by a dedicated permission string
  via `@perm.has(...)`, resolved from the DB-only role/permission system (BR-24, BR-39).
- **Security-in-depth:** the `FEE_COLLECT_EXCESS` bank-excess gate and the `AUTO_EXCESS` reject-block
  are both enforced server-side even though the frontend also disables the corresponding UI affordance.
- **Exportability:** Fee Explorer, its semester-wise variant, Fee Refunds, and unified Receipts are all
  exportable to Excel/PDF with metadata (filters, sort) baked into the export header.

---

## 6. Known Gaps / Not Yet Implemented

1. **BUSINESS_REQUIREMENTS.md's BR-30 section is stale on Student Type as a fee dimension.**
   BR-30's text and its `FeeStructureGroup` data-model sketch describe a 4-dimension key including
   `studentType` (`UNIQUE: (program, academicYear, course, quota, feeState, gender, studentType)`).
   Migration `V170__remove_student_type_from_fee_structure_groups.sql` subsequently dropped the
   `student_type` column and narrowed the unique constraint to 6 fields (no student type) — confirmed
   against the current `FeeStructureGroup.java` entity, the `fee_structure_groups` table, and the
   frontend `fee-structure-form.component.ts`'s Combination Picker (no student-type control). Current
   behavior: **Day Scholar vs Hosteler is not a separate fee configuration** — it is implicit in
   whether the `HOSTEL_FEE` line item is included. `FeeStructureController.getFeeGuideline` and
   `FeeStructureService.findForEnquiry(..., StudentType, ...)` still accept `studentType` as a request
   parameter, but only use it to filter the `HOSTEL_FEE` item out of the response for `DAY_SCHOLAR`
   callers — it is not part of the group lookup key. BR-1's and BR-23's "amended by BR-30" notes are
   consistent with the current 3-dimension (quota × feeState × gender) picture and do not need
   correction; only BR-30 itself is stale on this point.
2. **BR-23's note that the HOSTEL_FEE/TRANSPORT_FEE filter "no longer applies" under BR-30 is only
   half accurate against current code.** The current `findForEnquiry(..., StudentType, ...)` overload
   still filters out `HOSTEL_FEE` for `DAY_SCHOLAR` callers, but does **not** filter `TRANSPORT_FEE` at
   all (it is returned regardless of student type) — differing from the original BR-23 rule
   ("TRANSPORT_FEE only for DAY_SCHOLAR"). Not otherwise documented in BUSINESS_REQUIREMENTS.md.
3. **No admin CRUD for `FeeState`.** Only seeded via `V165` (Tamil Nadu, Other State);
   `FeeStateController` is read-only (`GET /fee-states`). Adding a new fee-segment category currently
   requires a Flyway migration, not a Master Lifecycle Status Management-style admin screen — unlike
   most other masters in this codebase.
4. **General partial refunds and a dedicated payment/void-cancellation flow remain explicitly out of
   scope** per BR-36 — confirmed still true in current code (no such endpoints/services exist).
