# Business Requirements Document — Enquiry-to-Admission Workflow

**System:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing | **Built by:** Raster / Raster Images Pvt. Ltd.

---

## 1. Executive Summary / Business Objective

SKSCON's admissions process runs through several hand-offs — front office capture, admin fee approval, cashier collection, document verification, and finally admission — each historically done by a different team. This module digitizes that entire pipeline as a single `Enquiry` record that carries state through each hand-off automatically, so that: (a) fee totals shown to a prospect are always the institution's current authoritative rate (never manually mistyped), (b) no student is admitted without their fee being finalized, paid, and documents verified, and (c) money collected before a student officially exists is never lost when they convert.

## 2. Stakeholders

- **Front Office** — first point of contact; owns enquiry creation and interest tracking.
- **Admissions Admin** — owns fee finalization (discount approval) and final admission conversion.
- **Accounts/Cashier team** — owns payment collection against finalized enquiries.
- **Document Verification staff** — owns document intake/verification gate before admission.
- **Prospective students / parents** — indirect stakeholders; experience is the enquiry form and its fee transparency.
- **College Management (SKSCON)** — needs pipeline visibility (Student Explorer, exports) and financial control (no fee edits above guideline).

## 3. Business Rules

| ID | Rule | Rationale |
|---|---|---|
| BR-ENQ-1 (BR-3) | Fee guideline auto-loads from program+course+quota+feeState+gender+studentType for the current academic year; read-only; blocks submission if unconfigured (with Other-State fallback) | Prevents front office from quoting a fee that doesn't match institutional policy |
| BR-ENQ-2 (BR-6) | Admin fee finalization may only discount, never increase, the guideline total; requires a reason; recorded separately from the enquiry's original values | Preserves an audit trail and prevents fee inflation |
| BR-ENQ-3 (BR-7) | Payment collection is allowed only from `FEES_FINALIZED` onward, and remains allowed through `DOCUMENTS_SUBMITTED`/`DOCUMENTS_VERIFIED` without reverting status; blocked for `NOT_INTERESTED`/closed/`ADMITTED`/converted enquiries | Lets cashiers collect balances at any pre-admission stage without disrupting document workflow |
| BR-ENQ-4 (BR-8) | Enquiry status transitions automatically based on system actions, not manual free-form edits | Guarantees the pipeline state always matches what actually happened |
| BR-ENQ-5 (BR-9) | Admission only proceeds once all mandatory documents are verified (`DOCUMENTS_VERIFIED`) | Compliance / regulatory document completeness before enrolling a student |
| BR-ENQ-6 (BR-10) | Conversion to student is a one-way, irreversible action from `DOCUMENTS_VERIFIED`, generating a roll number and linking `convertedStudentId` | Once a student record exists, the enquiry is historical, not editable state |
| BR-ENQ-7 (BR-10) | Admission document requirements are read from the student's program's *current* mapping at any time; documents removed from requirements later are preserved, not deleted, and not counted as missing | Program requirement changes must not retroactively invalidate past admissions |
| BR-ENQ-8 (BR-11) | Student Explorer surfaces every student regardless of admission channel, with program/course/academic-year/status/student-type/search filters | Gives management a single view across all admission paths |
| BR-ENQ-9 (BR-12) | Student type (Day Scholar/Hosteler) is one of the fee-structure-group dimensions (superseded from the older post-lookup HOSTEL/TRANSPORT filtering by BR-30) | Ensures hostel/transport charges are applied correctly per student |
| BR-ENQ-10 (BR-18) | Country/state are required on every enquiry, defaulting to India/Tamil Nadu, and carry forward to the student record | Supports geographic analytics and government admission reporting |
| BR-ENQ-11 (BR-21) | Student first-graduate status and parent education are optional capture fields, kept in sync between `Student` and the scholarship eligibility profile | Supports first-graduate scholarship determination |
| BR-ENQ-12 (BR-22) | Name, phone, country, state, program, course (if applicable), enquiry date, referral source, and student type are mandatory before an enquiry can be saved; referral source conditionally surfaces a person-search (agent/student/faculty) | Enforces minimum viable data capture and links referrals to a real person where applicable |
| BR-ENQ-13 (BR-45) | Enquiry-stage pre-payments become reusable credit against the converted student's semester fees, recomputed fresh each collection (`total enquiry payments − credit already applied`), applied earliest-open-semester-first, ahead of new tendered amounts, with a full audit trail per application | No pre-admission payment is ever lost or requires manual reconciliation after conversion |

