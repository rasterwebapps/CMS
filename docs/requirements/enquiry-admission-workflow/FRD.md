# Functional Requirements Document — Enquiry-to-Admission Workflow

**System:** OneCMS / College Management System | **Client:** SKSCON / SKS College Of Nursing | **Built by:** Raster / Raster Images Pvt. Ltd.

---

## 1. Overview

This module implements the funnel that turns a front-office enquiry into an enrolled `Student`: enquiry capture with a live fee guideline, admin fee finalization (discount-only), accounting payment collection with enquiry-credit carry-forward, document submission/verification, and final conversion.

## 2. Actors & Permissions

Exact permission codes found via `@PreAuthorize("@perm.has('...')")` in `EnquiryController`, `EnquiryDocumentController`, `StudentController`, plus related codes seeded in migrations:

| Permission | Used For |
|---|---|
| `ENQUIRY_VIEW` | View enquiry list/detail/pending queues |
| `ENQUIRY_CREATE` | Create a new enquiry |
| `ENQUIRY_EDIT` | Update enquiry, change status, submit documents, convert to student |
| `ENQUIRY_DELETE` | Delete an enquiry |
| `ENQUIRY_EXPORT` | Export enquiries to Excel/PDF |
| `FEE_FINALIZE` | Finalize (discount) an enquiry's fee |
| `FEE_COLLECT` | Collect payment against an enquiry |
| `ENQUIRY_FEE_COLLECT_ADVANCE` | Collect beyond currently-open term outstanding, up to full course fee (`allowAdvance`) — referenced in `EnquiryPaymentRequest` javadoc |
| `FEE_COLLECT_EXCESS` | Collect beyond the full course fee via DD/Bank Transfer, auto-generating a non-rejectable refund (`allowExcess`, BR-36) |
| `DOCUMENT_SUBMISSION_MANAGE` | Add/edit/delete enquiry documents, upload files |
| `DOCUMENT_SUBMISSION_VIEW` | Seeded permission (V88/V123/V172); **not enforced** on any document read endpoint found in code |
| `DOCUMENT_VERIFICATION_MANAGE` | Verify/reject documents, complete verification, list verification-pending queue |
| `STUDENT_VIEW` | Access Student Explorer/list, view student |
| `STUDENT_CREATE` | Create student, assign/generate roll numbers |
| `STUDENT_EDIT` | Edit student, program transfer |
| `STUDENT_EXPORT` | Export student list |

Actors: Front Office (ENQUIRY_*), Admissions Admin (FEE_FINALIZE, ENQUIRY_EDIT, STUDENT_CREATE), Accounts/Cashier (FEE_COLLECT, ENQUIRY_FEE_COLLECT_ADVANCE, FEE_COLLECT_EXCESS), Document Verification staff (DOCUMENT_VERIFICATION_MANAGE), Front Office/Registrar (DOCUMENT_SUBMISSION_MANAGE). Role-to-permission assignment is DB-only via Role Management — not hardcoded.

## 3. Screens & UI Behavior

### 3.1 Enquiry Form (`enquiry-form`)
- Mandatory: Full Name, Phone, Country (default India), State (default Tamil Nadu), Program, Course (conditionally required when the program has courses — dynamic Angular validator), Enquiry Date, Referral Source, Student Type (BR-22).
- Date of Birth: required, must be in the past.
- Gender: required.
- Admission Quota, Fee State (auto-derived from address state), Gender, Student Type feed the 6-dimension fee guideline lookup (BR-3/BR-30); fee panel is read-only and shows a contextual "what's missing" message until all dimensions resolve.
- Referral Type dropdown (from Referral Type master); Agent dropdown appears only when referral type code = `AGENT_REFERRAL`; person-search dropdown appears for `ALUMNI`/`STUDENT` (search students) and `FACULTY` (search faculty) referral codes, populating `referredStudentId`/`referredFacultyId`/`referredStaffId` (only one populated).
- Save button disabled when fee total is 0 / unresolved (hard block).

### 3.2 Fee Finalization (`finance/fee-finalization`)
- Lists enquiries in `INTERESTED` status (plus, per code comments, can be re-edited any time before `ADMITTED`).
- Shows guideline total, quota/state/gender/studentType as read-only context, commission info (display only).
- Admin enters `discountAmount` (must not exceed total fee) + `discountReason`; net fee computed as `totalFee − discountAmount`; year-wise/term-wise split editable.
- Cannot set `totalFee` above the guideline.

