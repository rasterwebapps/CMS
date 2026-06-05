# 📋 CMS Business Requirements

> **College Management System** — Business rules, workflow definitions, and functional requirements for all modules. This document is the single source of truth for business logic and must be updated whenever any business or workflow change is made.

---

## 📋 Table of Contents

- [General Documentation Policy](#-general-documentation-policy)
- [BR-28: Notification & Alert Preferences](#br-28-notification--alert-preferences)
- [BR-1: Fee Structure & Academic Year](#br-1-fee-structure--academic-year)
- [BR-2: Year-wise Fee Boxes per Program Duration](#br-2-year-wise-fee-boxes-per-program-duration)
- [BR-3: Fee Structure Guideline on Enquiry Screen](#br-3-fee-structure-guideline-on-enquiry-screen)
- [BR-4: Referral Type Master](#br-4-referral-type-master)
- [BR-5: Referral Commission Tracking & Student Fee Separation](#br-5-referral-commission-tracking--student-fee-separation)
- [BR-6: Admin Fee Finalization Workflow](#br-6-admin-fee-finalization-workflow)
- [BR-7: Payment Collection by Accounting Team](#br-7-payment-collection-by-accounting-team)
- [BR-8: Enquiry Status Workflow](#br-8-enquiry-status-workflow)
- [BR-9: Submit Documents](#br-9-submit-documents)
- [BR-10: Convert Enquiry to Student](#br-10-convert-enquiry-to-student)
- [BR-11: Student Explorer with Filters](#br-11-student-explorer-with-filters)
- [BR-12: Student Type on Enquiry](#br-12-student-type-on-enquiry)
- [BR-13: Semester-Wise Fee Collection](#br-13-semester-wise-fee-collection)
- [BR-14: Scholarship Type Master](#br-14-scholarship-type-master)
- [BR-15: Student Scholarship Eligibility Profile](#br-15-student-scholarship-eligibility-profile)
- [BR-16: Scholarship Application Lifecycle](#br-16-scholarship-application-lifecycle)
- [BR-17: Scholarship Disbursement](#br-17-scholarship-disbursement)
- [BR-18: Enquiry Location Fields](#br-18-enquiry-location-fields)
- [BR-19: Transaction Reference Mandatory for Electronic Payments](#br-19-transaction-reference-mandatory-for-electronic-payments)
- [BR-20: Fee Type Classification Expansion](#br-20-fee-type-classification-expansion)
- [BR-21: Student First-Graduate & Parent Education Tracking](#br-21-student-first-graduate--parent-education-tracking)
- [BR-22: Enquiry Form — Mandatory Fields, Location Defaults & Referral-Linked Person Search](#br-22-enquiry-form--mandatory-fields-location-defaults--referral-linked-person-search)
- [BR-23: Authoritative Fee Calculation & Penny-Safe Numeric Rules](#br-23-authoritative-fee-calculation--penny-safe-numeric-rules)
- [BR-24: DB-Driven RBAC & Identity-Only Keycloak](#br-24-db-driven-rbac--identity-only-keycloak)
- [BR-25: Profile Self-Service Editing and User Profile Photo](#br-25-profile-self-service-editing-and-user-profile-photo)
- [BR-26: Faculty Document Review Summary and Verification Locks](#br-26-faculty-document-review-summary-and-verification-locks)
- [BR-27: Permanent Admission Number Generation](#br-27-permanent-admission-number-generation)
- [BR-29: UI Validation & Form Behaviour Standards](#br-29-ui-validation--form-behaviour-standards)
- [BR-30: Multi-Dimension Fee Structure (Quota × State × Gender × Student Type)](#br-30-multi-dimension-fee-structure-quota--state--gender--student-type)
- [BR-31: Student Data Import — Legacy Migration](#br-31-student-data-import--legacy-migration)
- [Enquiry-to-Admission Lifecycle (End-to-End)](#-enquiry-to-admission-lifecycle-end-to-end)
- [Change Log](#-change-log)

---

## 📌 General Documentation Policy

> **Mandatory Rule:** Whenever any business requirement, workflow, status transition, fee logic, or operational process is added or modified, it **must** be documented in this file and all related documents (milestone trackers, manual test cases, CHANGELOG) before the change is considered complete.

This applies to:
- New or modified business rules
- Changes to status workflows or transitions
- Changes to fee calculation logic
- New entity relationships or master data tables
- Changes to role-based access or screen assignments
- New screens or modifications to existing screens

**Failure to document business/workflow changes is considered an incomplete task** and will not pass code review.

---

## BR-1: Fee Structure & Academic Year

### Business Rule

The fee structure is defined **per course per academic year**. Since fees may vary from year to year, every fee structure entry must be scoped to a specific academic year. When viewing or calculating fees, the system must always reference the fee structure for the **current academic year** associated with the selected course.

### Fee Type Classification

All 8 fee types are grouped into two categories displayed on the fee structure screen:

| Category | Fee Types | Included in Course Total |
|---|---|---|
| **Generic** | TUITION, LAB_FEE, LIBRARY_FEE, EXAMINATION_FEE, MISCELLANEOUS, LATE_FEE | ✅ Yes |
| **Additional** | HOSTEL_FEE, TRANSPORT_FEE | ❌ No (shown separately) |

- The **Course Total (Generic)** shown on the fee structure screen **excludes** HOSTEL_FEE and TRANSPORT_FEE.
- The **Additional Fees Total** (HOSTEL_FEE + TRANSPORT_FEE) is shown separately below the course total.
- This separation allows enquiry and finalization screens to pick the relevant additional fee based on the student's accommodation type.

> **⚠️ Amended by BR-30.** The uniqueness key for fee structures has been extended to include `quota`, `feeState`, `gender`, and `studentType`. See [BR-30](#br-30-multi-dimension-fee-structure-quota--state--gender--student-type) for the current authoritative rule.

### Key Points

1. A fee structure record is uniquely identified by the combination of **program + academic year + fee type**.
   - **BR-30 update:** The unique group key is `program + academicYear + course + quota + feeState + gender + studentType`. Multiple groups can exist per program/year, one per dimension combination.
2. **There must be only one fee structure group per course (or program) per academic year per dimension combination.** Creating a second group for the same 7-field combination is rejected by the system.
3. Fee structures from previous academic years are retained for historical reference but are not used for new enquiries or admissions.
4. When a new academic year begins, administrators must create new fee structure entries for each program. Previous year entries are **not** automatically carried forward.
5. The fee structure screen must allow filtering by both program and academic year.
6. A fee structure group can be saved only when the **Course Total (Generic)** is greater than zero.

### Entities Involved

- `FeeStructure` — has `program` (FK) and `academicYear` (FK) fields
- `Program` — defines the program for which fees are configured
- `AcademicYear` — scopes the fee structure to a specific year

### Permissions

- `FEE_STRUCTURE_MANAGE` — can create, update, and delete fee structures

### Academic Year Cohort Seat Allocation

When an academic year is created, the administrator must be able to enter intake seats for each active program/course offered in that admission year. The system creates the corresponding `Cohort` rows during academic-year creation using the existing cohort model.

Key rules:

1. Seat allocation is captured per active `Program` for the new academic year.
2. Each allocation has `managementSeats` and `counsellingSeats`; blank values are saved as `0` and negative values are rejected.
3. The cohort uniqueness remains `program + admissionAcademicYear`, preventing duplicate cohorts for the same program/year.
4. Seat allocation is saved in the same transaction as academic-year creation, so a failed cohort allocation rolls back the academic year setup.
5. Existing academic years continue to manage seats from the academic-year detail screen.

---

## BR-2: Year-wise Fee Boxes per Program Duration

### Business Rule

When selecting a program in the fee structure screen, the system must dynamically generate **year-wise fee input boxes for every fee type** based on the program's `durationYears` field. Each box represents one year of the program (e.g., "Year 1", "Year 2", "Year 3" for a 3-year program). All 8 fee types are shown for all year boxes.

### Key Points

1. The number of year-wise fee boxes equals the `durationYears` value of the **selected program** (not the course).
2. Year boxes are shown for **every fee type** (TUITION, LAB_FEE, LIBRARY_FEE, EXAMINATION_FEE, HOSTEL_FEE, TRANSPORT_FEE, MISCELLANEOUS, LATE_FEE).
3. Each box is labeled sequentially: "Year 1", "Year 2", "Year 3", "Year 4", etc.
4. Each year-wise amount is stored as a separate record in the database, linked to the fee structure.
5. The sum of all year-wise amounts for a fee type equals the total fee for that fee type.
6. Blank year-wise amount boxes are treated as `0`; at least one generic fee year amount must be greater than zero before saving.
7. This allows institutions to have different fee amounts for different years of the same program.
8. The year box count is driven by program duration; course selection only scopes which fee structure to save (per program+course+academic year).

### Example

| Program | Duration | Year Boxes Generated |
|---------|----------|---------------------|
| B.Tech (CSE) | 4 years | First Year, Second Year, Third Year, Fourth Year |
| M.Sc (Physics) | 2 years | First Year, Second Year |
| MBA | 2 years | First Year, Second Year |
| B.Sc (Nursing) | 4 years | First Year, Second Year, Third Year, Fourth Year |

### Entities Involved

- `Program` — `durationYears` field determines the number of boxes
- `FeeStructure` — parent fee definition per program/academic year
- `FeeStructureYearAmount` (new) — stores year-wise amounts linked to fee structure

### Data Model (New)

```
FeeStructureYearAmount:
  - id (PK)
  - feeStructure (FK → FeeStructure)
  - yearNumber (INTEGER, 1-based)
  - yearLabel (VARCHAR, e.g., "First Year")
  - amount (DECIMAL/BigDecimal)
  - createdAt, updatedAt (audit)
```

### Permissions

- `FEE_STRUCTURE_MANAGE` — can define year-wise fee amounts

---

## BR-3: Fee Structure Guideline on Enquiry Screen

> **⚠️ Amended by BR-30.** Fee lookup now requires 6 fields (program, course, quota, state, gender, student type). The old 2-field flow (program + course) is superseded. See [BR-30](#br-30-multi-dimension-fee-structure-quota--state--gender--student-type) for the full current rule.

### Business Rule

When the front office fills in the enquiry form with program, course, quota, state, gender, and student type, the system automatically looks up and displays the **fee for that exact combination in the current academic year** as a read-only guideline. If no configuration is found, submission is blocked until admin configures the fee.

### Key Points

1. The fee guideline is **read-only** on the enquiry screen — it cannot be manually edited.
2. **Flow**: Program → Course (if program has courses) → Quota → (State auto-derived from address state) → Gender → Student Type → Fee auto-loads.
   - For programs with no courses, `courseId = null` is valid; fee auto-loads as soon as the other 5 fields are filled.
   - For programs with courses, a course must be selected before fee loads.
3. Fee lookup uses **all 6 dimensions** against the current active academic year. Missing any required field shows a contextual guidance message in the fee banner; the fee does not load.
4. **Fallback**: If no exact match is found, the system tries the fee state marked `isFallback = true` (e.g., "Other State") with the same quota/gender/studentType. If the fallback also has no configuration, submission is **blocked** with: *"No fee structure configured for this combination. Please contact admin."*
5. The address `state` field (from the country/state/district selector) is automatically mapped to a `FeeState` for lookup — no separate state selector is shown.
6. The total fee is saved with the enquiry record for use during fee finalization (BR-6).
7. The backend is the source of truth; client-submitted fee totals are not trusted.

### Fee Dimensions

| Dimension | Source on Enquiry Form | Default |
|-----------|------------------------|---------|
| **Quota** | Admission Quota dropdown (Management / Counselling) | Management |
| **State** | Auto-derived from address State field | Tamil Nadu (or fallback) |
| **Gender** | Gender field | Female |
| **Student Type** | Student Type toggle | Day Scholar |

Each fee structure group is configured for a specific combination. HOSTEL_FEE and TRANSPORT_FEE are separate fee type line items within the group configured for that student type — no post-lookup filtering is applied.

### Submission Blocking Rule (BR-30)

- If the fee total is `0` or no fee structure is found after fallback, the **Create / Update Enquiry button is disabled** and an error is shown.
- This is a hard block — enquiry cannot be submitted without a valid fee configuration.

### Screen Layout

```
┌─────────────────────────────────┬──────────────────────────────┐
│ Enquiry Form                    │ Fee Structure                 │
│                                 │                              │
│ Name: [___________]             │ Program: B.Sc Nursing        │
│ Phone: [__________]             │ Duration: 4 Years            │
│ Email: [__________]             │                              │
│ Program: [▼ Select Program]     │ Total Course Fee             │
│ Course: [▼ Select Course]       │ (Hosteler)                   │
│ Student Type: [▼ Hosteler]      │ ₹4,20,000                   │
│ Total Fee: ₹4,20,000 (read-only)│                              │
│ Referral Type: [▼ Select]       │                              │
│ Agent: [▼ Select] (if agent)    │                              │
│ Remarks: [___________]          │                              │
│ [Save] [Cancel]                 │                              │
└─────────────────────────────────┴──────────────────────────────┘
```

### Permissions

- `ENQUIRY_VIEW` / `ENQUIRY_CREATE` / `ENQUIRY_EDIT` — can access enquiry form with fee guideline

---

## BR-4: Referral Type Master

### Business Rule

Referral types must be managed as a **separate master entity** where administrators can add, edit, activate, and deactivate referral types. This replaces the fixed `EnquirySource` enum approach for referral categorization. The `source` field on enquiries has been removed; the `referralType` FK is now the **sole field** for tracking how an enquiry was referred.

### Key Points

1. The referral type master is a standalone CRUD entity (not an enum).
2. Default referral types to be seeded: `WALK_IN`, `PHONE`, `ONLINE`, `AGENT_REFERRAL`, `STAFF`, `ALUMNI`, `PARENT`, `ADVERTISEMENT`.
3. Each referral type has:
   - `hasCommission` (Boolean) — indicates whether this referral type incurs a commission.
   - `commissionAmount` (BigDecimal) — the commission amount when `hasCommission` is true.
4. A `hasCommission` value of `false` means no additional fee impact.
5. When `hasCommission` is `true`, the `commissionAmount` is pre-filled on the enquiry form as the referral additional amount (editable by the user).
6. Referral types can be activated/deactivated (soft delete). Only active types appear in the enquiry form dropdown.
7. The enquiry form's referral type field is **required** and fetches data from this master table.
8. When the selected referral type code is `AGENT_REFERRAL`, an Agent dropdown appears for selecting the referring agent.

### Data Model

```
ReferralType:
  - id (PK)
  - name (VARCHAR, unique, e.g., "Staff", "Agent Referral")
  - code (VARCHAR, unique, e.g., "STAFF", "AGENT_REFERRAL")
  - hasCommission (BOOLEAN, default false)
  - commissionAmount (DECIMAL/BigDecimal, default 0)
  - description (TEXT, optional)
  - isActive (BOOLEAN, default true)
  - createdAt, updatedAt (audit)
```

### Endpoints

- `GET /api/v1/referral-types` — List all (filter by isActive)
- `GET /api/v1/referral-types/{id}` — Get by ID
- `POST /api/v1/referral-types` — Create (`REFERRAL_TYPE_MANAGE`)
- `PUT /api/v1/referral-types/{id}` — Update (`REFERRAL_TYPE_MANAGE`)
- `DELETE /api/v1/referral-types/{id}` — Soft delete / deactivate (`REFERRAL_TYPE_MANAGE`)

### Permissions

- `REFERRAL_TYPE_MANAGE` — full CRUD on referral types

---

## BR-5: Referral Commission Tracking & Student Fee Separation

### Business Rule

When a referral type is selected on the enquiry screen, if the referral type has `hasCommission=true`, the system must resolve and track the applicable **commission amount** for internal payout reporting. Referral commission is **not** part of the student's fee and must never increase `feeGuidelineTotal`, `finalCalculatedFee`, fee finalization totals, payment dues, receipts, or student fee allocations.

### Key Points

1. If the selected referral type's `hasCommission` is **false**, commission is recorded as ₹0 and `commissionPaymentStatus = NOT_APPLICABLE`.
2. If `hasCommission` is **true**, the system resolves the commission server-side:
   - If the selected agent has a positive `commissionAmount`, use the agent amount and set `commissionSource = AGENT`.
   - Otherwise use the referral type commission amount and set `commissionSource = REFERRAL_TYPE`.
3. The **student fee** = Current-year fee structure total after student-type filtering (BR-3, BR-12, BR-23). It is never increased by commission.
4. Commission amount is stored separately as `commissionAmount` with `commissionSource` and `commissionPaymentStatus` for referral payout workflows.
5. Client-submitted `referralAdditionalAmount` and `finalCalculatedFee` values are not trusted by the backend for student fee calculation.

### Calculation Formula

```
Student Fee = Current-Year Fee Structure Total (after student-type filtering)
Commission Payable = Agent override commission OR Referral Type commission OR ₹0
```

### Example

| Scenario | Current-Year Student Fee | Referral Type | Commission Amount | Student Fee Saved | Commission Tracked |
|----------|--------------------------|---------------|-------------------|-------------------|--------------------|
| Walk-in enquiry | ₹4,00,000 | Walk-In | ₹0 | ₹4,00,000 | ₹0 |
| Agent referral | ₹4,00,000 | Agent Referral | ₹15,000 | ₹4,00,000 | ₹15,000 |
| Staff referral | ₹4,00,000 | Staff | ₹0 | ₹4,00,000 | ₹0 |

---

## BR-6: Admin Fee Finalization Workflow

### Business Rule

The enquiry screen is used by the **front office** to capture initial data. Once the enquiry is submitted and the student expresses interest in joining (status = INTERESTED), the **admin** reviews and finalizes the fee structure on the **Fee Finalization Screen**. The guideline values from the enquiry are pre-populated; the admin may only apply a **discount** — fees cannot be increased above the guideline.

### Key Points

1. The front office submits the enquiry with: student details, program + course selection, referral type, student type, and location/person referral details.
2. The backend recalculates and saves fee guideline values from the current active academic year's fee structure; browser-submitted fee totals are treated as display-only hints and must not be trusted.
3. The **Fee Finalization Screen** shows a list of enquiries in **INTERESTED** status.
4. The admin selects an enquiry to finalize, and the form is pre-populated with:
   - Total fee from enquiry's `finalCalculatedFee` (or `feeGuidelineTotal`) — **read-only, not editable**
   - **BR-30 update**: All 4 fee dimensions are shown as read-only context: Quota, State (fee state name), Gender, Student Type
   - Commission info separately for referral payout visibility only — it does not increase the student fee
5. The admin can:
   - **Provide a discount** (reduce the fee) by entering a discount amount and reason
   - **Cannot increase** the fee above the pre-loaded total
   - The discount amount must not exceed the total fee (validated)
6. The admin's finalized values are saved separately (not overwriting the original enquiry values) for audit purposes.
7. Upon finalization, the enquiry status automatically transitions to **FEES_FINALIZED**.
8. The `assignedTo` field is **not used** in this workflow.

### Data to Capture in Enquiry

| Field | Source | Description |
|-------|--------|-------------|
| `feeGuidelineTotal` | Backend Fee Structure Calculation | Total fee from current active academic year fee structure guideline |
| `feeGuidelineYearWise` | Fee Structure | JSON/related records of year-wise breakdown |
| `referralTypeId` | Referral Type Master | Selected referral type |
| `commissionAmount` | Referral Type / Agent | Internal payout amount; not part of student fee |
| `finalCalculatedFee` | Backend Computed | Same as feeGuidelineTotal after student-type filtering; commission excluded |
| `studentType` | Front office input | DAY_SCHOLAR or HOSTELER |

### Data to Capture in Finalization

| Field | Source | Description |
|-------|--------|-------------|
| `finalizedTotalFee` | Pre-populated from enquiry | Admin's confirmed fee (read-only — cannot be increased) |
| `discountAmount` | Admin input | Discount applied (if any, must not exceed total fee) |
| `discountReason` | Admin input | Reason for discount |
| `netFee` | Computed | finalizedTotalFee - discountAmount |
| `yearWiseFees` | Admin input | Year-wise distribution of net fee |
| `finalizedBy` | System | Admin who finalized |
| `finalizedAt` | System | Timestamp of finalization |

### Permissions

- `FEE_FINALIZE` — can finalize fees (exclusive; finalization is a management action)

---

## BR-7: Payment Collection by Accounting Team

### Business Rule

After admin finalization (BR-6), the finalized fee data is presented to the **accounting team / cashier** for payment collection on the **Payment Collection Screen**. The screen lists enquiries in **FEES_FINALIZED** status. The student may pay the full amount or partial amounts using various payment modes.
Once fees are finalized, balance collection is allowed at any pre-admission stage, including DOCUMENTS_SUBMITTED and DOCUMENTS_VERIFIED, except for NOT_INTERESTED, CLOSED, CANCELLED, ADMITTED, and converted enquiries. Payments collected after document submission/verification preserve the current enquiry status and do not move it backward. Admitted students use the student fee collection flow.

### Key Points

1. The **Payment Collection Screen** shows a list of enquiries in **FEES_FINALIZED** status.
2. The cashier selects an enquiry and sees the finalized fee breakdown: total fee, discount, net fee, year-wise amounts.
3. The cashier enters payment details: amount, payment date, payment mode, transaction reference, and remarks.
4. Payment can be collected in **full** or in **parts** (partial/advance).
5. Upon full payment, the enquiry status transitions to **FEES_PAID**.
6. Upon partial payment, the enquiry status transitions to **PARTIALLY_PAID**.
7. Payments are currently tracked against the **enquiry** record. Student record creation happens at explicit conversion (BR-10).
8. Supported payment modes: `CASH`, `CARD`, `UPI`, `BANK_TRANSFER`, `CHEQUE`.

### Payment Status Tracking

| Status | Condition |
|--------|-----------|
| **PAID** | Full amount collected |
| **PARTIALLY_PAID** | Partial amount collected |
| **PENDING** | No payment yet, not overdue |
| **OVERDUE** | Past due date with pending balance |

### Permissions

- `FEE_COLLECT` — can collect fee payments from enquiry prospects
- `STUDENT_FEE_VIEW` — can view student fee/payment data

---

## BR-8: Enquiry Status Workflow

### Business Rule

The enquiry status transitions automatically based on actions taken in the system. The status workflow reflects the enquiry's lifecycle from initial contact through admission.

### Status Definitions

| Status | Description | Triggered By |
|--------|-------------|-------------|
| **ENQUIRED** | Initial enquiry created by front office | Creating a new enquiry |
| **INTERESTED** | Student has shown interest in joining | Front office updates after follow-up |
| **NOT_INTERESTED** | Student is not interested | Front office updates after follow-up |
| **FEES_FINALIZED** | Admin has finalized the fee structure | Admin completes fee finalization (BR-6) |
| **FEES_PAID** | Full fees have been paid | Full payment collected (BR-7) |
| **PARTIALLY_PAID** | Partial fees have been paid | Partial payment collected (BR-7) |
| **DOCUMENTS_SUBMITTED** | Student has submitted/uploaded required documents and is pending verification | Documents uploaded/submitted (BR-9) |
| **DOCUMENTS_VERIFIED** | All mandatory submitted documents have been verified | Document verification completed (BR-9) |
| **ADMITTED** | Enquiry has been converted to an admission and student record | Complete Admission (BR-10) |
| **CLOSED** | Enquiry closed without conversion | Admin manually closes |

Once fees are finalized, balance collection is allowed at any pre-admission stage, including DOCUMENTS_SUBMITTED and DOCUMENTS_VERIFIED, except for NOT_INTERESTED, CLOSED, CANCELLED, ADMITTED, and converted enquiries. Payments collected after document submission/verification preserve the current enquiry status and do not move it backward. Admitted students use the student fee collection flow.

### Status Transition Diagram

```
ENQUIRED
  ├── → INTERESTED → FEES_FINALIZED
  │                        ├── → FEES_PAID → DOCUMENTS_SUBMITTED → DOCUMENTS_VERIFIED → ADMITTED
  │                        ├── → PARTIALLY_PAID → DOCUMENTS_SUBMITTED → DOCUMENTS_VERIFIED → ADMITTED
  │                        └── (admin can edit finalized fees at any time before ADMITTED)
  │
  ├── → NOT_INTERESTED → CLOSED
  │
  └── → CLOSED
```

### Automatic Status Transitions

| Action | From Status | To Status |
|--------|-------------|-----------|
| Create enquiry | — | ENQUIRED |
| Mark as interested | ENQUIRED | INTERESTED |
| Mark as not interested | ENQUIRED | NOT_INTERESTED |
| Admin finalizes fees | INTERESTED | FEES_FINALIZED |
| Full payment collected | FEES_FINALIZED / PARTIALLY_PAID | FEES_PAID |
| Partial payment collected | FEES_FINALIZED | PARTIALLY_PAID |
| Documents submitted | FEES_PAID / PARTIALLY_PAID | DOCUMENTS_SUBMITTED |
| All mandatory documents verified | DOCUMENTS_SUBMITTED | DOCUMENTS_VERIFIED |
| Complete admission | DOCUMENTS_VERIFIED | ADMITTED |
| Close enquiry | Any (except ADMITTED) | CLOSED |

### Note on Previous Statuses

The previous status values (`NEW`, `CONTACTED`, `FEE_DISCUSSED`) are being replaced by this enhanced workflow. The new statuses better reflect the actual business process from enquiry through admission.

---

## BR-9: Submit Documents

### Business Rule

After fees are paid (fully or partially), the student must submit required documents (e.g., 10th certificate, 12th certificate, ID proofs). Once all mandatory documents are submitted/uploaded, the enquiry status transitions to **DOCUMENTS_SUBMITTED** and enters the document verification queue. Once all mandatory documents are verified, the enquiry status transitions to **DOCUMENTS_VERIFIED**.

### Key Points

1. Required documents vary by program but typically include:
   - 10th (SSLC) mark sheet / certificate
   - 12th (HSC) mark sheet / certificate
   - Transfer Certificate (TC)
   - Migration Certificate
   - Community Certificate (if applicable)
   - Aadhar Card
   - Passport-size photographs
   - Income Certificate (for scholarship eligibility)
2. Each document has a status such as `NOT_UPLOADED`, `UPLOADED`, `VERIFIED`, or `REJECTED`.
3. The system tracks which documents are submitted and which are pending.
4. The enquiry status transitions to **DOCUMENTS_SUBMITTED** when all mandatory documents are submitted/uploaded.
5. Document verification can be done by authorized staff/admin.
6. The enquiry status transitions to **DOCUMENTS_VERIFIED** only when all mandatory documents are verified.

### Entities Involved

- `EnquiryDocument` (new or linked to existing `AdmissionDocument`) — tracks submit documents per enquiry
- Existing `AdmissionDocument` entity already supports document types and verification workflow

### Permissions

- `DOCUMENT_SUBMISSION_VIEW` — can view document submission status
- `DOCUMENT_SUBMISSION_MANAGE` — can record and verify submitted documents

---

## BR-10: Convert Enquiry to Student

### Business Rule

Once the enquiry reaches **DOCUMENTS_VERIFIED** status, the system provides the option to **complete admission**. This creates a full student entity and admission record in the system with all data captured during the enquiry process.

### Key Points

1. Complete Admission is only allowed from **DOCUMENTS_VERIFIED** status.
2. The student record is created with:
   - Personal details from the enquiry (name, email, phone)
   - Program from the enquiry
   - Admission date set to the conversion date
   - Fee allocation linked to the finalized fee data
   - Documents linked from the enquiry
3. Upon completion, the enquiry status transitions to **ADMITTED**.
4. The enquiry retains a reference to the created student (`convertedStudentId`).
5. The conversion is **irreversible** — once converted, the enquiry cannot be reverted.
6. A roll number is generated for the student based on the institution's numbering scheme.
7. Admission form view, print, and download outputs must use the same printable template. The official printable admission form is optimized for A4 portrait output, excludes the Academic Qualifications section, and must keep text readable in print preview. The document checklist is displayed in two balanced columns: up to 20 documents render as 10 rows, 23 documents render as 12 rows, 31 documents render as 16 rows, and so on using `ceil(document count / 2)` rows. The passport photo submitted as the `PASSPORT_PHOTO` admission document must be shown in the photo box when available. The download action must produce the same printable output as view/print, not a separate layout.
8. Admission document checklists are based on the student's program's current required-document mapping. If a required document is added to a program after admission creation, existing admissions remain valid and the new document appears as `NOT_UPLOADED` so staff can upload it from the admission/student documents screen. If a document type is removed from the program requirements after it was already collected, the uploaded document and verification history are preserved and shown as a collected document that is no longer currently required; it is not deleted and is not counted as missing.

### Permissions

- `ADMISSION_CREATE` / `ADMISSION_EDIT` — can convert enquiry to student

---

## BR-11: Student Explorer with Filters

### Business Rule

All students created through the enquiry-to-admission process (and other admission channels) must be available in a **Student Explorer** screen with comprehensive filtering capabilities.

### Key Points

1. The Student Explorer provides a searchable, filterable list of all students.
2. Available filters:
   - **Program** — filter by enrolled program
   - **Speciality** — filter by speciality
   - **Academic Year** — filter by admission year
   - **Semester/Year** — filter by current semester or year
   - **Status** — filter by student status (ACTIVE, ON_LEAVE, SUSPENDED, etc.)
   - **Fee Status** — filter by fee payment status (PAID, PARTIALLY_PAID, OVERDUE)
   - **Search** — free-text search by name, roll number, email, or phone
3. The explorer supports pagination and sorting.
4. Each student row shows: roll number, name, program, semester, fee status, student status.
5. Clicking a student navigates to their detailed profile.

### Permissions

- `STUDENT_VIEW` — can access student explorer
- `STUDENT_EDIT` — can maintain student records where assigned

---

## BR-12: Student Type on Enquiry

> **⚠️ Amended by BR-30.** Student type is no longer the sole fee dimension — it is one of four (quota, state, gender, studentType). The post-lookup filtering by fee type (HOSTEL_FEE / TRANSPORT_FEE) described below is superseded. Fee lookup now uses a dedicated group per studentType; filtering happens at configuration time, not at lookup time.

### Business Rule

Every enquiry must capture the **student type** — whether the student will be a **Day Scholar** or a **Hosteler**. Under BR-30, student type is used as one of four dimensions to select the correct fee structure group at enquiry creation. The group itself contains the appropriate fee types for that student type.

### Key Points

1. Student type is an optional field on the enquiry form: `DAY_SCHOLAR` or `HOSTELER`.
2. When **Day Scholar** is selected, the total fee shown **excludes HOSTEL_FEE** and **includes TRANSPORT_FEE**.
3. When **Hosteler** is selected, the total fee shown **excludes TRANSPORT_FEE** and **includes HOSTEL_FEE**.
4. When not specified, the total fee includes all fee types.
5. The student type is saved with the enquiry record and shown in the enquiry list.
6. Rules for fee type inclusion:

| Fee Type | Day Scholar | Hosteler | Not Specified |
|---|---|---|---|
| TUITION | ✓ | ✓ | ✓ |
| LAB_FEE | ✓ | ✓ | ✓ |
| LIBRARY_FEE | ✓ | ✓ | ✓ |
| EXAMINATION_FEE | ✓ | ✓ | ✓ |
| HOSTEL_FEE | ✗ | ✓ | ✓ |
| TRANSPORT_FEE | ✓ | ✗ | ✓ |
| MISCELLANEOUS | ✓ | ✓ | ✓ |
| LATE_FEE | ✓ | ✓ | ✓ |

### Entities Involved

- `Enquiry` — has `studentType` field (nullable, values: `DAY_SCHOLAR`, `HOSTELER`)
- `FeeStructure` — filtered by fee type based on student type

### Permissions

- `ENQUIRY_CREATE` / `ENQUIRY_EDIT` — can set/update student type on enquiry form

---

## BR-13: Semester-Wise Fee Collection

### Business Rule

Fee structures are defined **yearly** (one amount per program year), but actual payment tracking is done **semester-wise**. Each yearly fee is automatically split into two equal semesters at the point of fee finalization.

### Key Points

1. When a fee allocation is finalized for a student, the system automatically splits each year's fee into two semesters:
   - **Semester 1**: 50% of the year fee (floor-rounded), due at the year start date.
   - **Semester 2**: remaining 50% (handles odd amounts), due 6 months after Semester 1.

2. A student can pay **any amount** at any time — the system does not enforce minimum semester amounts.

3. Payment cascades across semesters in order (Semester 1 first, then Semester 2, then Year 2 Semester 1, etc.). A single payment may partially or fully cover multiple semesters in one transaction.

4. **One receipt is issued per payment** (not per semester). A single receipt number appears across all semester installment lines created by that payment, making the receipt the atomic unit of accounting.

5. The semester-wise fee status table **must always be the first section shown** when opening a student's fee payment screen, even before the page is fully loaded (skeleton state). It shows: semester label, fee amount, amount paid, outstanding, due date, and payment status.

6. Each semester displays a status: `PAID`, `PARTIAL`, or `PENDING`. A semester is also flagged as `OVERDUE` (visual indicator only) when its due date has passed and there is an outstanding balance.

7. Receipts are displayed grouped by receipt number, showing which semesters were covered in each payment transaction.

### Semester Label Format
- `Year 1 - Semester 1`, `Year 1 - Semester 2`, `Year 2 - Semester 1`, etc.

### API Endpoints
- `POST /api/v1/student-fees/finalize` — finalizes fee and creates 2 semester records per year
- `GET /api/v1/student-fees/{studentId}/semester-status` — returns semester-wise status (always shown first in UI)
- `POST /api/v1/student-fees/{studentId}/collect` — collects any amount, cascades across semesters, returns ONE receipt with semester breakdown
- `GET /api/v1/student-fees/{studentId}/receipts` — returns all installments; UI groups by receipt number

---

---

## BR-14: Scholarship Type Master

### Business Rule

Scholarship types are managed as a **master entity** allowing administrators to configure both institution-funded and government-portal scholarship schemes. Each type defines the discount calculation method, eligibility year constraints, renewal requirements, and — for government schemes — the portal name, URL, and scheme code.

### Key Points

1. Each scholarship type has a unique `code` (e.g. `MERIT`, `SC_GOVT`, `EWS`, `FIRST_GRAD`).
2. **Application Mode** determines how the scholarship is processed:
   - `INSTITUTION` — the college reviews and approves directly inside the CMS.
   - `GOVT_PORTAL` — the actual application is submitted through an external government portal (NSP, ePass Tamil Nadu, TNSMS, TNSCST). The CMS only tracks the status; the college acts as a forwarding institution.
3. **Discount Type** controls how the scholarship amount is calculated:

   | Discount Type | Calculation |
   |---|---|
   | `PERCENTAGE` | `discountValue` % of the student's total fee (capped at `maxAmountPerYear` if set) |
   | `FIXED_AMOUNT` | A fixed rupee amount (capped at `maxAmountPerYear` if set) |
   | `FULL_WAIVER` | 100 % of the total fee (capped at `maxAmountPerYear` if set) |

4. `eligibleFromYear` / `eligibleToYear` restrict eligibility by program year: year 1 = semester 1–2, year 2 = semester 3–4, etc. Null means no restriction.
5. `renewalRequired = true` means the scholarship must be renewed each academic year via an explicit renewal action.
6. Only active scholarship types appear in student-facing application dropdowns.
7. Default seeded types: `MERIT`, `SC_GOVT`, `ST_GOVT`, `OBC_GOVT`, `BC_STATE`, `EWS`, `FIRST_GRAD`, `SPORTS`.

### API Endpoints

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/scholarships` | SCHOLARSHIP_VIEW | List all active scholarship types |
| GET | `/api/v1/scholarships/{id}` | SCHOLARSHIP_VIEW | Get by ID |
| POST | `/api/v1/scholarships` | SCHOLARSHIP_MANAGE | Create new scholarship type |
| PUT | `/api/v1/scholarships/{id}` | SCHOLARSHIP_MANAGE | Update scholarship type |
| DELETE | `/api/v1/scholarships/{id}` | SCHOLARSHIP_MANAGE | Deactivate (soft delete) |

### Permissions

- `SCHOLARSHIP_VIEW` — list and view scholarship types
- `SCHOLARSHIP_MANAGE` — create, update, and deactivate scholarship types

---

## BR-15: Student Scholarship Eligibility Profile

### Business Rule

Each student has exactly **one** scholarship eligibility profile (`student_scholarship_eligibility`). This profile stores the criteria required to determine which government and institutional scholarships the student qualifies for. It also holds DBT (Direct Benefit Transfer) banking details required by government portal schemes.

### Eligibility Criteria

| Field | Purpose |
|---|---|
| `isFirstGraduate` | Student is the first person in their family to attend college — qualifies for FIRST_GRAD scholarship |
| `isMeritBased` | Flagged by admin based on academic performance |
| `isSportsQuota` | Admitted under sports quota |
| `isEconomicallyWeaker` | Auto-set to true if `annualFamilyIncome < ₹3,00,000` (EWS income limit) |
| `annualFamilyIncome` | Annual family income in rupees (used for EWS determination) |
| Income Certificate | Number, issuing authority, issue date |
| Community Certificate | Number, issuing authority, issue date |
| First-Graduate Certificate | Number, issuing authority, issue date |
| `fatherEducation` / `motherEducation` | Highest educational qualification of parents |

### EWS Income Limit

A student is automatically flagged as **Economically Weaker Section (EWS)** when `annualFamilyIncome < ₹3,00,000`. This threshold is defined as `StudentScholarshipEligibilityService.EWS_INCOME_LIMIT`.

### DBT (Direct Benefit Transfer) Details

Government scholarship schemes (NSP, ePass TN) credit money directly to the student's Aadhaar-linked bank account. The following fields must be captured before a govt-portal scholarship can be sanctioned:

| Field | Description |
|---|---|
| `aadhaarNumber` | 12-digit Aadhaar number (stored as digits; displayed masked: `XXXXXXXX9012`) |
| `bankAccountNumber` | Bank account number |
| `bankIfsc` | IFSC code |
| `bankName` | Name of the bank |
| `bankBranch` | Branch name |
| `dbtLinked` | `true` when the account has been seeded with Aadhaar for DBT credit |

### Verification

Users with `SCHOLARSHIP_APPROVE` can **verify** the eligibility profile, setting `verifiedBy`, `verifiedAt`, and `verificationRemarks`. Verification is a prerequisite for government portal scholarship applications.

### API Endpoints

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/students/{id}/eligibility` | SCHOLARSHIP_VIEW | Get eligibility profile |
| PUT | `/api/v1/students/{id}/eligibility` | SCHOLARSHIP_MANAGE | Update eligibility profile |
| PUT | `/api/v1/students/{id}/eligibility/verify` | SCHOLARSHIP_APPROVE | Verify/sign off the profile |

### Permissions

- `SCHOLARSHIP_MANAGE` — can update eligibility profiles
- `SCHOLARSHIP_APPROVE` — can verify eligibility profiles

---

## BR-16: Scholarship Application Lifecycle

### Business Rule

A student can apply for one or more scholarships per academic year. Each application goes through a defined lifecycle from submission to approval/rejection and, for government schemes, government sanction. A student may not have more than one application for the same academic year.

### Application Status Transitions

```
PENDING
  ├── → APPROVED  (by college admin)
  │       ├── → SANCTIONED (govt-portal only — govt sanction number recorded)
  │       └── → CANCELLED  (before disbursement)
  ├── → ON_HOLD   (pending document re-submission)
  │       └── → APPROVED / REJECTED
  └── → REJECTED  (with mandatory reason)
        └── → (terminal)
```

| Status | Triggered By |
|---|---|
| `PENDING` | Student / staff submits application |
| `APPROVED` | College admin approves (sets approvedAmount, disbursementFrequency, validFrom/Till) |
| `SANCTIONED` | For GOVT_PORTAL types only — govt sanction number and date recorded; money will be credited via DBT |
| `REJECTED` | Admin rejects with a mandatory reason |
| `ON_HOLD` | Admin places on hold (e.g. documents missing) |
| `CANCELLED` | Application withdrawn before approval |

### Transition Rules

1. Only `PENDING` or `ON_HOLD` applications can be **Approved**.
2. Only `APPROVED` applications can be **Sanctioned** (and only for `GOVT_PORTAL` scholarship types).
3. Attempting to sanction an `INSTITUTION`-mode scholarship raises a 409 error.
4. `APPROVED` applications **cannot** be rejected or cancelled (prevents data loss after approval).
5. A student can have only **one** scholarship application per academic year — duplicates are rejected with HTTP 400.
6. Year-of-study restriction: if the scholarship type has `eligibleFromYear`/`eligibleToYear` set, the student's current semester must map to an eligible program year.

### Scholarship Amount Calculation

```
PERCENTAGE   →  totalFee × (discountValue / 100)  [capped at maxAmountPerYear]
FIXED_AMOUNT →  discountValue                      [capped at maxAmountPerYear]
FULL_WAIVER  →  totalFee                           [capped at maxAmountPerYear]
```

### Renewal

Approved scholarships marked `renewalRequired = true` can be renewed with a single action. Renewal creates a new `PENDING` application for the **next academic year**, linking back to the source via `renewedFrom`.

### Eligible Scholarship Determination

When the system computes which scholarships a student qualifies for, it checks:
- The student's `communityCategory` field against caste-based government schemes (SC/ST/OBC/BC/MBC).
- The student's `isFirstGraduate` flag or the eligibility profile's `isFirstGraduate` for the FIRST_GRAD scheme.
- The eligibility profile's `annualFamilyIncome` against the EWS limit for the EWS scheme.
- The eligibility profile's `isMeritBased` flag for merit-based scholarships.
- The scholarship type's year-of-study range against the student's current semester.

### API Endpoints

| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/students/{id}/scholarships/eligible` | SCHOLARSHIP_VIEW | List scholarships the student qualifies for |
| GET | `/api/v1/students/{id}/scholarships` | SCHOLARSHIP_VIEW | List student's applications |
| POST | `/api/v1/students/{id}/scholarships/apply` | SCHOLARSHIP_APPLY | Submit a new application |
| GET | `/api/v1/scholarship-applications` | SCHOLARSHIP_APPROVE | List all pending applications |
| PUT | `/api/v1/scholarship-applications/{id}/approve` | SCHOLARSHIP_APPROVE | Approve an application |
| PUT | `/api/v1/scholarship-applications/{id}/reject` | SCHOLARSHIP_APPROVE | Reject an application |
| PUT | `/api/v1/scholarship-applications/{id}/cancel` | SCHOLARSHIP_MANAGE | Cancel an application |
| PUT | `/api/v1/scholarship-applications/{id}/sanction` | SCHOLARSHIP_APPROVE | Record govt sanction (GOVT_PORTAL only) |
| POST | `/api/v1/scholarship-applications/{id}/renew` | SCHOLARSHIP_APPLY | Renew approved scholarship for next year |

### Permissions

- `SCHOLARSHIP_APPROVE` — can approve, reject, sanction, and view pending applications
- `SCHOLARSHIP_APPLY` — can submit or renew applications
- `SCHOLARSHIP_VIEW` — can view scholarship applications

---

## BR-17: Scholarship Disbursement

### Business Rule

For **institution-funded** scholarships (INSTITUTION mode), after approval the college may disburse the scholarship amount as a fee waiver, direct credit, or cheque. Each disbursement event is recorded separately and linked to the scholarship application and academic year.

For **government portal** scholarships (GOVT_PORTAL mode), the government credits money directly to the student's Aadhaar-linked bank account via DBT — the college does not manually disburse these. Only a `SANCTIONED` status and correctly captured DBT details are required.

### Disbursement Modes

| Mode | Description |
|---|---|
| `DIRECT_CREDIT` | Amount transferred directly to student's bank account (or applied to fee ledger) |
| `FEE_WAIVER` | Amount deducted from the student's outstanding fee balance in the CMS |
| `CHEQUE` | Physical cheque issued; cheque number must be recorded |

### Key Points

1. Disbursements can only be recorded against **APPROVED** or **SANCTIONED** scholarship applications.
2. Each disbursement records: amount, date, mode, transaction reference (DIRECT_CREDIT), cheque number (CHEQUE), bank name, remarks, and the staff member who recorded it.
3. `semesterNumber` is optional — used when the disbursement is tied to a specific semester.
4. The full disbursement history is visible on the student's scholarship screen and on the application detail.

### API Endpoints

| Method | Path | Permission | Description |
|---|---|---|---|
| POST | `/api/v1/scholarship-applications/{id}/disburse` | SCHOLARSHIP_DISBURSE | Record a disbursement |
| GET | `/api/v1/scholarship-applications/{id}/disbursements` | SCHOLARSHIP_VIEW | Get disbursements for an application |
| GET | `/api/v1/students/{id}/scholarships/disbursements` | SCHOLARSHIP_VIEW | All disbursement history for a student |

### Permissions

- `SCHOLARSHIP_DISBURSE` — can record disbursements and fee-waiver disbursements
- `SCHOLARSHIP_VIEW` — can view scholarship disbursements

---

## BR-18: Enquiry Location Fields

### Business Rule

To improve geographic analytics and to support government admission reports, every enquiry can optionally capture the student's **home location**: country, state, and district.

### Key Points

1. `country` and `state` are **required** fields on the enquiry form (updated by BR-22).
2. `district` remains optional.
3. `country` defaults to **India** and `state` defaults to **Tamil Nadu** when a new enquiry is opened.
4. The fields are available as filters on the enquiry list screen and appear in enquiry exports.
5. Location data is carried forward when an enquiry is converted to a student record.

### Data Model

New columns on the `enquiries` table:

| Column | Type | Nullable |
|---|---|---|
| `country` | VARCHAR | Yes |
| `state` | VARCHAR | Yes |
| `district` | VARCHAR | Yes |

### Permissions

- `ENQUIRY_CREATE` / `ENQUIRY_EDIT` — can set and update location fields on enquiry

---

## BR-19: Transaction Reference Mandatory for Electronic Payments

### Business Rule

When collecting fees through any electronic payment channel (UPI, bank transfer, or cheque), a **transaction reference number** is mandatory. CASH and CARD payments do not require a transaction reference.

### Affected Payment DTOs

Enforced via the `@TransactionReferenceRequired` custom Bean Validation annotation on:

| DTO | Endpoint |
|---|---|
| `EnquiryPaymentRequest` | `POST /api/v1/enquiries/{id}/payments` |
| `TermFeePaymentRequest` | `POST /api/v1/student-fees/{id}/collect` |
| `CollectPaymentRequest` | Finance — collect payment dialog |

### Required by Payment Mode

| Payment Mode | Transaction Reference Required |
|---|---|
| `CASH` | ❌ Not required |
| `CARD` | ❌ Not required |
| `UPI` | ✅ Mandatory |
| `BANK_TRANSFER` | ✅ Mandatory |
| `CHEQUE` | ✅ Mandatory (cheque number used as reference) |

### Validation Behaviour

- If `transactionReference` is null or blank when mode is UPI/BANK_TRANSFER/CHEQUE, the API returns HTTP 400 with a descriptive validation message.
- The front-end enforces this rule at form-submission time with an inline error.

---

## BR-20: Fee Type Classification Expansion

### Business Rule

The `FeeType` enum was expanded to support paramedical and vocational programs that require additional specialized fee components.

### Current Fee Types

| Fee Type | Category | Notes |
|---|---|---|
| `TUITION` | Generic | Core academic fee |
| `LABORATORY_FEE` | Generic | Renamed from `LAB_FEE` |
| `LIBRARY_FEE` | Generic | |
| `EXAMINATION_FEE` | Generic | |
| `CLINICAL_FEE` | Generic | **New** — nursing / paramedical / medical programs |
| `BOOK_AND_PACKET_FEE` | Generic | **New** — books, stationery, study materials |
| `UNIFORM_AND_SHOES_FEE` | Generic | **New** — uniform kit (nursing, polytechnic, etc.) |
| `UNIVERSITY_REGISTRATION_FEE` | Generic | **New** — university affiliation / registration charges |
| `MISCELLANEOUS` | Generic | |
| `LATE_FEE` | Generic | Penalty for late payment |
| `HOSTEL_FEE` | Additional | Excluded from day-scholar totals |
| `TRANSPORT_FEE` | Additional | Excluded from hosteler totals |

### Key Points

1. All Generic fee types contribute to the **Course Total** subject to student-type rules (BR-1, BR-12).
2. `HOSTEL_FEE` and `TRANSPORT_FEE` remain Additional — excluded from the course total and displayed separately.
3. `LAB_FEE` has been **renamed to `LABORATORY_FEE`** — any existing stored data must be migrated via Flyway (see `V99__rename_lab_fee_add_new_fee_types.sql`).

---

## BR-21: Student First-Graduate & Parent Education Tracking

### Business Rule

To support first-graduate scholarship determination and government reports, a student record may optionally capture whether they are the **first person in their family to pursue higher education**, along with the highest educational qualification of both parents.

### Fields Added to Student Entity

| Field | Type | Description |
|---|---|---|
| `isFirstGraduate` | BOOLEAN (default false) | True when the student is the first in their family to attend college |
| `fatherEducation` | VARCHAR | Father's highest educational qualification (e.g. "Class 8", "Graduate") |
| `motherEducation` | VARCHAR | Mother's highest educational qualification |

### Relationship with Eligibility Profile

- `Student.isFirstGraduate` is a **quick-access copy** used directly in scholarship eligibility checks.
- The detailed eligibility profile (`StudentScholarshipEligibility`) also holds `isFirstGraduate` plus supporting certificate details.
- When the eligibility profile is updated via the eligibility API, the parent `Student.isFirstGraduate` field is **also updated automatically**.

### Permissions

- `STUDENT_CREATE` / `STUDENT_EDIT` — can set these fields during student creation or edit

---

## BR-22: Enquiry Form — Mandatory Fields, Location Defaults & Referral-Linked Person Search

### Business Rule

The enquiry form must enforce mandatory data collection at the point of entry and must provide a contextual person-search dropdown whenever the referral source maps to a specific person in the system.

### Mandatory Fields

The following fields are **required** before an enquiry can be saved:

| Field | Validation |
|---|---|
| Full Name | Required, max 255 chars |
| Phone | Required |
| Country | Required — default pre-filled as **India** |
| State | Required — default pre-filled as **Tamil Nadu** |
| Program | Required |
| Course | Required **when** courses exist for the selected program |
| Enquiry Date | Required |
| Referral Source | Required |
| Student Type | Required (Day Scholar / Hosteler) |

### Location Defaults

When the form opens for a **new** enquiry:
- `Country` is pre-filled with `India`
- `State` is pre-filled with `Tamil Nadu`

The user can change these at any time before saving.

### Referral-Linked Person Search

When the front office selects a referral type, the form shows a contextual search field to identify the specific person who referred the student:

| Referral Type Code | Person Table | Search Criteria |
|---|---|---|
| `AGENT_REFERRAL` | `agents` (active) | Existing dropdown (no change) |
| `ALUMNI` | `students` | Search by full name or roll number |
| `STUDENT` | `students` | Search by full name or roll number |
| `FACULTY` | `faculty` | Search by full name or employee code |
| All other codes | — | No person search shown |

Selecting a person from the dropdown stores their ID on the enquiry (`referred_student_id` or `referred_faculty_id`). Only one will be populated per enquiry. The person's name is resolved and returned in the response for display.

### New Referral Type Seeds

Two new referral types added to the master:

| Name | Code | Default Commission | System-Defined |
|---|---|---|---|
| Student Referral | `STUDENT` | ₹500 | No |
| Faculty Referral | `FACULTY` | ₹500 | No |

### Data Model

New columns on `enquiries` table:

| Column | Type | References | Description |
|---|---|---|---|
| `referred_student_id` | BIGINT | `students(id)` ON DELETE SET NULL | Populated when referral is ALUMNI or STUDENT |
| `referred_faculty_id` | BIGINT | `faculty(id)` ON DELETE SET NULL | Populated when referral is FACULTY |

New fields on `EnquiryResponse`:

| Field | Source |
|---|---|
| `referredStudentId` | `enquiries.referred_student_id` |
| `referredStudentName` | Resolved from `students.first_name + last_name` |
| `referredFacultyId` | `enquiries.referred_faculty_id` |
| `referredFacultyName` | Resolved from `faculty.first_name + last_name` |

### Course Required Behaviour

Course is **conditionally required**:
- If the selected program has one or more courses → Course field is required
- If the program has no courses configured → Course field is optional and cleared

This is enforced via a dynamic Angular validator updated after each program change.

### Permissions

- `ENQUIRY_CREATE` / `ENQUIRY_EDIT` — can create and edit enquiries with these fields

---

## BR-23: Authoritative Fee Calculation & Penny-Safe Numeric Rules

> **BR-30 Update:** Points 2 and 4 below are superseded for new enquiries. Under BR-30, fee lookup now uses a 6-field key (`programId + courseId + quota + feeStateId + gender + studentType`) against `FeeStructureGroup`. The HOSTEL_FEE / TRANSPORT_FEE filter in point 4 no longer applies — the correct group for the student's type is selected at lookup time. All other penny-safe arithmetic rules remain unchanged.

### Business Rule

All student fee amounts must be calculated from authoritative backend data using exact decimal arithmetic. The system must never overcharge or undercharge due to duplicated academic-year fee structures, referral commission, browser floating-point rounding, stale client-submitted totals, or unfiltered fee rows. Not even one paise may be introduced or lost by calculation logic.

### Root Cause Prevented

An enquiry for BSc Nursing showed ₹23,45,000 even though the configured fee structure was ₹10,00,000. This can happen when `/fee-structures?programId=&courseId=` returns fee structures across multiple academic years and the frontend sums every row. The fix is mandatory current-academic-year scoping plus backend recalculation on save.

### Authoritative Calculation Rules

1. **Current academic year only:** Enquiry fee lookup must use only active fee structures for the current active academic year.
2. **Course-specific:** When a course is selected, only fee structures for that exact `programId + courseId + currentAcademicYearId` combination are included.
3. **Active rows only:** Inactive fee rows are excluded from enquiry totals.
4. **Student type filtering:**
   - `HOSTEL_FEE` is included only for `HOSTELER`.
   - `TRANSPORT_FEE` is included only for `DAY_SCHOLAR`.
   - All other fee types are included.
5. **Referral commission excluded:** Referral/agent commission is tracked separately and must not be added to `finalCalculatedFee`, finalization total, outstanding balance, receipts, or allocations.
6. **Backend source of truth:** The backend recalculates `feeDiscussedAmount`, `finalCalculatedFee`, and `yearWiseFees` during enquiry create/update. Client-sent fee fields are not trusted.
7. **Fee finalization source of truth:** Fee finalization uses the enquiry's backend-calculated `finalCalculatedFee`; it does not trust the browser-submitted `totalFee` except as a fallback for legacy records without a saved calculated fee.
8. **Discount bounds:** Discount must be ≥ ₹0 and must not exceed the authoritative total fee.
9. **Exact decimal scale:** Backend monetary values are normalized to two decimal places. Values with more than two decimal places are rejected rather than silently rounded.
10. **Frontend paise arithmetic:** Frontend totals and proportional discount splitting must use integer paise internally, converting back to rupees only for display/API payloads.

### Regression Requirement

For BSc Nursing with current-year total ₹10,00,000, an enquiry for any student including Mani must save and display `finalCalculatedFee = ₹10,00,000` unless the current active academic year's fee structure itself changes. Historical/previous-year fee rows and referral commissions must not change this amount.

### Permissions

- `ENQUIRY_CREATE` / `ENQUIRY_EDIT` — create enquiries and view authoritative totals
- `FEE_FINALIZE` — finalize fees using the authoritative total

---

## BR-24: DB-Driven RBAC & Identity-Only Keycloak

### Business Rule

Application authorization is controlled by database role-permission mappings only. Keycloak is used for authentication and identity; Keycloak realm roles must not be used to grant application access.

### Key Points

1. Backend access checks use DB permission codes through `@perm.has('PERMISSION_CODE')`.
2. Controllers and services must not authorize by hardcoded role names such as `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`.
3. Frontend navigation, route guards, and action buttons must use DB permissions returned by `/api/v1/permissions/my`.
4. Keycloak realm exports must not define or assign application business roles through `roles.realm`, `defaultRoles`, or user `realmRoles`.
5. Immutable platform roles are limited to `DEV_ADMIN` and `SUPPORT_ADMIN`; they are seeded and cannot be edited through role management.
6. The go-live baseline contains only three DB roles: `DEV_ADMIN`, `SUPPORT_ADMIN`, and `collegeadmin`.
7. The DB role `collegeadmin` is the admission-focused operational role. It receives only the permissions needed to create/manage admission setup masters and complete enquiry → admission → fee finalization/payment workflows.
8. `collegeadmin` must not see or assign the `DEV_ADMIN` or `SUPPORT_ADMIN` platform roles.
9. Other operational roles are not required in the go-live baseline and are removed by the go-live wipe process.

---

## 🔄 Enquiry-to-Admission Lifecycle (End-to-End)

This section describes the complete lifecycle of a student from initial enquiry to admission:

```
Step 1: ENQUIRY (Front Office)
  ↓  Front office creates enquiry with student details, program selection
  ↓  System displays fee structure guideline for selected program (BR-3)
  ↓  Front office selects referral type; additional amount shown if applicable (BR-4, BR-5)
  ↓  Final calculated fee is computed and saved with enquiry
  ↓  Status: ENQUIRED

Step 2: FOLLOW-UP (Front Office)
  ↓  Front office follows up with the student
  ↓  If interested → Status: INTERESTED
  ↓  If not interested → Status: NOT_INTERESTED → CLOSED

Step 3: FEE FINALIZATION (Admin)
  ↓  Admin reviews the enquiry's guideline values
  ↓  Admin can adjust fees: increase, discount, modify year-wise split
  ↓  Admin finalizes the fee structure
  ↓  Status: FEES_FINALIZED

Step 4: PAYMENT COLLECTION (Accounting/Cashier)
  ↓  Cashier collects payment (full or partial)
  ↓  Multiple payment modes supported
  ↓  Receipts generated for each payment
  ↓  Status: FEES_PAID or PARTIALLY_PAID

Step 5: SUBMIT DOCUMENTS
  ↓  Student submits required documents (10th, 12th, TC, etc.)
  ↓  Status: DOCUMENTS_SUBMITTED

Step 6: VERIFY DOCUMENTS
  ↓  Admin/staff verifies all mandatory documents
  ↓  Status: DOCUMENTS_VERIFIED

Step 7: COMPLETE ADMISSION (Admin)
  ↓  Admin completes admission from verified enquiries only
  ↓  Student record created with all data
  ↓  Roll number assigned
  ↓  Fee allocation linked
  ↓  Status: ADMITTED

Step 8: STUDENT EXPLORER
  ↓  Student appears in Student Explorer with all filters
  ↓  Full student lifecycle management begins
```

---


## BR-25: Profile Self-Service Editing and User Profile Photo

Users may personalize their own Profile page without gaining access to admin-only Faculty/Student edit forms.

### Self-edit whitelist

The `/api/v1/profile/me/self-info` endpoint resolves the caller exclusively from the authenticated JWT. It does not accept a target user ID.

| Field | Faculty self-edit | Student self-edit | Admin self-edit |
|-------|-------------------|-------------------|-----------------|
| Phone | Yes | Yes | No |
| Blood Group | Yes | Yes | No |
| Address: postal address/street/city/district/state/pincode | Yes | Yes | No |
| Profile Photo | Yes | Yes | Yes |
| Employee Code / Designation / Speciality / Status / Joining Date | No — admin-only | N/A | N/A |
| Roll Number / Program / Year of Study / Admission Date | N/A | No — admin-only | N/A |
| Email / Login Identity | No — SSO/admin-managed | No — SSO/admin-managed | No — SSO/admin-managed |

### Profile photo rules

- Profile photos are stored on the `app_users` record, not in Faculty/Student document verification records.
- Faculty `FACULTY_PHOTO` documents are not reused for profile avatars.
- All users may upload/remove their own profile photo.
- Supported upload types: JPEG and PNG only.
- Maximum size: 2 MB.

---

## BR-26: Faculty Document Review Summary and Verification Locks

Faculty document review must be visible from faculty discovery screens and faculty detail documents, while protecting reviewed evidence from accidental overwrite. The standalone Faculty Document Verification queue screen is retired; reviewers work from the selected faculty member's Documents tab.

### Faculty list review status

- Faculty list API responses include a derived `documentReview` summary with total documents, required documents, pending-verification, rejected, missing-required, and verified-required counts.
- Document review is derived from faculty document rows and document-type requirements; it must not be stored as a separate faculty lifecycle state.
- Faculty list UI displays a document-review badge for each faculty member in both card and table views.
- Badge precedence is: rejected, needs verification, missing required, all verified, no documents, then has documents.
- Users can filter faculty by document review status: all document states, needs verification, rejected, missing required, fully verified, no documents, or has any documents.
- Clicking the document badge/action from the faculty list opens `/faculty/{id}#documents`, and the faculty detail page must select the Documents tab when this hash is present.

### Verification lock rules

- A document marked `VERIFIED` is read-only for file replacement until an authorized reviewer changes its status away from `VERIFIED`.
- A document cannot be marked `VERIFIED` unless a file is uploaded.
- A document cannot be marked `REJECTED` unless a rejection reason is provided.
- Verification and rejection actions record reviewer identity, timestamp, and history for auditability.
- Re-uploading a non-verified document resets its verification metadata and moves the document back to `UPLOADED` for review.
- Faculty document review must use DB-driven permissions; no screen or API should hardcode role names for document actions.
- Faculty document review must not add, replace, or overload `FacultyStatus`; employment status remains independent from document review status.

---

## BR-27: Permanent Admission Number Generation

### Business Rule

Every completed admission must receive a permanent, immutable **admission number** that acts as the student's lifelong college reference until and beyond roll number, UMIS number, and university registration number generation.

### Key Points

1. The admission number is generated **only when admission completion/confirmation succeeds**.
2. Failed admission attempts must not reserve or consume an admission number. A retry receives a number only after the successful transaction completes.
3. Format is academic-year scoped: `ADM-2526-0001`.
4. The sequence resets for each joining academic year and is unique across the college for that academic year.
5. The number is stored on the student record as an immutable reference and is not manually editable from the UI.
6. Receipts must display the receipt number and, once available, the admission number. Roll number remains optional until generated later.
7. Admission and student list/detail screens must show and search by admission number.
8. Generated number series must be visible in a read-only Number Sequences screen backed by DB-driven permissions.

### Entities Involved

- `Student.admissionNumber` — permanent admission reference.
- `ApplicationNumberSequence` — common sequence registry for admission numbers and future generated numbers.
- `PaymentReceipt.admissionNumber` — receipt snapshot of the admission reference when collecting student payments.

### Permissions

- `NUMBER_SEQUENCE_VIEW` — can view the generated number sequence registry.


---

## BR-29: UI Validation & Form Behaviour Standards

> **Scope:** Applies to every master-data form, entity form, and input component in the system. These are non-negotiable base rules — every screen that collects user input MUST comply. Implemented via `shared/validators/cms-validators.ts`.

---

### 29.1 Boundary Value (min / max length)

| Rule | Detail |
|------|--------|
| Min & max character limits must be enforced on every text field | Backend constraints define the values; the frontend must mirror them |
| **Leading / trailing spaces are not counted** toward the character minimum | `"  A  "` counts as 1 character (trimmed before length check) |
| No more than **one consecutive space** is allowed anywhere in a text field | `"John  Doe"` (two spaces) → rejected |

**Validator:** `noConsecutiveSpaces()` + `trimmedMinLength(n)` from `cms-validators.ts`.

---

### 29.2 Code Fields

| Rule | Detail |
|------|--------|
| Spaces are **not allowed anywhere** inside a code value | Neither leading, trailing, nor mid-word |
| Copy-paste with embedded spaces must be rejected immediately (on input) | Strip via `stripSpaces()` utility in `(input)` handler |
| Codes are alphanumeric; special chars allowed: underscore `_`, hyphen `-` | Apply via `pattern` or `noInternalSpaces()` validator |
| **Unique validation is case-insensitive** | `ABC`, `abc`, `Abc` → all treated as the same code on the backend |
| **Save as-is** — the casing the user enters is preserved in the database | `"BSc"` saves as `"BSc"`, not normalised |

**Validator:** `noInternalSpaces()` from `cms-validators.ts`.

---

### 29.3 Unique Validation (Name & Code)

| Rule | Detail |
|------|--------|
| Unique check is **case-insensitive** | `"Nursing"`, `"NURSING"`, `"nursing"` → same entity |
| Value is **saved exactly as entered** (case preserved) | Don't normalise to uppercase/lowercase before persisting |
| Duplicate check must cover both active and inactive records | A soft-deleted record still blocks re-creation under the same name/code |

Backend returns HTTP 409 with a descriptive message. Frontend must surface that message verbatim in the form error area.

---

### 29.4 Dropdown

| Rule | Detail |
|------|--------|
| Add a placeholder/header option when the list is optional | e.g., `"— Select Speciality —"` as first option |
| Hovered item must have a visually distinct background | Different from the selected-item highlight |
| Selected item must show a check or distinct highlight | Never same style as unselected hover |

---

### 29.5 Autocomplete

| Rule | Detail |
|------|--------|
| On form submit, free text that does not match a valid option must be rejected | Validate the bound object, not the display string |
| Hover style ≠ selected style | Same contrast rule as dropdown |
| Suggestion list: show **most recently changed items first**, else alphabetical | Backend must sort by `updatedAt DESC` then `name ASC` |
| If searchable by code, suggestion list shows `CODE \| Name` format | e.g., `BSC-NURS \| B.Sc Nursing` |
| Autocomplete must NOT open on double-space | Treat double-space as invalid input per 29.1 |

---

### 29.6 Date Picker

| Rule | Detail |
|------|--------|
| Restrict future or past dates per the business context of each field | Academic year start/end: no future; Birth date: no future; Expiry: only future |
| **FROM / TO range fields:** TO date must be ≥ FROM date | Disable dates in TO picker that are before the selected FROM date |

---

### 29.7 Form Submit & Multi-Click Prevention

| Rule | Detail |
|------|--------|
| Submit button shows a spinner while the API call is in progress | Use `MatProgressSpinner` (diameter 18) inside the button |
| Submit button is **disabled** while saving | `[disabled]="saving()"` |
| Only one API call is made per user action | The button state must gate all subsequent clicks |
| On submit, mark all fields touched to reveal inline errors | Call `form.markAllAsTouched()` before returning on invalid |

**Pattern:** `saving` signal set to `true` before API call, reset on `next` and `error`.

---

### 29.8 Update Form

| Rule | Detail |
|------|--------|
| Update button label must be **"Update"** in edit mode, **"Save"** in create mode | `isEditMode() ? 'Update' : 'Save'` |
| Inactive records **can be updated** (status field can be toggled on the form) | Don't disable edit access based on active/inactive status |

---

### 29.9 Inactive & Delete Protection

| Rule | Detail |
|------|--------|
| An entity linked to any other active record **cannot be made inactive** | Backend returns 409; frontend must show the error message |
| An entity linked to any other record (active or historic) **cannot be deleted** | Backend returns 409; frontend must show a clear error: "Cannot delete — this record is in use" |
| Before any delete, a **confirmation dialog** must appear | Dialog must state the entity name and warn about permanence |

---

### 29.10 Table, Sorting & Pagination

| Rule | Detail |
|------|--------|
| Default list order: **name ascending** for master data; **date descending** for transactions | Applies to initial page load and after any CRUD operation |
| Clicking a sort column in create/edit mode must not reset the display order field | Sorting UI only affects the list view, not form values |
| Pagination page-change must show a **spinner** in the table area until data loads | Replace table rows with a centered spinner |

---

### 29.11 Search

| Rule | Detail |
|------|--------|
| Search must cover both **active and inactive** records | Unless explicitly specified that only active items are searchable |
| Search is based on **name AND code** (where code exists) | Both fields must be matched |
| If code is mandatory for an entity, search must accept code as a standalone term | A code-only search returns the matching record |

---

### Implementation Reference

| Validator / Utility | File | Use on |
|---|---|---|
| `noConsecutiveSpaces()` | `shared/validators/cms-validators.ts` | All name / text fields |
| `noInternalSpaces()` | `shared/validators/cms-validators.ts` | All code fields |
| `trimmedMinLength(n)` | `shared/validators/cms-validators.ts` | All name fields with min length |
| `cmsFieldError(control, label)` | `shared/validators/cms-validators.ts` | All `getErrorMessage()` methods |
| `stripSpaces(value)` | `shared/validators/cms-validators.ts` | Code `(input)` event handler |
| `collapseSpaces(value)` | `shared/validators/cms-validators.ts` | Name `(blur)` event handler |

---

## 📝 Change Log

| Date | BR ID(s) | Change Description | Changed By |
|------|----------|-------------------|------------|
| 2026-06-03 | BR-31 | **Student Data Import — Legacy Migration:** Added full BR for bulk student migration. Documents 9-item pre-conditions checklist (programs, courses, academic years, cohorts + seat allocations, fee structures, fee states, WALK_IN referral type, country seed); recommended course-wise import strategy with step-by-step workflow and rationale (error isolation, cohort verification, fee structure uniformity per course); fee history multi-payment row explanation (`year_1_fee`..`year_6_fee` = annual fee split per programme year, not per-payment; subsequent rows per student leave fee structure columns blank); 44-column Students sheet with all personal/demographic/family/address/registration/classification fields; Qualifications sheet (8 fields); Fee History sheet (16 fields); Step 2 defaults panel including `admission_category`; cohort assignment logic (error if cohort missing for course+AY); fee state inference from address state; boolean strict-format rule (`TRUE`/`FALSE` only); unique number conflict check for 4 registration numbers; 3-pass import execution flow; post-import state; and permissions (`IMPORT_DATA`). | — |
| 2026-05-25 | BR-10 | Admission/student document screens now prioritize missing required documents at the top, allow uploading newly required documents for existing admissions, and preserve previously collected documents that were later removed from program requirements as "not currently required" records. | — |
| 2026-05-25 | BR-10 | Admission printable template updated for one-page A4 print preview: Academic Qualifications are excluded from official View/Print/Download output, print text readability is improved, the document checklist remains two-column, and the submitted `PASSPORT_PHOTO` document is the source for the admission-form passport photo. | — |
| 2026-05-25 | BR-10 | Admission form view/print/download standardized on one printable template; document checklist now renders in two balanced columns using `ceil(document count / 2)` rows (20→10, 23→12, 31→16), and the download action generates PDF instead of HTML. | — |
| 2026-05-21 | BR-30, BR-3, BR-6, BR-23 | **BR-30 post-implementation fixes (review pass):** (1) Enquiry form — gender change now re-fetches fee; fee banner shows contextual "what's still needed" text; programs without courses correctly trigger fee calculation (`courseId` removed from null-guard in `applyAuthoritativeFees`); `updateCourseValidator` triggers fee load for no-course programs; `tryLoadFeeGuideline` guard prevents misleading "not found" when courses exist but none is selected. (2) Fee finalization — Quota filter dropdown added to toolbar; `filteredEnquiries` includes quota in text search; `applyEqualSplitFallback` uses actual program `durationYears` (not hardcoded 4); `discountReason` signal synced to FormControl; "Fee Basis" group label + divider added to info panel. (3) API layer — `applyAuthoritativeFees` error message now shows fee state name (not raw ID); `GET /fee-structures/grouped` extended to accept `quota`, `feeStateId`, `gender`, `studentType` as optional filters; `DataIntegrityViolationException` handler improved with specific messages for `uq_fee_structure_group` and `uq_fee_structure_group_fee_type` constraint violations. | — |
| 2026-05-21 | BR-30, BR-1, BR-3, BR-6, BR-12 | **Multi-dimension fee structure (BR-30):** Fee structure uniqueness key extended to `program + academicYear + course + quota + feeState + gender + studentType`. New `FeeState` master (Tamil Nadu = default, Other State = fallback); new `FeeStructureGroup` entity; `FeeStructure` items linked to group. `Enquiry` gains `admissionQuota` and `feeState` FK. Fee lookup on enquiry form uses 6 fields; state auto-derived from address; fallback to Other State if no exact match; submission blocked if no configuration found. Enquiry form adds Admission Quota dropdown (default: Management). Fee finalization shows all 4 dimensions as read-only context rows. Migrations V165–V168. BR-1 uniqueness rule amended; BR-3 fee-load flow rewritten; BR-6 finalization amended; BR-12 studentType noted as one of 4 dimensions. | — |
| 2026-05-18 | BR-29 | Added UI Validation & Form Behaviour Standards: boundary value / empty-space rules, code-field no-space rule, case-insensitive unique validation, dropdown/autocomplete UX, date-picker range rules, submit multi-click prevention, update button label, inactive/delete protection, table ordering, pagination spinner, and search scope. Implemented `cms-validators.ts` with `noConsecutiveSpaces`, `noInternalSpaces`, `trimmedMinLength`, `cmsFieldError`, `stripSpaces`, `collapseSpaces`. Applied across all master-data form components. | — |
| 2026-05-16 | BR-28 | Added notification & alert preferences: generic vs role-specific categories, per-role defaults, delivery channels (in-app/email/both), backend requirements for `user_notification_preferences` table, sending service triggers, and current localStorage-only state with migration path to backend | — |
| 2026-05-16 | BR-27 | Added permanent admission number generation on successful admission completion/confirmation, academic-year format `ADM-2526-0001`, immutable student reference, receipt display, searchable admission/student screens, and read-only number sequence registry | — |
| 2026-05-14 | BR-26 | Added derived faculty document-review summary on faculty discovery screens, review-status filtering, document workflow entry points, verification lock/audit rules, re-upload reset behavior, and the rule that document review must not create or overload `FacultyStatus` | — |
| 2026-05-14 | BR-25 | Added profile self-service rules: authenticated users can update only their own phone, blood group, and address; profile photos are stored on `app_users` for all roles with JPEG/PNG and 2 MB limits; admin-only academic/employment/login fields remain locked | — |
| 2026-05-12 | BR-24 | RBAC aligned to DB-driven authorization: Keycloak realm exports are identity-only, immutable default roles are limited to `DEV_ADMIN`/`SUPPORT_ADMIN`, and `collegeadmin` is scoped to admission-related DB permissions | — |
| 2026-05-22 | BR-24 | Go-live RBAC baseline tightened to exactly `DEV_ADMIN`, `SUPPORT_ADMIN`, and `collegeadmin`; `collegeadmin` can manage admission-required masters and admission/fee workflows but cannot see platform admin roles | — |
| 2026-05-06 | BR-3, BR-5, BR-6, BR-23 | Fixed enquiry fee over-calculation risk: fee guideline lookup is current-academic-year scoped and active-row-only; backend recalculates enquiry fee totals from authoritative fee structures on create/update; referral/agent commission is decoupled from student fee and tracked separately; fee finalization uses backend-calculated totals and enforces exact two-decimal monetary values with discount bounds; frontend aggregation uses integer paise arithmetic to prevent rounding drift | — |
| 2026-05-06 | BR-22 | Enquiry form mandatory fields (Phone, Country, State, Program, Course), Country/State pre-filled to India/Tamil Nadu; referral-linked person search: AGENT_REFERRAL→Agent dropdown (existing), ALUMNI/STUDENT→Student table search, FACULTY→Faculty table search; two new referral type seeds (STUDENT ₹500, FACULTY ₹500); `referred_student_id` and `referred_faculty_id` FK columns added to enquiries; course required conditionally based on program having courses | — |
| 2026-05-06 | BR-14 to BR-21 | Added scholarship management module: (BR-14) Scholarship Type master with INSTITUTION/GOVT_PORTAL application modes, PERCENTAGE/FIXED_AMOUNT/FULL_WAIVER discount types, year-of-study eligibility bounds, renewal flag, govt portal fields; (BR-15) Student scholarship eligibility profile with EWS income auto-flag (₹3,00,000 limit), DBT bank account & Aadhaar fields, admin verification workflow; (BR-16) Scholarship application lifecycle — PENDING → APPROVED → SANCTIONED (govt-portal only) / REJECTED / ON_HOLD / CANCELLED, renewal across academic years, year-of-study restriction; (BR-17) Scholarship disbursement recording with DIRECT_CREDIT / FEE_WAIVER / CHEQUE modes; (BR-18) Optional country/state/district location fields on Enquiry; (BR-19) Transaction reference mandatory for UPI, BANK_TRANSFER, CHEQUE payments via custom `@TransactionReferenceRequired` Bean Validation annotation; (BR-20) FeeType enum expanded with CLINICAL_FEE, BOOK_AND_PACKET_FEE, UNIFORM_AND_SHOES_FEE, UNIVERSITY_REGISTRATION_FEE; LAB_FEE renamed LABORATORY_FEE; (BR-21) Student entity gains isFirstGraduate, fatherEducation, motherEducation fields auto-mirrored from eligibility profile | — |
| 2026-04-27 | BR-13 | Added semester-wise fee collection: yearly fees auto-split into 2 semesters on finalization, payment cascade logic, single receipt per payment (fixed multiple-receipt bug), semester status as primary view in UI, receipt grouping by receipt number | — |
| 2026-04-17 | BR-1, BR-2, BR-3, BR-12 | Fee structure and enquiry enhancements: (1) BR-1 updated — one fee structure group per course+academic year enforced; (2) BR-2 updated — year boxes based on program durationYears, all 8 fee types shown; (3) BR-3 updated — enquiry shows total fee only (no split), filtered by student type; (4) BR-12 added — student type (DAY_SCHOLAR/HOSTELER) on enquiry, controls fee inclusion | — |
| 2026-04-16 | BR-3, BR-4, BR-5, BR-6, BR-7 | Enquiry-to-Fee Workflow enhancements: (1) BR-3 updated for program→course→fee flow with course selection; (2) BR-4 updated — `guidelineValue` replaced with `hasCommission` boolean + `commissionAmount`, `source` enum dropped in favor of `referralType` FK; (3) BR-5 updated to reflect commission-based calculation; (4) BR-6 updated — fee finalization is now enquiry-driven, lists INTERESTED enquiries; (5) BR-7 updated — payment collection lists FEES_FINALIZED enquiries, payments tracked against enquiry | — |
| 2026-04-15 | BR-1 to BR-11 | Initial business requirements documented for fee structure, enquiry workflow, referral types, payment collection, submit documents, and student explorer | — |

---

## BR-28: Notification & Alert Preferences

> **Status:** UI scaffold complete (localStorage only). Backend service and delivery pipeline pending.

### Summary

Users must be able to control which notifications they receive and how they are delivered. Preferences are stored per-user and respect both generic (all-role) and role-specific categories.

### Notification Categories

#### Generic — shown to all roles
| Key | Label | Default |
|-----|-------|---------|
| `systemAnnouncements` | System Announcements | On |
| `profileReminders` | Profile & Document Reminders | On |

#### Role-specific
| Role | Category Key | Label | Default |
|------|-------------|-------|---------|
| ADMIN, FRONT_OFFICE | `admissionUpdates` | Admission Updates | On |
| ADMIN, CASHIER, FRONT_OFFICE | `feeAlerts` | Fee Alerts & Due Dates | On |
| FACULTY | `examSchedule` | Exam & Timetable Changes | On |
| FACULTY | `attendanceAlerts` | Attendance Threshold Alerts | On |
| STUDENT | `feeAlerts` | Fee Due Reminders | On |
| STUDENT | `examSchedule` | Exam Schedule & Results | On |
| STUDENT | `attendanceAlerts` | Low Attendance Warnings | On |

### Delivery Channels
- **In-App** — notification bell in toolbar (already exists as UI chrome, no backend yet)
- **Email** — sent to the user's registered login email via backend mail service
- **Both** — in-app and email simultaneously

Default channel for all roles: **In-App only**.

### Storage
- Preferences stored in `user_notification_preferences` table (to be created via Flyway migration)
- Key: `(user_id, category_key)` unique constraint
- Fields: `enabled BOOLEAN`, `channel ENUM('IN_APP','EMAIL','BOTH')`, `updated_at`
- Fallback: if no DB row exists, use the category default

### Backend Requirements
1. `GET /api/v1/notifications/preferences` — return user's current preferences
2. `PUT /api/v1/notifications/preferences` — update one or more preferences
3. Notification sending service (queued, not synchronous) — triggers when relevant events fire:
   - Fee finalization → feeAlerts subscribers
   - Admission status change → admissionUpdates subscribers
   - Document verification → profileReminders subscribers
   - System config change → systemAnnouncements all users
4. Email delivery via JavaMail / SMTP configured in `application.yml`

### Current Implementation State (2026-05-16)
- UI: Profile page Notifications card with channel toggle + category toggles
- Storage: `localStorage` only (`cms_notif_prefs`) — **not** persisted to server
- Categories shown are static and not yet role-filtered
- No actual notification delivery exists yet
- The UI must be updated to call backend endpoints once the service is built, replacing the localStorage layer

### Permissions Required
- `NOTIFICATION_MANAGE` — update own notification preferences (all authenticated users)

---

---

## BR-30: Multi-Dimension Fee Structure (Quota × State × Gender × Student Type)

### Business Rule

The fee a student pays varies across four admission dimensions: **Admission Quota** (Management / Counselling), **State** (Tamil Nadu or Other State), **Gender** (Male / Female / Other), and **Student Type** (Day Scholar / Hosteler). Each unique combination of these four dimensions — scoped to a program, academic year, and optional course — has its own independently configured fee structure group.

### Fee Dimensions

| Dimension | Values | Notes |
|-----------|--------|-------|
| **Admission Quota** | `MANAGEMENT`, `COUNSELLING` | Captured on the enquiry form |
| **Fee State** | Master-table entries (initially: Tamil Nadu, Other State) | Derived from the student's address state; extensible |
| **Gender** | `MALE`, `FEMALE`, `OTHER` | Captured on the enquiry form |
| **Student Type** | `DAY_SCHOLAR`, `HOSTELER` | Captured on the enquiry form |

### Data Model

```
FeeState (master table):
  id, name, code, isDefault, isFallback, sortOrder, isActive

FeeStructureGroup:
  program, academicYear, course (nullable), quota, feeState, gender, studentType
  UNIQUE: (program, academicYear, course, quota, feeState, gender, studentType)

FeeStructure (items within a group):
  feeStructureGroup (FK), feeType, amount, yearAmounts[]
  UNIQUE: (feeStructureGroup, feeType)
```

### Unique Key Rule

One fee structure group per `(program, academicYear, course, quota, feeState, gender, studentType)`. Attempting to create a duplicate combination is rejected.

### Fee State Resolution

1. The student's **address state** (free-text, from the country/state/district selector) is matched against `FeeState.name` (case-insensitive).
2. If matched → use that FeeState's ID for lookup.
3. If not matched → use the FeeState marked `isFallback = true` (initially: "Other State").
4. The student does **not** select a fee state separately — it is derived automatically.

### Fallback Rule

Fee lookup order:
1. Exact match: `(program, year, course, quota, feeState, gender, studentType)`.
2. Fallback match: same as above but with `feeState = fallback state`.
3. If neither exists → block enquiry submission with an admin contact message.

### Admin Configuration (Fee Structure Form)

Admins configure fees using a **Combination Picker → Fee Grid** pattern:
1. Select: Academic Year, Program, Course (optional)
2. Select: Quota, State, Gender, Student Type (4 new dropdowns)
3. Once all 7 criteria are filled → fee grid appears
4. Enter per-year amounts for each fee type
5. Save creates the group + items

### Enquiry Form Integration

- **Admission Quota** dropdown added after Course selection (default: Management)
- **State** is derived automatically from the address state — no separate selector
- **Gender** and **Student Type** already existed; defaults are Female and Day Scholar
- All 6 fields must be filled before fee auto-loads
- Submission is blocked if no fee structure is found

### Fee Finalization Integration

The finalization screen shows all 4 dimensions as read-only context:
Quota, State (fee state name), Gender, Student Type.
The year-wise fee breakdown is loaded from the stored `yearWiseFees` JSON on the enquiry.
If `yearWiseFees` is absent (old data), the system re-fetches via the guideline endpoint using the stored dimensions.

### New API Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/api/v1/fee-states` | List active fee states (for dropdowns) |
| `GET` | `/api/v1/fee-structures/guideline` | Fee lookup for enquiry (all 6 params required) |
| `GET/POST/PUT/DELETE` | `/api/v1/fee-structures/bulk` | Group-based fee structure CRUD |
| `DELETE` | `/api/v1/fee-structures/group` | Delete a specific group + its items |

### Permissions

- `FEE_STRUCTURE_MANAGE` — configure fee structure groups
- `ENQUIRY_CREATE` / `ENQUIRY_EDIT` — set quota/student type on enquiry
- `FEE_FINALIZE` — finalize fees (unchanged from BR-6)

### Migration Notes

- Existing fee structure rows were cleared; fee structures must be re-entered via the admin UI.
- Existing enquiries retain null `admissionQuota` and `feeStateId`; they are not affected by BR-30.
- New enquiries require all 4 dimensions to be filled.

---

---

## BR-31: Student Data Import — Legacy Migration

### Business Rule

Administrators must be able to bulk-migrate existing students from paper records or a legacy system into the CMS without going through the enquiry → fee finalization → admission flow. The import is a **one-time or occasional administrative operation** that creates fully-formed Student, Admission, and fee records from a structured Excel file.

Every imported student receives a synthetic `ADMITTED` enquiry (tagged WALK_IN) so the system's referential integrity constraint (every admission must originate from an enquiry) is satisfied without polluting the live enquiry pipeline.

---

### Pre-Conditions Checklist

All of the following masters **must exist in the system before running any import**. Missing any of these will cause import rows to fail.

| # | Master | Where to Create | Why Required |
|---|--------|-----------------|--------------|
| 1 | **Programs** | Masters → Programs | Every student row requires a valid `program_code`. Import errors the row if the program is not found. |
| 2 | **Courses** | Masters → Courses | Required for cohort resolution. Each course must be linked to its program and have a unique `course_code`. |
| 3 | **Academic Years** | Masters → Academic Years | At least one academic year must exist for every admission year present in the import file. One must be flagged `isCurrent = true` as the system default. |
| 4 | **Cohorts** | Masters → Cohorts → Initialize | One cohort per `(course × admission academic year)` combination. **Import errors the row if no cohort is found.** Cohorts must be created before import — use the "Initialize Cohorts" action after creating an academic year. |
| 5 | **Cohort Seat Allocations** | Masters → Cohorts → Edit Seats | Set `total_seats` and `management_percentage` on each cohort. Without this, seat tracking reports will show 0 capacity against actual enrolment. |
| 6 | **Fee Structures** | Masters → Fee Structures | Create fee structure groups for every `(program × academic year × course × quota × fee state × gender)` combination present in old student data. Required for fee structure reporting; not used to auto-fill import amounts (amounts are always provided explicitly in the file). |
| 7 | **Fee States** | Already seeded | Tamil Nadu and Other State are seeded by default. The import infers fee state from each student's address state — no manual action needed unless additional fee state segments are required. |
| 8 | **Referral Type: WALK_IN** | Masters → Referral Types | Must exist with `code = WALK_IN`. The import hard-codes this referral type for all synthetic enquiries. If missing, the import throws an exception and aborts. |
| 9 | **Country with ID = 1** | Seed data | Address records are created with `country_id = 1` (India). Must be present in the `countries` table. Normally present from initial seed data. |

---

### Recommended Import Strategy — Course-Wise Approach

Although the import template accepts students from **any number of courses in a single file**, the recommended approach is to **import one course at a time**. This is a firm operational guideline, not a technical limitation.

#### Why Course-Wise Is Recommended

| Reason | Detail |
|--------|--------|
| **Fee structures differ per course** | BSc Nursing (4 years) has different annual fee slabs than GNM (3 years). Mixing courses in one file means `year_1_fee`…`year_6_fee` vary row-by-row, increasing the chance of data entry errors. |
| **Programme duration varies** | A 3-year course uses `year_1`…`year_3`; a 4-year course uses `year_1`…`year_4`. Per-course files have a uniform column structure. |
| **Cohort pre-conditions are per course** | Before importing, the admin must verify cohorts exist for every `(course × admission academic year)` in the file. Verifying one course at a time is far easier than cross-checking all cohorts for all courses before a combined import. |
| **Error isolation** | If one course's data has a problem mid-import, it doesn't block or partially corrupt another course's records. |
| **Easier post-import audit** | After importing BSc Nursing, the admin can spot-check a few students in the UI before moving to the next course. |

#### Recommended Workflow Per Course

```
For each course (BSc Nursing → GNM → ANM → … ):

  Preparation
  ───────────
  1. Verify all pre-conditions exist for this course:
     - Program and Course master records
     - Academic year records for every admission batch in this course
     - Cohorts initialized for every (course × academic year) combination
     - Seat allocations set on each cohort
     - Fee structures created for this course's (program × AY × quota × fee state × gender)

  Data Entry
  ──────────
  2. Download the template from Student Management → Data Import → Step 1
  3. Fill the Students sheet with only this course's students (all admission batches)
  4. Fill the Qualifications sheet for these students
  5. Fill the Fee History sheet:
       - First row per student: total_fee + year_1_fee…year_N_fee (N = programme duration)
         AND the first payment if one exists
       - Subsequent rows per student: leave total_fee / year_* blank; fill payment columns only
         (one row per additional receipt)

  Import
  ──────
  6. Step 2 — Set defaults: select the programme, choose the appropriate academic year
  7. Click "Validate" — review all warnings and errors; fix the Excel file if needed
  8. Click "Execute" once validation passes with no errors
  9. Spot-check 2–3 students in Student Management to confirm data, fees, and receipts

  Repeat for the next course.
```

#### Fee History Sheet — Multi-Payment Rows Explained

Each row in the Fee History sheet represents **one payment receipt**, not one academic year.

| Row | Role | Which columns to fill |
|-----|------|-----------------------|
| First row per student | Sets fee allocation **and** records first payment | All columns: `total_fee`, `discount_*`, `net_fee`, `year_1_fee`…`year_N_fee`, `amount_paid`, `payment_date`, `payment_mode`, `receipt_number` |
| Subsequent rows per student | Records additional payment receipts only | Only: `student_email`, `amount_paid`, `payment_date`, `payment_mode`, `receipt_number`, `remarks` — leave all other columns blank |

The `year_1_fee`…`year_6_fee` columns represent **how the programme total fee is split per year of study** (e.g., Year 1: ₹50,000 / Year 2: ₹50,000 / Year 3: ₹50,000 for a 3-year course). They are **not** per-payment amounts. Leave them blank to split evenly across the programme duration.

---

### Import Template Structure

The template is downloaded from **Student Management → Data Import → Step 1 (Download Template)**. It is generated live from your system's current master data (programs, courses, academic years are auto-populated in the Reference sheet).

The workbook contains four sheets:

| Sheet | Purpose |
|-------|---------|
| **Students** | One row per student — all personal, demographic, academic, family, address, registration, and classification fields (44 columns) |
| **Qualifications** | Academic history — one or more rows per student, linked by `student_email` |
| **Fee History** | Fee allocation and historical payment records — one or more rows per student, linked by `student_email` |
| **Reference** | Auto-populated valid codes and enum values (read-only — used for dropdown validation only) |

Header colour coding: **Pink = Required**, **Blue = Optional**, **Yellow row = data type hint**.

---

### Students Sheet — All 44 Fields

#### Core Identity (Required)

| Column | Required | Format | Notes |
|--------|----------|--------|-------|
| `first_name` | Yes | Text | |
| `last_name` | Yes | Text | |
| `email` | Yes | Email | Must be unique across the entire system |
| `phone` | No | Text | |

#### Academic Programme

| Column | Required | Format | Default | Notes |
|--------|----------|--------|---------|-------|
| `program_code` | Yes | Code (see Reference) | — | Must match an existing program |
| `course_code` | No | Code (see Reference) | — | Must match an existing course; required for cohort assignment |
| `joining_academic_year` | No | Text e.g. `2024-25` | Current academic year | Must match an existing academic year name exactly |
| `application_date` | No | Date `DD-MM-YYYY` | Today | Original application/admission date from old records |
| `student_type` | No | Enum (see Reference) | Step 2 default | `DAY_SCHOLAR` or `HOSTELER` |

#### Personal / Demographics

| Column | Required | Format | Default | Notes |
|--------|----------|--------|---------|-------|
| `date_of_birth` | No | Date `DD-MM-YYYY` | — | |
| `gender` | No | Enum (see Reference) | — | `MALE`, `FEMALE`, `OTHER` |
| `aadhar_number` | No | Text (12 digits) | — | |
| `nationality` | No | Text | Step 2 default (`Indian`) | |
| `religion` | No | Text | — | |
| `community_category` | No | Enum (see Reference) | — | `SC`, `ST`, `BC`, `MBC`, `DNC`, `OC`, `OTHERS` |
| `caste` | No | Text | — | |
| `blood_group` | No | Enum (see Reference) | — | `A_POSITIVE`, `A_NEGATIVE`, `B_POSITIVE`, `B_NEGATIVE`, `O_POSITIVE`, `O_NEGATIVE`, `AB_POSITIVE`, `AB_NEGATIVE` |

#### Family (Basic)

| Column | Required | Format | Default |
|--------|----------|--------|---------|
| `father_name` | No | Text | — |
| `mother_name` | No | Text | — |
| `parent_mobile` | No | Text | — |

#### Address

| Column | Required | Format | Default |
|--------|----------|--------|---------|
| `postal_address` | No | Text | — |
| `street` | No | Text | — |
| `city` | No | Text | — |
| `district` | No | Text | — |
| `state` | No | Text | Step 2 default | Used to infer fee state — see Fee State Inference below |
| `pincode` | No | Text | — |

#### Registration Numbers (Unique — Conflict Check Applied)

| Column | Required | Format | Default | Notes |
|--------|----------|--------|---------|-------|
| `admission_number` | No | Text (from old system) | Auto-generated | If provided and already exists in DB → **error the row** |
| `roll_number` | No | Text (from old system) | Auto-generated | If provided and already exists in DB → **error the row** |
| `university_registration_number` | No | Text (university-issued) | — | If provided and already exists in DB → **error the row** |
| `umis_number` | No | Text (UMIS-issued) | — | If provided and already exists in DB → **error the row** |

#### Admission Classification

| Column | Required | Format | Default | Notes |
|--------|----------|--------|---------|-------|
| `admission_category` | No | Enum (see Reference) | Step 2 default (`MANAGEMENT`) | `MANAGEMENT` or `COUNSELLING` — used for quota seat tracking |

#### Extended Family Contacts

| Column | Required | Format |
|--------|----------|--------|
| `father_phone` | No | Text |
| `father_email` | No | Email |
| `mother_phone` | No | Text |
| `mother_email` | No | Email |

#### Scholarship / Socioeconomic

| Column | Required | Format | Default | Notes |
|--------|----------|--------|---------|-------|
| `is_first_graduate` | No | `TRUE` or `FALSE` | `FALSE` | Strict format only — any other value triggers a warning and defaults to `FALSE` |
| `father_education` | No | Enum (see Reference) | — | `ILLITERATE`, `PRIMARY`, `SECONDARY`, `HSC`, `UG`, `PG`, `DOCTORATE` |
| `mother_education` | No | Enum (see Reference) | — | Same values as father_education |

#### Medical / Disability

| Column | Required | Format | Default | Notes |
|--------|----------|--------|---------|-------|
| `physical_disability` | No | `TRUE` or `FALSE` | `FALSE` | Strict format only — any other value triggers a warning and defaults to `FALSE` |

#### Emergency Contact

| Column | Required | Format |
|--------|----------|--------|
| `emergency_contact_name` | No | Text |
| `emergency_contact_relationship` | No | Text e.g. `Father`, `Guardian` |
| `emergency_contact_phone` | No | Text |

#### Academic Placement / Profile

| Column | Required | Format | Notes |
|--------|----------|--------|-------|
| `lab_batch` | No | Text e.g. `Batch-A` | |
| `bio` | No | Text max 500 chars | Silently truncated if longer |

---

### Qualifications Sheet — All 8 Fields

| Column | Required | Format | Notes |
|--------|----------|--------|-------|
| `student_email` | Yes | Email | Must match a row in the Students sheet |
| `qualification_type` | Yes | Enum (see Reference) | `SSLC`, `HSC`, `DIPLOMA`, `UG`, `PG`, `OTHER` |
| `school_name` | No | Text | |
| `major_subject` | No | Text | |
| `total_marks` | No | Number | |
| `percentage` | No | Decimal e.g. `88.50` | |
| `month_year_of_passing` | No | Text e.g. `March 2022` | |
| `university_or_board` | No | Text | |

Multiple rows per student are allowed (one per qualification level).

---

### Fee History Sheet — All 16 Fields

One row = one payment transaction. The **first row per student** also creates the fee allocation (total fee + discount structure). Subsequent rows for the same student only add payment records.

| Column | Required | Format | Notes |
|--------|----------|--------|-------|
| `student_email` | Yes | Email | Must match a row in the Students sheet |
| `total_fee` | Yes | Decimal e.g. `450000` | Gross fee before discount |
| `discount_amount` | No | Decimal | Discount applied (if any) |
| `discount_reason` | No | Text | Reason for discount |
| `net_fee` | No | Decimal | Auto-computed if blank: `total_fee − discount_amount` |
| `amount_paid` | No | Decimal | Amount collected in this payment transaction |
| `payment_date` | No | Date `DD-MM-YYYY` | Date the payment was collected |
| `payment_mode` | No | Enum (see Reference) | `CASH`, `UPI`, `BANK_TRANSFER`, `CARD`, `CHEQUE`, `DEMAND_DRAFT`, `SCHOLARSHIP` |
| `receipt_number` | No | Text (from old system) | Original receipt number from legacy records |
| `remarks` | No | Text | |
| `year_1_fee` | No | Decimal | Exact fee for Year 1 |
| `year_2_fee` | No | Decimal | Exact fee for Year 2 |
| `year_3_fee` | No | Decimal | Exact fee for Year 3 |
| `year_4_fee` | No | Decimal | Exact fee for Year 4 |
| `year_5_fee` | No | Decimal | Exact fee for Year 5 |
| `year_6_fee` | No | Decimal | Exact fee for Year 6 |

#### Year-wise Fee Rules

- If **any** of `year_1_fee` through `year_6_fee` has a value > 0, those exact amounts are used for the student's fee allocation year breakdown. This supports students from different admission batches who paid different amounts per year under different fee slabs.
- If **all** year columns are blank, the net fee is split evenly across the programme's `durationYears` (e.g. ₹4,50,000 for a 3-year programme → ₹1,50,000 per year).
- A **warning** (not an error) is issued if the sum of provided year fees does not equal `net_fee`. The import proceeds — the admin is notified to verify.

---

### Step 2 — Default Values

Before uploading the file, admins set system-wide defaults that fill in blank cells. Values explicitly provided in the spreadsheet always override these defaults.

| Default | Options | System Default |
|---------|---------|----------------|
| Joining Academic Year | Any existing academic year | Current academic year |
| Student Type | DAY_SCHOLAR, HOSTELER | DAY_SCHOLAR |
| Admission Category | MANAGEMENT, COUNSELLING | MANAGEMENT |
| Nationality | Free text | Indian |
| State | Free text | (blank) |
| Starting Year of Study | 1–12 | 1 |
| Skip errored rows | On / Off | On — valid rows import even if some rows fail |

---

### Cohort Assignment Logic

Every student must be assigned to a cohort. The cohort is resolved automatically using the student's `course_code` and `joining_academic_year`.

| Scenario | Result |
|----------|--------|
| `course_code` provided AND cohort exists for `(course, academic year)` | Student assigned to cohort ✅ |
| `course_code` provided BUT no cohort found for `(course, academic year)` | **Row errored** — "No cohort found for course X in AY Y. Create the cohort first." |
| `course_code` NOT provided | **Warning issued** — student imported with `cohort = null`. Cohort must be assigned manually from the student detail screen. |

---

### Fee State Inference Logic

The fee state is automatically inferred from the student's `state` address field and set on the synthetic enquiry. This ensures the student is correctly bucketed for fee structure group reporting without requiring any additional column.

| Student's `state` field | Fee State Assigned |
|-------------------------|--------------------|
| `Tamil Nadu` (case-insensitive match against FeeState names) | Tamil Nadu fee state |
| Anything else, or blank | Fallback fee state (Other State) |

This mirrors the live enquiry form behaviour described in BR-30.

---

### Validation Behaviour

| Condition | Behaviour |
|-----------|-----------|
| `first_name`, `last_name`, `email`, `program_code` blank | **Error** — row rejected |
| Email already exists in DB | **Error** — row rejected |
| `admission_number` / `roll_number` / `university_registration_number` / `umis_number` provided and already in DB | **Error** — row rejected |
| Program code not found | **Error** — row rejected |
| Academic year not found | **Error** — row rejected; if blank, Step 2 default is used |
| Course code not found | **Warning** — import continues; course set to null |
| Cohort not found for `(course, academic year)` | **Error** — row rejected |
| `is_first_graduate` or `physical_disability` has unrecognised value | **Warning** — field defaults to `FALSE`; row not rejected |
| `admission_category` has unrecognised value | **Warning** — defaults to `MANAGEMENT`; row not rejected |
| Year-wise fee sum ≠ net fee | **Warning** — import proceeds; admin is notified |
| `amount_paid` > `total_fee` | **Error** — fee row rejected |
| `bio` field > 500 characters | Silently truncated to 500 characters |

---

### Import Execution Flow

The import runs in **three sequential passes** within a single database transaction (unless "Skip errored rows" is enabled, in which case failed rows are skipped without aborting the transaction).

```
Pass 1 — Students + Admissions
  For each valid student row:
    1. Resolve Program, Course, Academic Year, Cohort
    2. Check unique number conflicts (admission_number, roll_number, URN, UMIS)
    3. Create Student record (44 fields)
    4. Create synthetic ADMITTED Enquiry (WALK_IN referral, fee state inferred)
    5. Create Admission record linked to Student + Academic Year + Enquiry

Pass 2 — Academic Qualifications
  For each qualification row:
    1. Resolve student by email
    2. Resolve student's Admission record
    3. Create AcademicQualification linked to Admission

Pass 3 — Fee Allocation + Payment History
  For each fee row:
    1. Resolve student by email
    2. First row per student → create StudentFeeAllocation
       (uses year_1_fee..year_6_fee if provided, otherwise even split)
    3. Each row with amount_paid > 0 → record historical payment
       (date, mode, receipt number, remarks preserved from old records)
```

---

### Post-Import State

After a successful import, each migrated student will have:

- A complete `Student` record with all provided fields populated
- An `Admission` record linked to their academic year
- A synthetic `ADMITTED` `Enquiry` (tagged WALK_IN, status = ADMITTED)
- `cohort_id` set on the student (if `course_code` was provided and cohort existed)
- `admissionCategory` (MANAGEMENT / COUNSELLING) set for quota seat tracking
- `feeState` set on the synthetic enquiry for fee structure reporting
- Academic qualifications linked to their admission
- `StudentFeeAllocation` with year-wise fee breakdown
- Historical payment records with original dates, modes, and receipt numbers

Students with `cohort = null` (due to missing `course_code`) must be manually assigned to a cohort from the Student Management → Student detail screen.

---

### Permissions

- `IMPORT_DATA` — required to access the Data Import screen, download the template, validate, and execute the import

---

### Entities Created per Imported Student

| Entity | Count per Student |
|--------|------------------|
| `Student` | 1 |
| `Enquiry` (synthetic) | 1 |
| `Admission` | 1 |
| `AcademicQualification` | 0..N (one per qualification row) |
| `StudentFeeAllocation` | 0 or 1 (if Fee History row exists) |
| `PaymentRecord` | 0..N (one per fee row with `amount_paid > 0`) |

---

> **⚠️ Documentation Policy:** Any changes to business rules, workflows, status transitions, fee logic, or operational processes described in this document must be reflected here **before** the corresponding code change is merged. This document, along with the milestone trackers and manual test cases, must always remain in sync with the implementation.
