# Functional Requirements Document — Fee Structure & Collection

**Product:** OneCMS / College Management System
**Company:** Raster / Raster Images Pvt. Ltd.
**Client:** SKSCON / SKS College Of Nursing
**Module:** Finance — Fee Structure, Fee Finalization, Payment Collection, Refunds

---

## 1. Overview

This FRD covers the shipped implementation of fee configuration (`FeeStructureGroup`/`FeeStructure`),
the enquiry-stage fee guideline, admin fee finalization into a semester billing schedule
(`StudentFeeAllocation`/`SemesterFee`), student-stage payment collection (`FeeInstallment`) including
the bank-excess auto-refund mechanism, pre-admission credit carry-forward, and the fee refund
lifecycle. Source: `backend/src/main/java/com/cms/{controller,service,model,repository,dto}`,
`backend/src/main/resources/db/migration/`, `frontend/src/app/features/finance/`,
`frontend/src/app/features/fee-reports/`.

## 2. Actors & Permissions

All permissions below are literal strings grepped from `@PreAuthorize("@perm.has('...')")`
annotations in the relevant controllers/services. Role→permission assignment is DB-only (Role
Management module) — not documented here per project convention.

| Permission | Screen / Endpoint | Action |
|---|---|---|
| `FEE_STRUCTURE_MANAGE` | Fee Structure List/Form, Term Billing Schedule | Create/update/delete fee structure groups & items; manage billing schedule |
| `FEE_FINALIZE` | Fee Finalization (enquiry-stage) | Gates both ends correctly: frontend route/nav/in-page action (`app.routes.ts`, `nav-config.ts`, `enquiry-list.component.ts`) and the backend endpoint it actually calls, `POST /enquiries/{id}/finalize-fees` (`EnquiryController.finalizeFees`, see the enquiry-admission-workflow module). Not the same endpoint as `POST /student-fees/finalize` below — see §8 note. |
| `STUDENT_FEE_MANAGE` | Student Fee allocation creation (post-admission), Fee Demand generation | `POST /student-fees/finalize` (creates the term-wise installment allocation from an already-finalized fee, reached from `StudentFeeDetailComponent` at `/student-fees/:studentId`, itself gated by `STUDENT_FEE_VIEW`/`STUDENT_FEE_MANAGE`), `POST /fee-demands/generate` |
| `STUDENT_FEE_VIEW` | Student Fee Detail, Fee Explorer, Receipts, Credit Applications | All student-fee read endpoints |
| `STUDENT_FEE_EXPORT` | Fee Explorer export | `GET /student-fees/explorer/export` |
| `STUDENT_FEE_EXPORT_SEMESTER_WISE` | Fee Explorer semester-wise export | `GET /student-fees/explorer/export/semester-wise` (dedicated permission, not a reuse of `STUDENT_FEE_EXPORT`, per the operation-wise permission mapping gate) |
| `FEE_COLLECT` | Fee Collection, Student Fee Detail, Enquiry payments | `POST /student-fees/{id}/collect`, `.../collect-advance`, `POST /enquiries/{id}/payments`, `GET /enquiries/{id}/payments` |
| `FEE_COLLECT_EXCESS` | Student Fee Detail — Advance Payment "bank excess" checkbox | Bypass total-outstanding cap on `collectAdvancePayment` (DD/Bank Transfer only); checked server-side in `PaymentCollectionService` even though the checkbox is also frontend-gated |
| `ENQUIRY_FEE_COLLECT_ADVANCE` | Enquiry payment advance/excess | Analogous enquiry-stage advance/excess gate in `EnquiryPaymentService` (separate permission from `FEE_COLLECT_EXCESS`) |
| `FEE_REFUND` | Fee Refund initiation | `POST /student-fees/refunds` |
| `FEE_REFUND_APPROVE` | Fee Refund List (approve/reject/list) | `GET /student-fees/refunds`, `.../pending`, `.../{id}/approve`, `.../{id}/approve-onebook`, `.../{id}/reject` |
| `FEE_REFUND_EXPORT` | Fee Refund List export | `GET /student-fees/refunds/export` |
| `FEE_REPORT_VIEW` | Fee Reports Dashboard | `GET /fee-reports/outstanding`, `.../collection-summary`, `.../student-ledger` |
| `RECEIPT_VIEW` / `RECEIPT_EXPORT` | Receipts List (unified) | `GET /receipts`, `GET /receipts/{receiptNumber}`, `GET /receipts/export` |
| `MY_FEE_VIEW` | Student self-service | `GET /student-fees/my/summary`, `.../my/receipts`, `.../my/penalties` |
| `MY_WARD_FEE_VIEW` | Guardian self-service | `GET /guardian/wards/{studentId}/fee-summary`, `.../fee-receipts`, `.../fee-penalties` |
| `MY_WARD_FEE_PAY` | Guardian self-service payment | `POST /guardian/wards/{studentId}/fee-payments/orders` (Razorpay order) |
| `ENQUIRY_VIEW` | Enquiry credit-application read | `GET /enquiries/{id}/credit-applications` |