## 4. Business Process / Workflow Narrative

### 4.1 Status State Machine
```
ENQUIRED
  → INTERESTED  (front office marks interest)
      → FEES_FINALIZED   (admin finalizes fee, BR-6)
          → FEES_PAID          (full payment)      ─┐
          → PARTIALLY_PAID     (partial payment)    ├→ DOCUMENTS_SUBMITTED
                                                      │      → DOCUMENTS_VERIFIED
                                                      │           → ADMITTED (irreversible)
  → NOT_INTERESTED  (front office marks not interested)
```
Note: code implements 9 states exactly as above; the `CLOSED` terminal state described in the narrative BR text was not found implemented (see enquiry-admission-workflow SRS §6 Known Gaps) — `NOT_INTERESTED` is the de facto terminal non-conversion state in the shipped system.

### 4.2 Fee Guideline & Finalization Formula
```
Fee Guideline Total  = Fee Structure Group total for
                        (program, course, academicYear=current, quota, feeState, gender, studentType)
                        [fallback to feeState.isFallback=true if exact match missing]

Commission Payable    = Agent.commissionAmount (if agent selected and > 0)
                         ELSE ReferralType.commissionAmount (if hasCommission=true)
                         ELSE 0
                         [never added to student fee]

Finalized Net Fee     = finalizedTotalFee (≤ guideline total) − discountAmount
```

### 4.3 Payment & Credit Application
Cashier collects against `FEES_FINALIZED`+ enquiries (full/partial, `CASH/CARD/UPI/BANK_TRANSFER/CHEQUE`). On conversion, any unconsumed enquiry-stage payment total is applied automatically as credit against the new student's earliest-open semester fee demand(s) before new tendered amounts, with each consumption recorded as an `EnquiryCreditApplication` row.

### 4.4 Document → Admission Gate
Required documents vary by program (10th/12th certs, TC, Migration, Community cert, Aadhar, photos, Income cert for scholarship). Each document is `NOT_UPLOADED → UPLOADED → VERIFIED` (or `REJECTED`/`RETURNED`). All-mandatory-uploaded moves the enquiry to `DOCUMENTS_SUBMITTED`; all-mandatory-verified moves it to `DOCUMENTS_VERIFIED`, the only status from which conversion is allowed.

## 5. Success Criteria

Not formally defined with explicit KPIs in the reviewed documentation — inferred from feature completeness: every enquiry captured through front office reaches a terminal state (`ADMITTED` or `NOT_INTERESTED`) with a fully auditable trail of status changes, fee finalization, payments, and document verification, and zero enquiry-stage payment is left unaccounted for after conversion.

## 6. Assumptions & Constraints

- Backend is the sole source of truth for fee totals and commission amounts; the client never supplies a trusted fee/commission value (BR-3, BR-5, BR-6).
- A fee finalization can only reduce the guideline total, never exceed it.
- Referral commission is always tracked separately from the student's fee and never inflates it.
- The multi-dimension Fee Structure module (quota × state × gender × studentType) must be fully configured for a program/academic-year before enquiries against it can be created.

## 7. Known Gaps / Deferred

- `CLOSED`/`CANCELLED` enquiry statuses referenced in BR-7/BR-8 narrative text are not present in the implemented `EnquiryStatus` enum — no explicit "close enquiry" action exists in `EnquiryController`.
- `DOCUMENT_SUBMISSION_VIEW` permission is seeded in the database but the document read endpoints (`GET` list, verification-status, history, download) carry no `@PreAuthorize` check in `EnquiryDocumentController`, so this permission does not appear to gate anything in the current code.
