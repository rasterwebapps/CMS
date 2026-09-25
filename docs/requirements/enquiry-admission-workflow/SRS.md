# Software Requirements Specification — Enquiry-to-Admission Workflow

**System:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Module:** Finance & Asset Management (Release 1, Milestone 4) — Enquiry-to-Admission Workflow

---

## 1. Introduction

### 1.1 Purpose
This document specifies the requirements for the enquiry-to-admission workflow: the pipeline that carries a prospective student from initial front-office enquiry through fee finalization, payment collection, document submission/verification, and final conversion into a `Student` record.

### 1.2 Scope
Covers the `Enquiry` entity and its lifecycle, fee-guideline lookup at enquiry time, admin fee finalization, accounting-team payment collection against enquiries, document submission and verification, enquiry-to-student conversion, and the Student Explorer screen. Referral-type selection and commission calculation on the enquiry form are covered briefly (full detail in the `referral-commission-management` doc set). Multi-dimension fee structure configuration itself is covered in the `fee-structure-collection` doc set.

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md` — R1-M4.1b "Enquiry-to-Admission Workflow Enhancement" (~lines 464-508)
- `docs/BUSINESS_REQUIREMENTS.md` — BR-3, BR-6, BR-7, BR-8, BR-9, BR-10, BR-11, BR-12, BR-18, BR-21, BR-22, BR-45
- Source: `backend/src/main/java/com/cms/model/Enquiry.java`, `EnquiryDocument.java`, `EnquiryStatusHistory.java`, `EnquiryCreditApplication.java`, `controller/EnquiryController.java`, `controller/EnquiryDocumentController.java`, `service/EnquiryService.java`, `service/EnquiryDocumentService.java`
- Frontend: `frontend/src/app/features/enquiry/**`, `frontend/src/app/features/student/**`, `frontend/src/app/features/finance/fee-finalization/**`, `frontend/src/app/features/finance/fee-collection/**`

## 2. Overall Description

### 2.1 Product Perspective
This workflow is the admissions funnel of OneCMS. It sits between the front office (data capture) and the Student module (post-admission record). It depends on: Program/Course masters, the multi-dimension Fee Structure module (BR-30), the Referral Type master (BR-4), Agents/Staff Referrers, and the Academic Year master.

### 2.2 Actors / User Classes
- **Front Office staff** — creates/edits enquiries, updates interest status, submits documents.
- **Admin (Fee Finalization)** — reviews `INTERESTED` enquiries, applies discounts, finalizes fee.
- **Accounting / Cashier** — collects payments against `FEES_FINALIZED`+ enquiries.
- **Document Verification staff/Admin** — verifies submitted documents.
- **Admission staff/Admin** — completes admission (converts enquiry to student).
- All access is gated by the DB-driven permission system (no hardcoded roles).

### 2.3 Operating Environment
Angular SPA frontend, Spring Boot REST backend, PostgreSQL via Flyway migrations, Keycloak for identity only (BR-24). No offline mode.

### 2.4 Constraints / Assumptions
- Backend is authoritative for all fee totals; client-submitted fee values are treated as display hints only (BR-3, BR-6).
- The enquiry status field only ever holds the 9 values enumerated in code (see §6 Known Gap re: `CLOSED`).
- Fee guideline lookup depends on the multi-dimension Fee Structure module (quota × state × gender × studentType) being configured; an unconfigured combination hard-blocks enquiry submission.

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|---|---|---|---|
| FR-ENQ-1 | Create/edit an enquiry capturing name, phone, DOB, gender, program, course (conditional), enquiry date, referral type, student type, admission quota, country/state/district, referring person (agent/student/faculty) | Must | Program/Course/ReferralType/Agent masters |
| FR-ENQ-2 | Auto-lookup and display a read-only fee guideline for the enquiry's program+course+quota+feeState+gender+studentType combination in the current academic year, with an "Other State" fallback | Must | Fee Structure module (BR-30) |
| FR-ENQ-3 | Block enquiry creation/update when no fee configuration resolves (including fallback) | Must | FR-ENQ-2 |
| FR-ENQ-4 | Resolve and store a referral commission amount (agent override, else referral-type amount, else 0) independent of the student's fee total | Must | Referral Type / Agent |
| FR-ENQ-5 | Transition enquiry status automatically per action (`ENQUIRED → INTERESTED/NOT_INTERESTED → FEES_FINALIZED → FEES_PAID/PARTIALLY_PAID → DOCUMENTS_SUBMITTED → DOCUMENTS_VERIFIED → ADMITTED`), with status history recorded | Must | — |
| FR-ENQ-6 | Admin fee finalization: pre-populate guideline total (read-only), allow discount only (never an increase), require a reason, transition to `FEES_FINALIZED` | Must | FR-ENQ-2 |
| FR-ENQ-7 | Accounting payment collection against `FEES_FINALIZED`+ enquiries (full or partial, multiple payment modes), auto status transition to `FEES_PAID`/`PARTIALLY_PAID` | Must | FR-ENQ-6 |
| FR-ENQ-8 | Track enquiry-stage pre-payment as reusable credit and apply it automatically to the converted student's semester fee demands, earliest-open-semester first, with a full audit trail (`EnquiryCreditApplication`) | Must | FR-ENQ-7, Student conversion |
| FR-ENQ-9 | Submit and track required documents per enquiry (`EnquiryDocument`); auto-transition to `DOCUMENTS_SUBMITTED` when all mandatory documents are uploaded | Must | — |
| FR-ENQ-10 | Verify/reject submitted documents; auto-transition to `DOCUMENTS_VERIFIED` when all mandatory documents pass verification; maintain per-document history | Must | FR-ENQ-9 |
| FR-ENQ-11 | Convert an enquiry in `DOCUMENTS_VERIFIED` status into a `Student` record (irreversible), carrying forward personal data, program, finalized fee, location, and documents; generate a roll number | Must | FR-ENQ-10 |
| FR-ENQ-12 | Student Explorer: paginated, filterable (program, course, academic year, status, student type, search) list of all students | Must | Student module |
| FR-ENQ-13 | Enquiry list/export with filters (search, date range, status, program, course, student type, referral type, quota, agent, admission source, academic year) and Excel/PDF export | Should | FR-ENQ-1 |
| FR-ENQ-14 | Surface enquiry-stage credit applications on both the Enquiry and Student Fee Detail screens | Should | FR-ENQ-8 |

## 4. External Interface Requirements

### 4.1 Screens
- Enquiry Form (create/edit) — `frontend/src/app/features/enquiry/enquiry-form`
- Enquiry List — `frontend/src/app/features/enquiry/enquiry-list`
- Enquiry Detail — `frontend/src/app/features/enquiry/enquiry-detail`
- Fee Finalization — `frontend/src/app/features/finance/fee-finalization`
- Payment/Fee Collection — `frontend/src/app/features/finance/fee-collection`
- Document Collection / Submission — `frontend/src/app/features/enquiry/document-collection`, `document-submission`
- Document Verification — `frontend/src/app/features/enquiry/document-verification`
- Admission Completion / Convert — `frontend/src/app/features/enquiry/admission-completion`, `enquiry-convert`
- Student Explorer/List — `frontend/src/app/features/student/student-list`

### 4.2 API Endpoints (high-level; see FRD for full detail)
`POST/GET/PUT/PATCH/DELETE /enquiries`, `/enquiries/{id}/finalize-fees`, `/enquiries/{id}/submit-documents`, `/enquiries/{id}/convert`, `/enquiries/{id}/conversion-prefill`, `/enquiries/{id}/payments`, `/enquiries/{id}/credit-applications`, `/enquiries/{id}/status-history`, `/enquiries/export`, `/enquiries/{enquiryId}/documents/**`, `/students/explorer`.

### 4.3 Key DB Entities
`Enquiry`, `EnquiryDocument`, `EnquiryDocumentHistory`, `EnquiryStatusHistory`, `EnquiryPayment`, `EnquiryCreditApplication`, `Student`, `Admission`.

## 5. Non-Functional Requirements

- **Performance:** List/explorer endpoints are paginated (`Page<T>`); export endpoints stream Excel/PDF generation.
- **Security/RBAC:** Every mutating endpoint is gated by an explicit `@PreAuthorize("@perm.has('...')")` check tied to a DB-driven permission code (BR-24). Document GET endpoints (list, verification-status, history, download) carry **no** `@PreAuthorize` — see Known Gap below.
- **Auditability:** `EnquiryStatusHistory` records every status transition; `EnquiryDocumentHistory` records per-document verification actions; `finalizedBy`/`finalizedAt` capture who finalized fees; `EnquiryCreditApplication` records exactly how enquiry-stage credit was consumed.
- **Data integrity:** `Enquiry` uses optimistic locking (`@Version`). All monetary fields are `BigDecimal`.
- **Backend authority:** All fee totals and commission amounts are computed and trusted only from the backend, never the client (BR-3, BR-5, BR-6).

## 6. Known Gaps / Not Yet Implemented

- **`CLOSED` status is documented but not implemented.** BR-8's status table and transition diagram describe a `CLOSED` status (and BR-7 references a `CANCELLED` status), but the actual `EnquiryStatus` enum (`backend/src/main/java/com/cms/model/enums/EnquiryStatus.java`) has only 9 values: `ENQUIRED, INTERESTED, NOT_INTERESTED, FEES_FINALIZED, FEES_PAID, PARTIALLY_PAID, DOCUMENTS_SUBMITTED, DOCUMENTS_VERIFIED, ADMITTED`. There is no `CLOSED` or `CANCELLED` value and no "close enquiry" action was found in `EnquiryController`. In practice `NOT_INTERESTED` appears to serve as the terminal non-conversion state.
- **Document read endpoints are unauthenticated at the permission-check level.** `EnquiryDocumentController`'s `GET` (list), `/verification-status`, `/{id}/history`, and `/{id}/download` endpoints have no `@PreAuthorize` annotation, unlike the write endpoints (`DOCUMENT_SUBMISSION_MANAGE`/`DOCUMENT_VERIFICATION_MANAGE`). `DOCUMENT_SUBMISSION_VIEW` is seeded as a permission (V88, V123, V172) but is not referenced in any `@perm.has(...)` check found in this controller — it may be enforced only via general authentication, not this specific permission.
- **BR-45 enquiry-credit backfill (V187) is a one-time historical fix**, structurally separate from the ongoing `EnquiryCreditApplication` mechanism (V209) — not an ongoing feature to rely on for new data.