### 3.3 Payment / Fee Collection (`finance/fee-collection`)
- Lists `FEES_FINALIZED`+ enquiries eligible for collection.
- Fields: `amountPaid` (positive), `paymentDate`, `paymentMode` (`CASH/CARD/UPI/BANK_TRANSFER/CHEQUE`), `transactionReference` (conditionally required — `@TransactionReferenceRequired` for electronic modes, BR-19), remarks.
- `allowAdvance` checkbox (requires `ENQUIRY_FEE_COLLECT_ADVANCE`) permits collecting beyond the current open term.
- `allowExcess` checkbox (requires `FEE_COLLECT_EXCESS` + DD/Bank Transfer mode) permits collecting beyond the full course fee; the excess auto-generates a non-rejectable `FeeRefund` (source `AUTO_EXCESS`) — see `fee-structure-collection` doc set for full BR-36 detail.

### 3.4 Document Collection / Submission / Verification
- Document list per enquiry with type, status (`NOT_UPLOADED/UPLOADED/VERIFIED/REJECTED/RETURNED`), remarks, uploader/verifier audit fields (`verifiedBy`, `verifiedAt`), file metadata (`fileName`, `contentType`, `fileSize`, `storageKey`), `originalSubmitted` flag.
- Upload endpoint accepts multipart form data; guarded by either `DOCUMENT_SUBMISSION_MANAGE` or `DOCUMENT_VERIFICATION_MANAGE`.
- Verification-pending queue accessible under `DOCUMENT_VERIFICATION_MANAGE`.
- Per-document history view (`document/{id}/history`) shows every verification action.

### 3.5 Admission Completion / Convert (`admission-completion`, `enquiry-convert`)
- Only reachable from `DOCUMENTS_VERIFIED` status.
- Conversion prefill endpoint returns data to pre-populate the student creation form from the enquiry.
- Two convert variants exist in the controller: `PUT /{id}/convert` and `POST /{id}/convert` (`convertToStudentWithData`) — both `ENQUIRY_EDIT`-gated.
- Admission printable form: A4 portrait, excludes Academic Qualifications section, document checklist in two balanced columns (`ceil(count/2)` rows), shows `PASSPORT_PHOTO` document in photo box, download output identical to view/print.

### 3.6 Student Explorer (`student/student-list`)
- Filters: Program, Course, Academic Year, Status, Student Type, free-text search; paginated (`Pageable`, default size 25, sorted by `admissionNumber`).

## 4. Functional Workflows

### 4.1 Enquiry Status Transitions
```
create enquiry                       → ENQUIRED
mark interested                      → INTERESTED
mark not interested                  → NOT_INTERESTED
finalize fees (FEE_FINALIZE)         → FEES_FINALIZED
full payment collected               → FEES_PAID
partial payment collected            → PARTIALLY_PAID
all mandatory documents uploaded     → DOCUMENTS_SUBMITTED
all mandatory documents verified     → DOCUMENTS_VERIFIED
complete admission (from DOCUMENTS_VERIFIED only) → ADMITTED  [irreversible]
```
Implemented enum values only: `ENQUIRED, INTERESTED, NOT_INTERESTED, FEES_FINALIZED, FEES_PAID, PARTIALLY_PAID, DOCUMENTS_SUBMITTED, DOCUMENTS_VERIFIED, ADMITTED` (no `CLOSED`/`CANCELLED` — see Known Gaps).

### 4.2 Fee Guideline Formula
```
guideline = FeeStructureGroup.total WHERE program=P, course=C, academicYear=current,
            quota=Q, feeState=S, gender=G, studentType=T
IF no exact match: retry with S = the FeeState where isFallback = true
IF still none: block save
```

### 4.3 Commission Resolution (server-side, at enquiry create/update)
```
commissionAmount =
    IF agent selected AND agent.commissionAmount > 0 → agent.commissionAmount, source=AGENT
    ELSE IF referralType.hasCommission → referralType.commissionAmount, source=REFERRAL_TYPE
    ELSE → 0, status=NOT_APPLICABLE
```

### 4.4 Enquiry Credit Application (BR-45)
```
availableCredit = SUM(enquiry_payments) − SUM(enquiry_credit_applications.amount for this enquiry)
On every collection call against the converted student:
  for each open semester (earliest first):
    apply MIN(availableCredit, semester remaining balance) as a new
    EnquiryCreditApplication row (amount, semester fee ref, source receipt numbers joined)
    THEN apply newly tendered payment to remaining balance
```