## 3. Screens & UI Behavior

### 3.1 Fee Structure List (`finance/fee-structure-list/`)
Lists `GroupedFeeStructureResponse` rows via `GET /fee-structures/grouped`, filterable by program,
academic year, course, quota, fee state, gender. Each card/row shows the group's dimension badges and
computed total (year-amounts take precedence over the flat `amount` field; inactive fee types are
excluded from the displayed total — `FeeStructureService.findGrouped`).

### 3.2 Fee Structure Form — Combination Picker → Fee Grid (`finance/fee-structure-form/`)
Reactive form (`bulkForm`) with controls: `academicYearId`, `programId` (disabled until year chosen),
`courseId`, `quota` (default `MANAGEMENT`), `feeStateId` (defaults to the `isDefault=true` state),
`gender` (default `FEMALE`). Once all 6 are set, a `feeItems` FormArray renders — one group per
`FeeType`, each with per-year `yearAmounts` (`Validators.min(0)`). A "Replicate" helper
(`_computeReplicationTargets`) can fan the same grid out across every quota × fee-state × gender combo
in one action (skips `COUNSELLING` × fallback-state combos, since counselling seats are not applicable
to the "Other State" fallback). Saving posts `BulkFeeStructureRequest` to `POST/PUT /fee-structures/bulk`.
Editing a group with finalized enquiries against it prompts for a mandatory reason before the backend
will accept the change (`FeeStructureService.bulkUpdate`).

### 3.3 Fee Finalization (`finance/fee-finalization/`)
Lists enquiries in `INTERESTED`+ status; on selecting one, shows the guideline total (read-only),
Quota/State/Gender/Student-Type as read-only context, and a discount amount + reason input. Submits
`StudentFeeAllocationRequest` to `POST /student-fees/finalize`. The backend independently re-derives
discount bounds (`netFee >= 0`) and folds in any approved scholarship automatically — the discount
field on screen is additive to, not a replacement for, the scholarship discount.

### 3.4 Fee Collection (`finance/fee-collection/`)
Bulk, term-gated list of students with currently-collectible outstanding balances
(`PaymentCollectionService.getCollectibleOutstanding`); collection posts to
`POST /student-fees/{id}/collect`.

### 3.5 Student Fee Detail (`finance/student-fee-detail/`)
Per-student screen. The semester-wise fee status table is the primary content (`PAID`/`PARTIAL`/
`PENDING` per `SemesterFee`, with an `OVERDUE` visual flag when a due date has passed with outstanding
balance and `collectibleNow` flag per installment). The "Advance Payment" sub-form has an
`allowExcess` checkbox, visible/enabled only when: payment mode is `DEMAND_DRAFT`/`BANK_TRANSFER`
(`isExcessEligibleMode()`), and the caller holds `FEE_COLLECT_EXCESS`
(`canAllowExcess = permissionService.has('FEE_COLLECT_EXCESS')`). Checking it relaxes the
`maxOutstandingValidator` on the amount field. A live excess preview and confirmation-modal breakdown
show the amount that will be carved into a refund. Screen also shows receipts (grouped by receipt
number, including negative "Refund" rows for approved refunds) and the credit-applications audit
trail.

