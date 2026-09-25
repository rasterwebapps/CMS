# Business Requirements Document — Fee Structure & Collection

**Product:** OneCMS / College Management System
**Company:** Raster / Raster Images Pvt. Ltd.
**Client:** SKSCON / SKS College Of Nursing
**Module:** Finance — Fee Structure, Fee Finalization, Payment Collection, Refunds

---

## 1. Executive Summary

SKSCON needs each program's fee to vary legitimately by admission dimension (management vs
counselling quota, in-state vs out-of-state, gender) and by academic year, while guaranteeing every
rupee quoted at enquiry time, finalized at admission time, and collected at payment time traces back
to one authoritative, backend-computed number — never a browser-submitted total, never a stale prior
year's figure. The module also has to reconcile a structural reality of the admissions funnel: money
often arrives *before* a person is a student (enquiry-stage payments, bank-disbursed loan amounts that
overshoot what's actually owed) and must be tracked, applied, or refunded without ever being silently
dropped or requiring manual reconciliation.

This module implements that: a 3-dimension (quota × fee state × gender) fee structure keyed to
program/academic year/course, an enquiry-to-finalization-to-semester-billing pipeline, a
credit-carry-forward mechanism for pre-admission payments, and a permission-gated, non-rejectable
auto-refund mechanism for bank-rail overpayments.

## 2. Stakeholders

| Stakeholder | Interest |
|---|---|
| Front Office | Needs an accurate, automatic fee guideline while capturing an enquiry, with hard submission blocking if no fee is configured. |
| Admissions Admin | Finalizes fees per admitted student; needs to apply discounts without any risk of accidentally increasing a fee. |
| Accounting / Cashier | Collects payments term-by-term and via advance payment; needs bank-excess handling that doesn't require manual off-system tracking. |
| Finance/Approver role | Approves or rejects refund requests; needs an audit trail and protection against staff rejecting money that is unconditionally owed back. |
| Student / Guardian | Needs visibility into their own (or their ward's) fee status, receipts, and penalties, and — for guardians — a way to pay online. |
| Institution management | Needs assurance that fee totals are penny-accurate and that referral/agent commission never inflates what a student is actually charged. |

## 3. Business Rules

The table below maps every fee-structure-and-collection-relevant BR from `BUSINESS_REQUIREMENTS.md`
to a BR-FEE-N id, with a note on current, verified status.

| ID | Source | Rule | Rationale | Status |
|---|---|---|---|---|
| BR-FEE-1 | BR-1 | Fee structure is scoped per program + academic year (+ course); one group per dimension combination; not carried forward automatically each year. | Fees legitimately change year to year; forcing re-entry prevents silently reusing stale figures. | Current, with dimension key narrowed by BR-30/V170 (see BR-FEE-9). |
| BR-FEE-2 | BR-1 | Fee types split into Generic (contribute to Course Total) vs Additional (`HOSTEL_FEE`, `TRANSPORT_FEE` — shown separately, not in the course total). | Lets enquiry/finalization pick the right additional fee by accommodation type without double-counting in the headline total. | Current. |
| BR-FEE-3 | BR-2 | Year-wise fee boxes generated per fee type, count = `Program.durationYears`; at least one generic-fee year amount must be > 0. | Institutions charge different amounts per program year (e.g. lab-heavy final year). | Current. |
| BR-FEE-4 | BR-3 | Enquiry-screen fee guideline is read-only, auto-loaded once required dimensions are filled, backend-computed; submission is hard-blocked if no configuration is found (after fallback). | Prevents front-office staff from hand-typing (and getting wrong) a fee figure. | Current, but the field set is 5 required dimensions (program, quota, feeState-derived, gender, +course if applicable), not 6 — student type is used only to filter `HOSTEL_FEE`, confirmed by code (see BR-FEE-9). |
| BR-FEE-5 | BR-6 | Admin Fee Finalization pre-populates the guideline total read-only; admin may only discount, never increase; discount capped at the total fee. | Prevents finalization from becoming a second, uncontrolled place fees can change. | Current (`FeeFinalizationService.finalize` rejects negative `netFee`). |
| BR-FEE-6 | BR-7 | Payment Collection Screen operates on `FEES_FINALIZED` enquiries; full or partial payment; balance collection remains allowed through DOCUMENTS_SUBMITTED/VERIFIED (not NOT_INTERESTED/CLOSED/CANCELLED/ADMITTED/converted). | Keeps pre-admission fee collection flexible around the realistic order documents/fees actually arrive in. | Current (enquiry-stage payment; distinct from post-admission student-fee collection). |
| BR-FEE-7 | BR-13 | Each program-year fee is split into exactly 2 semester installments at finalization (Sem 1 = floor(amount/2), Sem 2 = remainder); a student may pay any amount at any time; one receipt spans however many semesters a single payment covers. | Matches the institution's real semester billing cadence while not forcing rigid minimum payments. | Current — confirmed verbatim in `FeeFinalizationService.finalize` (`RoundingMode.FLOOR`) and `PaymentCollectionService`'s cascading loop. |
| BR-FEE-8 | BR-19 | Transaction reference mandatory for UPI/Bank Transfer/Cheque; not required for Cash/Card. | Electronic/instrument payments are traceable to a bank-side record; cash/card are not expected to carry one. | Current (`@TransactionReferenceRequired` on `EnquiryPaymentRequest`, `CollectPaymentRequest`). |
| BR-FEE-9 | BR-30 | Fee varies by admission dimensions, one group per unique combination, scoped to program/year/course. | Institutions legitimately charge different totals by quota, state, and gender-linked hostel/scholarship policy. | **Partially stale — see note below.** |
| BR-FEE-10 | BR-20 | `FeeType` enum covers 12 types (10 Generic incl. `CLINICAL_FEE`/`BOOK_AND_PACKET_FEE`/`UNIFORM_AND_SHOES_FEE`/`UNIVERSITY_REGISTRATION_FEE`, 2 Additional). `LAB_FEE` renamed to `LABORATORY_FEE`. | Supports nursing/paramedical/vocational fee components beyond a generic college's fee types. | Current — verified against `com.cms.model.enums.FeeType`. |
| BR-FEE-11 | BR-23 | Authoritative, backend-only calculation: current-academic-year scoping, course-exact matching, active-rows-only, referral commission excluded from student fee, exact 2-decimal normalization, integer-paise frontend arithmetic. | A prior real incident (₹23,45,000 shown instead of ₹10,00,000 from summing multiple years' rows) makes this a hard, non-negotiable rule. | Current. |
| BR-FEE-12 | BR-36 | Bank-rail (DD/Bank Transfer) advance payments may exceed total outstanding when the collector holds `FEE_COLLECT_EXCESS`; the excess is auto-carved into a non-rejectable `AUTO_EXCESS` `FeeRefund` in the same transaction; general partial refunds and a void/cancel flow remain explicitly out of scope. | Bank/education-loan disbursements routinely release more than currently owed; the excess must never be silently dropped or manually tracked outside the system. | Current — verified in `PaymentCollectionService.collectAdvancePayment` and `FeeRefundService`. |
| BR-FEE-13 | BR-45 | Pre-admission enquiry payments become spendable credit against the converted student's semester fees, recomputed fresh each collection (`total enquiry payments − credit already applied`), consumed earliest-open-semester-first, ahead of newly tendered money; each consumption recorded in `enquiry_credit_applications`. | Money paid before conversion must never be "lost" or require manual reconciliation once the person becomes a student. | Current — verified in `PaymentCollectionService.collectPayment`/`collectAdvancePayment`. |

### Note on BR-FEE-9 / BR-30 staleness (as requested — verified against current code)

`BUSINESS_REQUIREMENTS.md`'s **BR-30** section (and its cross-references from BR-1, BR-3, BR-23)
describes **Student Type** (`DAY_SCHOLAR`/`HOSTELER`) as a fourth fee-structure dimension, with a
`FeeStructureGroup` unique key of `(program, academicYear, course, quota, feeState, gender,
studentType)`. This was true when BR-30 shipped (migrations V165–V168), but migration
**`V170__remove_student_type_from_fee_structure_groups.sql`** — a later, separate migration —
explicitly deleted all existing fee-structure data and dropped the `student_type` column, replacing
the unique constraint with a 6-field key that excludes it:

```sql
-- V170, verbatim comment:
-- Student type is no longer a fee dimension.
-- Day scholar vs hosteler cost is implicit: HOSTEL_FEE row = hosteler surcharge.
```

This is confirmed against the **current** `FeeStructureGroup.java` entity (3 dimension fields: `quota`,
`feeState`, `gender` — no `studentType` field at all), the `fee_structure_groups` table's current
constraint, and the frontend fee-structure form's Combination Picker (`fee-structure-form.component.ts`
— 6 controls: `academicYearId`, `programId`, `courseId`, `quota`, `feeStateId`, `gender`; no
`studentType` control).

**Current real behavior:** a student's Day Scholar vs Hosteler status does not select a different fee
group — it only determines whether the `HOSTEL_FEE` line item is included when the guideline total is
computed (`FeeStructureService.findForEnquiry(..., StudentType, ...)` filters out `HOSTEL_FEE` for
`DAY_SCHOLAR`; `TRANSPORT_FEE` is currently included for everyone regardless of student type, which is
itself a further drift from BR-23's original TRANSPORT_FEE-only-for-DAY_SCHOLAR text — also not yet
reflected in BUSINESS_REQUIREMENTS.md).

**Recommendation:** BUSINESS_REQUIREMENTS.md's BR-30 section (and its "Fee Dimensions" table, "Data
Model" sketch, and "Migration Notes") should be updated to describe the 3-dimension model and note
V170 as the rule that superseded it, the same way BR-30 itself documents that it amended BR-1/BR-3/
BR-23. This BRD reflects the current, correct behavior; no code change is implied or needed.

## 4. Business Process / Workflow Narrative

### 4.1 Fee configuration

An admin picks Academic Year → Program → Course (optional) → Quota → Fee State → Gender in the
Combination Picker. Once all 6 fields are set, a Fee Grid appears with one row per `FeeType` and one
column per program year (count = `Program.durationYears`). Saving creates one `FeeStructureGroup` plus
one `FeeStructure` item per fee type (each with its own `FeeStructureYearAmount` rows). A duplicate
6-field combination is rejected. The **Course Total (Generic)** — sum of the 10 Generic fee-type
amounts, excluding `HOSTEL_FEE`/`TRANSPORT_FEE` — must exceed zero to save.

### 4.2 Enquiry fee guideline

As the front office fills the enquiry form, once Program (+Course if applicable), Quota, Gender, and
address State (auto-mapped to a `FeeState`) are all set, `GET /fee-structures/guideline` is called.
The backend:
1. Looks up an exact `FeeStructureGroup` match for the current academic year.
2. If none, retries with the fallback `FeeState` ("Other State") holding quota/gender fixed.
3. If still none, returns 404 and the enquiry form hard-blocks submission ("No fee structure configured
   for this combination. Please contact admin.").
4. On a match, items are filtered to drop `HOSTEL_FEE` when the enquiry's student type is
   `DAY_SCHOLAR`; the remaining items are summed into the guideline total, which is stored on the
   enquiry (`feeGuidelineTotal`/`finalCalculatedFee`) and re-derived server-side on every save —
   never trusted from the browser.

### 4.3 Admin finalization

Once an enquiry is `INTERESTED` (or further along the funnel) and the front office has captured
payment interest, an admin opens Fee Finalization. The guideline total is pre-filled read-only. The
admin may enter a discount amount + reason; the backend also auto-adds any approved current-year
`StudentScholarship` discount. `netFee = totalFee - (manualDiscount + scholarshipDiscount)`, rejected
if negative. On save (`StudentFeeAllocation`), for each program year:

```
sem1Amount = floor(yearAmount / 2)
sem2Amount = yearAmount - sem1Amount     // absorbs any odd paisa
```

Two `SemesterFee` rows are created per year, labeled `"Year N - <Ordinal> Installment"`, with due
dates resolved from that year's `TermBillingSchedule` (ODD/EVEN), falling back to a year-shifted date
from the admission year's schedule if the target year's academic year record doesn't exist yet.

### 4.4 Payment collection

Two collection paths exist:

- **Collect Payment (term-gated, bulk list)** — `POST /student-fees/{id}/collect`. Only semesters whose
  term is currently open (`TermInstanceService.isSemesterFeeCollectibleNow`) are eligible; the request
  amount is capped at the sum of currently-collectible outstanding. A non-open semester with real
  outstanding blocks collection of anything later (protects billing order).
- **Advance Payment (per-student)** — `POST /student-fees/{id}/collect-advance`. Ignores the term gate
  entirely — can cover any semester up to **total** outstanding. With `allowExcess=true` on a
  DD/Bank Transfer payment by a `FEE_COLLECT_EXCESS` holder, the cap is bypassed: the receipt records
  the full bank-confirmed amount, and whatever exceeds total outstanding becomes an `AUTO_EXCESS`
  `FeeRefund` in the same transaction.

Both paths first apply any unconsumed enquiry-stage credit (`total enquiry payments minus credit
already applied`, tracked via `enquiry_credit_applications`) against the earliest open/eligible
semester(s), before applying the newly tendered amount — so a payment can be partly "free" from
pre-admission credit without the cashier doing anything special. Within one call, remaining money
cascades across semesters in due order; every semester line the call touches shares one receipt
number.

### 4.5 Refunds

`POST /student-fees/refunds` auto-detects the payer type (STUDENT/ENQUIRY) from the unified receipt
ledger and creates a `PENDING` `FeeRefund` for the receipt's full amount (installments-derived for
students, `amountPaid`-derived for enquiries). Only one active (non-`REJECTED`) refund may exist per
receipt. An approver (`FEE_REFUND_APPROVE`) then either:
- **Approves** — generates a refund number, records payout mode/date/reference, and — unless the
  refund's `source` is `AUTO_EXCESS` — soft-flags the underlying `FeeInstallment`/`EnquiryPayment` rows
  (`refundedAt`/`refundNumber`) so outstanding calculations exclude them.
- **Rejects** — sets `REJECTED` with a reason; **blocked entirely for `AUTO_EXCESS` refunds**
  (`rejectRefund()` throws `IllegalStateException`) since that money was genuinely received and is
  unconditionally owed back.

Approved refunds can alternatively be routed through OneBook (`.../approve-onebook`); the webhook
completion path (`completeOneBookRefund`) mirrors the manual-approval soft-flagging logic exactly.

## 5. Success Criteria

Not formally defined with a numeric KPI in the source documents. Inferred from feature completeness
and the explicit regression requirement in BR-23 ("For BSc Nursing with current-year total
₹10,00,000, an enquiry ... must save and display `finalCalculatedFee = ₹10,00,000`"): the module is
considered successful when (a) every fee figure shown or persisted traces to a single backend
calculation with no drift across enquiry → finalization → collection, (b) no payment or pre-admission
credit is ever silently dropped, and (c) bank-disbursed overpayments are automatically and
unconditionally returned to the payer without manual reconciliation.

## 6. Assumptions & Constraints

- Institution operates on a semester billing cadence (2 semesters per program year); the model does
  not support a trimester or quarterly cadence.
- `FeeState` is a small, effectively-fixed set (Tamil Nadu / Other State) maintained via migration, not
  a self-service master — expanding it currently requires backend deployment.
- Referral/agent commission is tracked for internal payout visibility only and must never increase the
  student-facing fee total (BR-5, BR-23) — enforced by keeping `agentCommission` a separate field on
  `StudentFeeAllocation`, never added into `netFee`.
- Excess-payment handling is scoped to the Advance Payment flow only; the term-gated bulk Collect
  Payment list and enquiry pre-admission payments do not support excess (though `EnquiryPaymentService`
  has an analogous, separately-gated `ENQUIRY_FEE_COLLECT_ADVANCE` advance-payment mechanism for the
  enquiry stage — see `EnquiryPaymentRequest` — which is a related but distinct capability from this
  module's `FEE_COLLECT_EXCESS`).

## 7. Known Gaps / Deferred

1. **BR-30 in BUSINESS_REQUIREMENTS.md is stale** on Student Type being a fee-structure dimension — see
   §3 note above. Current code uses a 3-dimension key (quota × fee state × gender); student type only
   filters the `HOSTEL_FEE` line item post-lookup.
2. **TRANSPORT_FEE is not filtered by student type** in the current guideline lookup, unlike the
   original BR-23 rule (`TRANSPORT_FEE` only for `DAY_SCHOLAR`) — it is now returned for every student
   type. Not documented anywhere in BUSINESS_REQUIREMENTS.md; flagged here for awareness, not evaluated
   for correctness (business-intent unclear from code alone).
3. **No admin-facing CRUD for the `FeeState` master** — read-only endpoint only; new fee-segment
   categories require a migration.
4. General partial refunds and a payment/receipt void flow remain explicitly out of scope per BR-36,
   confirmed unchanged in current code.