## 5. API Endpoints

| Method | Path | Permission | Notes |
|---|---|---|---|
| POST | `/enquiries` | `ENQUIRY_CREATE` | Create enquiry (`EnquiryRequest` → `EnquiryResponse`) |
| GET | `/enquiries` | — (no explicit annotation) | List (basic) |
| GET | `/enquiries/page` | `ENQUIRY_VIEW` | Paginated list |
| GET | `/enquiries/document-pending` | `ENQUIRY_VIEW` | Enquiries awaiting document submission |
| GET | `/enquiries/document-verification-pending` | `DOCUMENT_VERIFICATION_MANAGE` | Verification queue |
| GET | `/enquiries/admission-pending` | `ENQUIRY_VIEW` | Enquiries ready to convert |
| GET | `/enquiries/{id}` | — | Get by id |
| GET | `/enquiries/{id}/summary` | — | Summary DTO |
| GET | `/enquiries/{id}/year-wise-fee-status` | — | Year-wise fee status |
| GET | `/enquiries/{id}/status-history` | — | `EnquiryStatusHistoryResponse` list |
| PUT | `/enquiries/{id}` | `ENQUIRY_EDIT` | Update |
| PATCH | `/enquiries/{id}/status` | `ENQUIRY_EDIT` | Manual status update |
| POST | `/enquiries/{id}/finalize-fees` | `FEE_FINALIZE` | `FeeFinalizationRequest` → `FeeFinalizationResponse` |
| POST | `/enquiries/{id}/submit-documents` | `ENQUIRY_EDIT` | Mark documents submitted |
| PUT | `/enquiries/{id}/convert` | `ENQUIRY_EDIT` | Convert (variant 1) |
| POST | `/enquiries/{id}/convert` | `ENQUIRY_EDIT` | Convert with data (`EnquiryConversionRequest`) |
| GET | `/enquiries/{id}/conversion-prefill` | `ENQUIRY_VIEW` | `EnquiryConversionPrefillResponse` |
| POST | `/enquiries/{id}/payments` | `FEE_COLLECT` | `EnquiryPaymentRequest` → `EnquiryPaymentResponse` |
| GET | `/enquiries/{id}/payments` | `FEE_COLLECT` | Payment history |
| GET | `/enquiries/{id}/credit-applications` | `ENQUIRY_VIEW` | `EnquiryCreditApplicationDto` list (BR-45) |
| DELETE | `/enquiries/{id}` | `ENQUIRY_DELETE` | Delete |
| GET | `/enquiries/export` | `ENQUIRY_EXPORT` | Excel/PDF export with many filters |
| POST | `/enquiries/{enquiryId}/documents` | `DOCUMENT_SUBMISSION_MANAGE` | Add document record |
| GET | `/enquiries/{enquiryId}/documents` | — (no annotation) | List documents |
| PUT | `/enquiries/{enquiryId}/documents/{id}` | `DOCUMENT_SUBMISSION_MANAGE` | Update |
| DELETE | `/enquiries/{enquiryId}/documents/{id}` | `DOCUMENT_SUBMISSION_MANAGE` | Delete |
| GET | `/enquiries/{enquiryId}/documents/verification-status` | — (no annotation) | Status summary |
| POST | `/enquiries/{enquiryId}/documents/upload` | `DOCUMENT_SUBMISSION_MANAGE` or `DOCUMENT_VERIFICATION_MANAGE` | Multipart upload |
| POST | `/enquiries/{enquiryId}/documents/complete-verification` | `DOCUMENT_VERIFICATION_MANAGE` | Bulk-complete |
| PUT | `/enquiries/{enquiryId}/documents/{id}/verify` | `DOCUMENT_VERIFICATION_MANAGE` | Verify one |
| PUT | `/enquiries/{enquiryId}/documents/{id}/reject` | `DOCUMENT_VERIFICATION_MANAGE` | Reject one |
| GET | `/enquiries/{enquiryId}/documents/{id}/history` | — (no annotation) | Per-document history |
| GET | `/enquiries/{enquiryId}/documents/{id}/download` | — (no annotation) | Download file |
| GET | `/students/explorer` | — (no annotation) | Filtered/paginated student search |
| POST | `/students` | `STUDENT_CREATE` | Create student directly (non-enquiry path) |
| GET | `/students` | — | List |
| GET | `/students/export` | `STUDENT_EXPORT` | Export |