### 3.6 Fee Refund List (`finance/fee-refund-list/`)
Paginated, filterable (search/status/entityType/date range) list of `FeeRefundSummaryResponse`. Shows
an "Auto" source chip for `AUTO_EXCESS` refunds; the Reject action is hidden in the UI for them
(defense-in-depth alongside the server-side block in `FeeRefundService.rejectRefund`). Export to
Excel/PDF under `FEE_REFUND_EXPORT`.

### 3.7 Fee Explorer (`finance/fee-explorer/`)
Paginated `StudentFeeSummary` list, filters: search, program, academic year, year-of-study, allocation
status. Distinct filter-dropdown values are computed across all students, not just the loaded page
(`GET /student-fees/explorer/filter-options`). Exports (Excel/PDF), including a dedicated
semester-wise export variant.

### 3.8 Receipts List (`finance/receipts-list/`)
Unified list across student-stage and enquiry-stage payments (`GET /receipts`), filterable by
paymentMode/payerType/date range, exportable.

### 3.9 Fee Reports Dashboard (`fee-reports/fee-reports-dashboard/`)
Outstanding demands, collection summary, and per-student ledger, all scoped to a `termInstanceId` (or
`studentId` for the ledger).

## 4. Functional Workflows

### 4.1 Fee guideline lookup (enquiry stage)
```
GET /fee-structures/guideline?programId=&courseId=&quota=&feeStateId=&gender=&studentType=&academicYearId=
```
1. Resolve academic year: use `academicYearId` if given, else the current (`isCurrent=true`) year.
2. `groupRepository.findExact(programId, yearId, courseId, quota, feeStateId, gender)`.
3. If empty, look up `FeeState.isFallback=true`; if it differs from the requested `feeStateId`, retry
   the exact match with the fallback state's id (quota/gender/course unchanged).
4. If still empty → `404 Not Found`.
5. Else, fetch active `FeeStructure` items for the matched group, map to `FeeStructureResponse`.
6. If `studentType == DAY_SCHOLAR`, drop any `HOSTEL_FEE` item from the result (all other types,
   including `TRANSPORT_FEE`, pass through unfiltered regardless of student type).
7. `FeeGuidelineResponse(total = sum(items.amount), items)` is returned; `total = 0`/404 both surface
   as a hard submission block on the enquiry form.

### 4.2 Fee finalization → semester split
See BRD §4.3 for the narrative; formula:
```
sem1Amount = floor(yearAmount / 2)          // RoundingMode.FLOOR, scale 0
sem2Amount = yearAmount - sem1Amount        // absorbs the odd paisa
globalSemesterIndex = (yearNumber - 1) * 2 + {1 or 2}
label = "Year {yearNumber} - {Ordinal(globalSemesterIndex)} Installment"
```
Due dates: looked up per target academic year's `TermBillingSchedule` (ODD→Sem1, EVEN→Sem2); if that
year's `AcademicYear` row doesn't exist yet, the admission year's ODD/EVEN due date is shifted by the
same number of years (`shiftDueYear`).

### 4.3 Payment collection cascade (both `collect` and `collect-advance`)
1. Lock the allocation row (`findByStudentIdForUpdate`).
2. Compute `remainingEnquiryCredit = totalEnquiryPayments - alreadyAppliedCredit` (0 if no linked
   enquiry).
3. Compute the outstanding cap: **term-gated** sum of currently-collectible semesters
   (`collect`) or **total** outstanding across all semesters (`collect-advance`).
4. Validate `request.amount` against the cap — unless `collect-advance` with a valid
   `allowExcess` (excess path, see §4.4).
5. Iterate semesters in order; for each (term-gated path skips/breaks on non-open semesters per the
   rule in FRD §7 Edge Cases):
   a. `capacity = amount - alreadyPaid - alreadyCredited`.
   b. Apply as much `remainingEnquiryCredit` as fits (`creditForThis`); record an
      `EnquiryCreditApplication` row if > 0.
   c. `pendingForSemester = capacity - creditForThis`; apply as much of the caller's tendered
      `remaining` as fits into a new `FeeInstallment` row.
6. One receipt number covers every semester touched in this call
   (`unifiedReceiptService.saveStudentReceipt`, one row per receipt in the unified ledger).

### 4.4 Bank-excess auto-refund (Advance Payment only)
Preconditions checked server-side, all must hold: `request.amount() > totalOutstanding`,
`request.isAllowExcess() == true`, `paymentMode` ∈ {`DEMAND_DRAFT`, `BANK_TRANSFER`},
`permSecurityBean.has("FEE_COLLECT_EXCESS")`. If any fail → `IllegalStateException` /
`AccessDeniedException`. If all pass:
- The receipt's `amountPaid` is set to the **full** `request.amount()` (not just the portion applied).
- After the semester cascade consumes what it can, `remaining > 0` is the excess; it is carved into an
  `AUTO_EXCESS` `FeeRefund` via `FeeRefundService.createAutoExcessRefund(student, receiptNumber,
  remaining)`, `status=PENDING`, `requestedBy="SYSTEM"`, `reason="Auto-generated: payment exceeded
  total outstanding by ₹<amount>"`.
- On later approval: `AUTO_EXCESS` refunds skip the installment/enquiry-payment soft-flag step
  entirely (nothing was ever allocated to a fee); they generate a refund number and set `APPROVED`
  identically to a manual refund otherwise.
- `rejectRefund()` throws `IllegalStateException("Auto-generated excess refunds cannot be rejected")`
  for any refund with `source=AUTO_EXCESS`, unconditionally — no override exists.

### 4.5 Refund initiation & approval
```
POST /student-fees/refunds  { receiptNumber, reason }
```
1. Look up the receipt in the unified ledger (`PaymentReceipt`); 404 if not found.
2. Reject if an active (non-`REJECTED`) refund already exists for that receipt number.
3. Branch on `receipt.payerType`:
   - `STUDENT`: sum all `FeeInstallment` rows for that receipt number → `refundAmount`.
   - `ENQUIRY`: use the single `EnquiryPayment.amountPaid` for that receipt; reject if
     `payment.refundedAt != null` (already refunded).
4. Save `FeeRefund(status=PENDING, source=MANUAL, requestedBy=<caller>)`.

Approval (`POST .../{id}/approve`): generates `refundNumber` (year-scoped sequence), soft-flags the
underlying rows (unless `AUTO_EXCESS`), sets `APPROVED` + payout mode/date/reference/approvedBy.
Rejection (`POST .../{id}/reject`): only from `PENDING`; blocked entirely for `AUTO_EXCESS`.
OneBook payout (`POST .../{id}/approve-onebook`): pushes an `OneBookPaymentRequest`; the webhook
completion path (`FeeRefundService.completeOneBookRefund`) applies the identical
soft-flag/refund-number logic on `PAID`, sets `PAYMENT_FAILED` on `FAILED`, no-ops on `PROCESSING`.

## 5. API Endpoints