## 6. Data Model

- **`enquiries`** (`Enquiry`): `id`, `version`, `name`, `email`, `phone`, `date_of_birth`, `gender`, `program_id`, `course_id`, `academic_year_id`, `enquiry_date`, `status`, `student_type`, `admission_quota`, `fee_state_id`, `agent_id`, `referral_type_id` (NOT NULL), `remarks`, `fee_discussed_amount`, `final_calculated_fee`, `commission_amount`, `guideline_commission_amount`, `commission_source`, `commission_paid_amount`, `commission_payment_status`, `commission_rejection_reason/by/at`, `commission_number`, `year_wise_fees` (TEXT/JSON), `term_wise_fees`/`semester_wise_fees` (TEXT/JSON), `finalized_total_fee`, `finalized_discount_amount`, `finalized_discount_reason`, `finalized_net_fee`, `finalized_by`, `finalized_at`, `country_id`, `state`, `district`, `referred_student_id`, `referred_faculty_id`, `referred_staff_id`, `converted_student_id`, `admission_source` (default `'ENQUIRY_FLOW'`), `created_at`, `updated_at`.
- **`enquiry_documents`** (`EnquiryDocument`): `id`, `enquiry_id`, `admission_id`, `document_type`, `status` (`DocumentVerificationStatus`), `remarks`, `verified_by`, `verified_at`, `file_name`, `content_type`, `file_size`, `uploaded_at`, `storage_key`, `original_submitted`, `created_at`, `updated_at`.
- **`enquiry_document_history`** (`EnquiryDocumentHistory`) — per-action audit trail for document verification.
- **`enquiry_status_history`** (`EnquiryStatusHistory`) — records every status transition.
- **`enquiry_payments`** (`EnquiryPayment`) — pre-admission payments against an enquiry.
- **`enquiry_credit_applications`** (`EnquiryCreditApplication`) — one row per credit consumption at student-fee collection time; references enquiry, student, target semester fee, amount, source receipt number(s), timestamp (BR-45, V209; `receipt_number` widened to `VARCHAR(100)` in V211).
- Relationships: `Enquiry → Program`, `→ Course`, `→ AcademicYear`, `→ FeeState`, `→ Agent`, `→ ReferralType`; `Enquiry.convertedStudentId → Student.id` (loose FK by id, not a JPA relation); `EnquiryDocument → Enquiry`, `→ Admission`.

## 7. Edge Cases & Validation Rules

- Course is required only when the selected program has courses configured (dynamic validator, re-evaluated on program change).
- Fee guideline lookup requires all 6 dimensions resolved; partial selection shows contextual guidance, not an error, until complete.
- Discount amount must not exceed the pre-loaded total fee; fee can never be increased at finalization.
- `allowExcess` payments are DD/Bank Transfer only and require `FEE_COLLECT_EXCESS`; the excess portion becomes a non-rejectable `AUTO_EXCESS` refund (see `fee-structure-collection` FRD for full mechanics).
- `TransactionReferenceRequired` validator enforces a transaction reference for electronic payment modes (BR-19).
- Conversion is blocked unless status is exactly `DOCUMENTS_VERIFIED`; once converted, no reversal path exists in the controller.
- Only one of `referred_student_id`/`referred_faculty_id`/`referred_staff_id` is populated per enquiry, driven by the selected referral type code.
- Document requirement changes on a program do not retroactively affect already-admitted students: newly required documents appear as `NOT_UPLOADED`; removed-requirement documents already collected are preserved and shown as collected-but-not-required.
- Client-submitted `feeGuidelineTotal`, `referralAdditionalAmount`, `finalCalculatedFee` in `EnquiryRequest` are accepted as fields but the backend is documented (BR-3/BR-5/BR-6) as not trusting them for the authoritative calculation — it recomputes server-side.

## 8. Known Gaps / Deferred

- `CLOSED`/`CANCELLED` statuses described in BR-7/BR-8 are not implemented in `EnquiryStatus`; no "close enquiry" endpoint exists.
- `DOCUMENT_SUBMISSION_VIEW` permission is seeded but not enforced anywhere in `EnquiryDocumentController`; several document read endpoints (list, verification-status, history, download) and a few enquiry read endpoints (`GET /enquiries`, `GET /enquiries/{id}`, `/summary`, `/year-wise-fee-status`, `/status-history`) carry no `@PreAuthorize` annotation at all — access control for these appears to rely only on general authentication, not a specific permission code.