| Method | Path | Permission | Purpose |
|---|---|---|---|
| POST | `/fee-structures/bulk` | `FEE_STRUCTURE_MANAGE` | Create a fee structure group + items |
| PUT | `/fee-structures/bulk` | `FEE_STRUCTURE_MANAGE` | Update a group's items (upsert/delete-removed); requires `reason` if finalized enquiries exist |
| GET | `/fee-structures/finalized-count` | — | Count of finalized enquiries against a (program, quota, feeState, gender) combo |
| GET | `/fee-structures/guideline` | — | Enquiry-stage fee lookup (see §4.1) |
| GET | `/fee-structures/grouped` | — | Grouped list for the admin Fee Structure List screen |
| DELETE | `/fee-structures/group` | `FEE_STRUCTURE_MANAGE` | Delete a group + all its items/year-amounts |
| POST | `/fee-structures` | `FEE_STRUCTURE_MANAGE` | Create a single fee-structure item (legacy/completeness path; bulk is the main UI path) |
| GET | `/fee-structures` | — | List, filterable by programId/academicYearId/courseId |
| GET | `/fee-structures/{id}` | — | Get single item |
| PUT | `/fee-structures/{id}` | `FEE_STRUCTURE_MANAGE` | Update single item |
| DELETE | `/fee-structures/{id}` | `FEE_STRUCTURE_MANAGE` | Delete single item |
| GET | `/fee-states` | — | List active `FeeState`s, sorted (read-only master; no CRUD) |
| POST | `/student-fees/finalize` | `STUDENT_FEE_MANAGE` | Create a `StudentFeeAllocation` + semester schedule |
| GET | `/student-fees/{id}/allocation-exists` | `STUDENT_FEE_VIEW` | Boolean check |
| GET | `/student-fees/{id}/enquiry-year-fees` | `STUDENT_FEE_VIEW` | Pre-fill data from the linked enquiry's stored year-wise fees |
| GET | `/student-fees/{id}/semester-breakdown` | `STUDENT_FEE_VIEW` | Full allocation + semester detail |
| GET | `/student-fees/{id}/semester-status` | `STUDENT_FEE_VIEW` | Same as above (BR-13's "shown first" screen data) |
| GET | `/student-fees/my/summary` | `MY_FEE_VIEW` | Self-service; 204 if no linked student/allocation |
| GET | `/student-fees/my/receipts` | `MY_FEE_VIEW` | Self-service receipts |
| GET | `/student-fees/my/penalties` | `MY_FEE_VIEW` | Self-service penalties; 204 if none |
| POST | `/student-fees/{id}/collect` | `FEE_COLLECT` | Term-gated bulk collection |
| POST | `/student-fees/{id}/collect-advance` | `FEE_COLLECT` (+ `FEE_COLLECT_EXCESS` if excess) | Advance/bank-excess collection |
| GET | `/student-fees/{id}/penalties` | `STUDENT_FEE_VIEW` | Penalty calculation |
| GET | `/student-fees/explorer` | — | Paginated fee explorer (legacy unpaged mode via `?legacy=true`) |
| GET | `/student-fees/explorer/filter-options` | — | Distinct filter dropdown values |
| GET | `/student-fees/explorer/export` | `STUDENT_FEE_EXPORT` | Excel/PDF export |
| GET | `/student-fees/explorer/export/semester-wise` | `STUDENT_FEE_EXPORT_SEMESTER_WISE` | Semester-wise export variant |
| POST | `/student-fees/refunds` | `FEE_REFUND` | Initiate a refund (auto-detects STUDENT/ENQUIRY) |
| GET | `/student-fees/refunds` | `FEE_REFUND_APPROVE` | Paginated refund list, filterable |
| GET | `/student-fees/refunds/export` | `FEE_REFUND_EXPORT` | Excel/PDF export |
| GET | `/student-fees/refunds/pending` | `FEE_REFUND_APPROVE` | Pending-only list |
| POST | `/student-fees/refunds/{id}/approve` | `FEE_REFUND_APPROVE` | Approve |
| POST | `/student-fees/refunds/{id}/approve-onebook` | `FEE_REFUND_APPROVE` | Approve + push payout via OneBook |
| POST | `/student-fees/refunds/{id}/reject` | `FEE_REFUND_APPROVE` | Reject (blocked for `AUTO_EXCESS`) |
| GET | `/student-fees/receipts` | `STUDENT_FEE_VIEW` | All receipt summaries (grouped by receipt number) |
| GET | `/student-fees/{id}/receipts` | `STUDENT_FEE_VIEW` | One student's receipts (incl. refund/enquiry-payment rows) |
| GET | `/student-fees/{id}/receipts/{receiptId}` | `STUDENT_FEE_VIEW` | Single receipt |
| GET | `/student-fees/{id}/credit-applications` | `STUDENT_FEE_VIEW` | Credit-application audit trail |
| POST | `/enquiries/{id}/payments` | `FEE_COLLECT` | Enquiry-stage payment collection |
| GET | `/enquiries/{id}/payments` | `FEE_COLLECT` | List enquiry payments |
| GET | `/enquiries/{id}/credit-applications` | `ENQUIRY_VIEW` | Credit-application audit trail (enquiry side) |
| GET | `/receipts` | `RECEIPT_VIEW` | Unified receipts list (student + enquiry) |
| GET | `/receipts/{receiptNumber}` | `RECEIPT_VIEW` | Single unified receipt |
| GET | `/receipts/export` | `RECEIPT_EXPORT` | Excel/PDF export |
| GET | `/fee-reports/outstanding` | `FEE_REPORT_VIEW` | Outstanding demands for a term instance |
| GET | `/fee-reports/collection-summary` | `FEE_REPORT_VIEW` | Collection summary for a term instance |
| GET | `/fee-reports/student-ledger` | `FEE_REPORT_VIEW` | Per-student ledger |
| GET | `/fee-demands` | — | List by termInstanceId/enrollmentId/status |
| GET | `/fee-demands/{id}` | — | Single demand |
| POST | `/fee-demands/generate` | `STUDENT_FEE_MANAGE` | Generate demands for a term instance |
| GET | `/guardian/wards/{studentId}/fee-summary` | `MY_WARD_FEE_VIEW` | Ward's fee summary (ownership-verified) |
| GET | `/guardian/wards/{studentId}/fee-receipts` | `MY_WARD_FEE_VIEW` | Ward's receipts |
| GET | `/guardian/wards/{studentId}/fee-penalties` | `MY_WARD_FEE_VIEW` | Ward's penalties |
| POST | `/guardian/wards/{studentId}/fee-payments/orders` | `MY_WARD_FEE_PAY` | Create a Razorpay order for ward's fee |

## 6. Data Model

```
FeeState
  id, name (unique), code (unique), isFallback, isDefault, sortOrder, isActive, createdAt, updatedAt
  -- seeded only (V165): Tamil Nadu (default), Other State (fallback). No admin CRUD.

FeeStructureGroup                                   -- table fee_structure_groups
  id, program(FK), academicYear(FK), course(FK, nullable), quota(MANAGEMENT|COUNSELLING),
  feeState(FK), gender(MALE|FEMALE|OTHER), isActive, createdAt, updatedAt
  UNIQUE (program_id, academic_year_id, course_id, quota, fee_state_id, gender)   -- post-V170

FeeStructure                                         -- table fee_structures
  id, feeStructureGroup(FK), feeType(enum, 12 values), amount, description,
  isMandatory, isActive, createdAt, updatedAt
  UNIQUE (fee_structure_group_id, fee_type)

FeeStructureYearAmount                                -- table fee_structure_year_amounts
  id, feeStructure(FK), yearNumber, yearLabel, amount

StudentFeeAllocation                                  -- table student_fee_allocations
  id, student(FK), program(FK), totalFee, discountAmount, discountReason,
  scholarshipApplication(FK, nullable), scholarshipDiscountAmount, scholarshipDiscountReason,
  agentCommission, netFee, status(FeeAllocationStatus), hasHostelFee,
  finalizedAt, finalizedBy, createdAt, updatedAt

SemesterFee                                           -- table installment_fees
  id, allocation(FK), yearNumber, semesterLabel ("installment_label"), amount, dueDate,
  semesterSequence ("sequence": 1 or 2), createdAt, updatedAt

FeeInstallment                                        -- table fee_installments
  id, semesterFee(FK), student(FK), amountPaid, paymentDate, paymentMode, receiptNumber,
  transactionReference, remarks, refundedAt (nullable), refundNumber (nullable),
  createdAt, updatedAt

FeeRefund                                             -- table fee_refunds
  id, refundNumber (unique, set on APPROVED), originalReceiptNumber, entityType(STUDENT|ENQUIRY),
  studentId (nullable), enquiryId (nullable), studentName, rollNumber, admissionNumber, programName,
  refundAmount, reason, status(PENDING|APPROVED|REJECTED|PAYMENT_FAILED),
  source(MANUAL|AUTO_EXCESS, default MANUAL),                                -- V259
  requestedBy, requestedAt, paymentMode, paymentDate, transactionReference,
  approvedBy, approvedAt, rejectionReason, createdAt
  CONSTRAINT uq_fee_refunds_active_receipt -- one active (non-REJECTED) refund per receipt

EnquiryCreditApplication                              -- table enquiry_credit_applications
  id, enquiry(FK), student(FK), semesterFee(FK), amountApplied, receiptNumber (VARCHAR(100), V211),
  appliedAt

EnquiryPayment                                        -- pre-admission payments captured on the enquiry
  id, enquiry(FK), amountPaid, paymentDate, paymentMode, receiptNumber, transactionReference,
  remarks, refundedAt (nullable), refundNumber (nullable)

PaymentReceipt                                        -- unified receipt ledger, backs /receipts
  receiptNumber, payerType(STUDENT|ENQUIRY), payerId, payerName, ..., amountPaid, paymentDate,
  paymentMode, transactionReference, remarks, installmentsCovered, feeCategory
  (feeCategory: "TUITION_ONLY" | "TUITION_AND_HOSTEL", derived from allocation.hasHostelFee /
  enquiry.studentType)

FeeDemand                                             -- term-based demand generation (fee-reports)
  id, termInstance(FK), enrollment(FK), status(DemandStatus), ... (see FeeDemandDto)
```

**FeeType enum (12 values):** `TUITION`, `LABORATORY_FEE`, `LIBRARY_FEE`, `EXAMINATION_FEE`,
`CLINICAL_FEE`, `BOOK_AND_PACKET_FEE`, `UNIFORM_AND_SHOES_FEE`, `UNIVERSITY_REGISTRATION_FEE`,
`HOSTEL_FEE`, `TRANSPORT_FEE`, `MISCELLANEOUS`, `LATE_FEE`. Generic (course-total-contributing):
all except `HOSTEL_FEE`/`TRANSPORT_FEE` (Additional).

**AdmissionQuota enum:** `MANAGEMENT`, `COUNSELLING`.

## 7. Edge Cases & Validation Rules

- **No fee configured for a combination:** enquiry guideline lookup 404s after both exact and
  fallback-state attempts fail → enquiry form hard-blocks submission (Create/Update button disabled).
- **Duplicate fee-type in one bulk save:** rejected with `IllegalArgumentException` before any DB
  write (`validateNoDuplicateFeeTypes`).
- **Course total = 0:** bulk create/update rejected (`validateCourseTotalGreaterThanZero`) — sums only
  the 10 "generic" fee types.
- **Negative/blank amounts:** normalized to `0` if blank; negative throws `IllegalArgumentException`
  (`normalizeAmount`).
- **Editing a group with finalized enquiries and no reason supplied:** rejected with
  `IllegalStateException` naming the affected count; supplying a reason proceeds and audit-logs
  before/after amounts.
- **Finalizing twice for the same student:** `allocationRepository.existsByStudentId` check throws
  `IllegalStateException`.
- **Discount exceeding total fee (incl. scholarship):** `netFee < 0` throws
  `IllegalArgumentException("Total discount cannot exceed total fee")`.
- **Collecting more than the term-gated collectible cap:** `IllegalStateException` naming the actual
  collectible figure.
- **Collecting when nothing is due:** `IllegalStateException("No fees are currently due for
  collection...")`.
- **Advance payment exceeding total outstanding without `allowExcess`:** hard rejection
  (`IllegalStateException`), regardless of permission — the flag must be explicitly opted in per
  request.
- **Excess requested on a non-DD/Bank-Transfer mode, or without `FEE_COLLECT_EXCESS`:**
  `IllegalStateException`/`AccessDeniedException` respectively — enforced server-side independent of
  the frontend's checkbox gating.
- **Non-open (future) semester with real outstanding, term-gated path:** blocks collection of every
  later semester in the same call, even if a later semester happens to be fully covered by credit —
  mirrored identically between `collectPayment` and `calculateCollectibleOutstanding` to keep the
  "currently due" figure and the actual collection behavior in sync.
- **Second active refund on the same receipt:** rejected — one active (non-`REJECTED`) refund per
  receipt (`uq_fee_refunds_active_receipt`); a new `AUTO_EXCESS` refund on a receipt that already has
  a manual refund pending is blocked the same way (and vice versa), sequencing excess resolution
  before any full-receipt manual refund.
- **Rejecting an `AUTO_EXCESS` refund:** hard-blocked unconditionally — `IllegalStateException`, no
  override, no permission bypasses this.
- **Approving a non-`PENDING` refund, or rejecting a non-`PENDING` refund:** both rejected with
  `IllegalStateException`.
- **Transaction reference missing for UPI/Bank Transfer/Cheque:** HTTP 400 via
  `@TransactionReferenceRequired` bean validation, both for `CollectPaymentRequest` and
  `EnquiryPaymentRequest`.
- **Enquiry credit exceeding what's needed for the currently-collectible semesters:** the unconsumed
  remainder carries forward (recomputed fresh, not stored as a running balance) to the next
  collection call rather than being wasted or double-applied.

## 8. Known Gaps / Deferred

1. **BR-30 (student type as a 4th fee dimension) is stale** — see SRS §6 and BRD §3/§6 for the full,
   code-verified explanation. Current model: 3 dimensions (quota × fee state × gender); student type
   only filters `HOSTEL_FEE` post-lookup.
2. **`TRANSPORT_FEE` is not filtered by student type** in the current `findForEnquiry(...)` overload —
   a drift from the original BR-23 rule, not documented anywhere else in the repo's business docs.
3. **`FeeState` has no admin CRUD screen** — `FeeStateController` is `GET`-only; adding a new
   fee-segment category requires a Flyway migration.
4. ~~**`FEE_FINALIZE` gates the frontend, but the backend endpoint checks a different permission.**~~
   **Corrected 2026-09-24 — this was a documentation error, not a real bug.** An earlier pass through
   this doc claimed `FeeFinalizationComponent` (route `/student-fees/finalize`, gated by `FEE_FINALIZE`)
   submits to `POST /student-fees/finalize` (`StudentFeeController.finalize`, gated by
   `STUDENT_FEE_MANAGE`), and concluded no controller checks `FEE_FINALIZE` at all. On investigation
   that's wrong on every count: `FeeFinalizationComponent`'s only mutating call is
   `EnquiryService.finalizeFees()`, which posts to `POST /enquiries/{id}/finalize-fees`
   (`EnquiryController.finalizeFees`) — and that endpoint **is** annotated `@perm.has('FEE_FINALIZE')`.
   The frontend/backend permissions match correctly.
   Separately, `POST /student-fees/finalize` is a genuinely different action (create a student's
   term-wise fee allocation/installments from an already-finalized enquiry fee), reached from a
   different screen entirely — `StudentFeeDetailComponent` at route `/student-fees/:studentId`, which
   is itself gated by `STUDENT_FEE_VIEW`/`STUDENT_FEE_MANAGE` on the frontend, matching the backend's
   `STUDENT_FEE_MANAGE` exactly. The two endpoints just happen to share the literal path segment
   `/finalize` under `/student-fees`/`/enquiries`, which is what led the earlier pass to conflate them.
   No code change was made — changing either endpoint's permission would have broken a currently
   correct access gate.
5. General partial refunds and a payment/void-cancellation flow remain explicitly out of scope per
   BR-36, confirmed unchanged in current code.
