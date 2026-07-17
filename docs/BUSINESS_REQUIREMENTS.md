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
- [BR-32: Master Lifecycle Status Management](#br-32-master-lifecycle-status-management)
- [BR-33: Institution Master & Staff Referrer Institution Scoping](#br-33-institution-master--staff-referrer-institution-scoping)
- [BR-34: OneBook Payment Gateway Integration](#br-34-onebook-payment-gateway-integration)
- [BR-35: Library Rack/Shelf Master, Book Transfer & Multi-Library Schema](#br-35-library-rackshelf-master-book-transfer--multi-library-schema)
- [BR-36: Excess Bank Payment with Auto-Generated, Non-Rejectable Refund](#br-36-excess-bank-payment-with-auto-generated-non-rejectable-refund)
- [BR-37: Barcode Generation & Configurable Label-Printer Output](#br-37-barcode-generation--configurable-label-printer-output)
- [BR-38: Library Circulation — Catalogue, Issue/Return, Fines & Periodicals](#br-38-library-circulation--catalogue-issuereturn-fines--periodicals)
- [BR-39: Permission Model V2 — Tiers & Granular Screen-Level Permissions](#br-39-permission-model-v2--tiers--granular-screen-level-permissions)
- [BR-40: Role/User-Customizable Dashboard & Analytics Widget System](#br-40-roleuser-customizable-dashboard--analytics-widget-system)
- [BR-41: Number Sequence Redesign & Roll Number Generation/Assignment](#br-41-number-sequence-redesign--roll-number-generationassignment)
- [BR-42: Faculty Extended Profile & Document-Type Requirements Engine](#br-42-faculty-extended-profile--document-type-requirements-engine)
- [BR-43: Retro Admit (Legacy Direct Admit)](#br-43-retro-admit-legacy-direct-admit)
- [BR-44: MinIO File Storage Migration](#br-44-minio-file-storage-migration)
- [BR-45: Enquiry Payment Credit Application to Student Fee Demands](#br-45-enquiry-payment-credit-application-to-student-fee-demands)
- [BR-46: Designation Master](#br-46-designation-master)
- [BR-47: India Location & Country Master](#br-47-india-location--country-master)
- [BR-48: JWT Revoked-Token Tracking (Logout Denylist)](#br-48-jwt-revoked-token-tracking-logout-denylist)
- [BR-49: INC Nursing Curriculum Compliance — Per-Semester Hours, Electives, Attendance Thresholds & Batches](#br-49-inc-nursing-curriculum-compliance--per-semester-hours-electives-attendance-thresholds--batches)
- [BR-52: Student Promotion / Progression](#br-52-student-promotion--progression)
- [BR-53: Term Lifecycle Confirmation & Overdue Alerting](#br-53-term-lifecycle-confirmation--overdue-alerting)
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
9. In the **student fee collection** payment history, approved refund vouchers are shown as negative reversal entries (`refundNumber`) and include the linked original receipt number for traceability.

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
- **Override:** A holder of the `DOCUMENT_VERIFIED_OVERRIDE` permission (DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN only) may force-replace a `VERIFIED` faculty, admission, or enquiry document in a single action via the existing upload endpoint with `force=true`. This resets the document's status to `UPLOADED` and clears `verifiedBy`/`verifiedAt`, requiring it to be re-verified. Enquiry documents previously had no VERIFIED lock at all (`EnquiryDocumentService.uploadFile` allowed unconditional replacement); the same lock-by-default/override-by-permission model now applies there too, surfaced as a "Force Replace" action on the Document Verification queue screen and on the Enquiry detail page's Documents tab. The override never applies to self-service uploads (`/profile/me/documents/*`), which remain hard-locked once `VERIFIED` regardless of the uploader's permissions.

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

### 29.12 Active Lifecycle Governance (Phase 1: Program/Course)

| Rule | Detail |
|------|--------|
| Program status updates use a dedicated endpoint | `PATCH /programs/{id}/status` with payload `{ status, reason? }` |
| Course active-state updates use a dedicated endpoint | `PATCH /courses/{id}/status` with payload `{ isActive, reason? }` |
| Program cannot be deactivated when active child courses exist | Backend returns `409` with lifecycle code `ACTIVE_CHILD_EXISTS` |
| Program cannot be deactivated when active dependent masters exist | Active `IntakeRule`, `CurriculumVersion`, or `FeeStructureGroup` blocks deactivation (`409 ACTIVE_REFERENCE_EXISTS`) |
| Course cannot be activated while parent program is inactive | Backend returns `409` with lifecycle code `ANCESTOR_INACTIVE` |
| Course cannot be deactivated when active fee structures exist | Backend returns `409` with lifecycle code `ACTIVE_REFERENCE_EXISTS` |
| Inactive records remain editable where form allows status toggle | Existing BR-29.8 rule remains valid |
| Lifecycle conflicts must return structured conflict payload | Include `status`, `message`, `code`, `entity`, `entityId`, `timestamp` |

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
| 2026-07-17 | BR-53, BR-28 | **Term Lifecycle Confirmation & Overdue Alerting added:** advancing a term's status (`PLANNED→OPEN`/`OPEN→LOCKED`) now requires confirming a consequence dialog first. New daily `AcademicTermAlertService` job raises an in-app alert when a term is still `PLANNED` within 14 days of its start date, auto-resolving once the admin acts. First real slice of BR-28's notification-sending backend — new `notifications`/`notification_dismissals` tables (broadcast-style alerts, per-user dismissal), `GET /notifications/feed`/`POST /notifications/{id}/dismiss`, and the toolbar bell (previously a dead hardcoded badge) now wired to a real feed. New `academicTermAlerts` preference category, gated by `ACADEMIC_YEAR_MANAGE`. Migration V287. | — |
| 2026-07-17 | BR-52 | **Student Promotion select-step simplified:** picking a cohort was previously followed by two full academic-year → term cascades (From and To), meaningless repeat clicking for the common case. New `GET /student-promotions/active-terms?cohortId=` auto-detects the term instance(s) a cohort currently has `ENROLLED` students in (usually exactly one) and `GET /student-promotions/suggested-next-term?fromTermInstanceId=` auto-suggests the destination — the cascade now only appears as a manual fallback (new cohort with no enrollment yet, no next term created, or opted into via "Choose different terms manually"). No schema change; new repository query methods only (`AcademicYearRepository.findByStartDateGreaterThanOrderByStartDateAsc`, `StudentTermEnrollmentRepository.findByCohortIdAndStatus`). | — |
| 2026-07-16 | BR-52 | **Student Promotion / Progression added:** the first mechanism in the system for moving an existing student to the next academic year/term, following the real INC/Dr. MGR Medical University model (subject-wise arrears carried forward, cleared only by Final Year; max duration = double program length; per-subject attendance detention; mandatory preview before an irreversible bulk commit). Discovered and removed a pre-existing blind auto-advance (`TermInstanceService`'s `OPEN` transition previously called `generateEnrollmentsForTermInstance`/`generateRegistrationsForTermInstance`/`generateDemandsForTermInstance` unconditionally for every active student, with no eligibility check) — Promotion is now the sole owner of that rollover. `ExamResult` gains a persisted `outcome` (PASS/FAIL, external-marks-only for v1 — CIA marks don't exist yet). New `student_promotion_decisions` audit table; `STUDENT_PROMOTION_VIEW`/`MANAGE` permissions; new Academics nav screen. Migrations V284–V286. | — |
| 2026-07-16 | BR-49 | **Curriculum Version's course scope is now mandatory, removing the program-wide pattern entirely:** `curriculum_versions.course_id` (added nullable in V264 to let one version apply program-wide, e.g. to a single-course program like plain BSc Nursing) is now `NOT NULL` — every curriculum version must be tied to one specific course, eliminating the ambiguity of "which curriculum applies to this cohort" that the program-wide/course-scoped fallback created. Existing NULL rows are backfilled to the program's lowest-id course (V282; all pre-existing data was test/dummy, confirmed safe). As a direct consequence, the row-level override on `curriculum_term_courses.course_id` (V278 — let one program-wide version carve out course-specific exceptions for individual subjects, e.g. MSc Nursing Adult vs Child sharing a version) became pure duplication of the parent version's course and was dropped (V282), along with the Curriculum Map screen's "Restrict to Course (optional)" per-subject UI. `CourseOfferingServiceImpl.resolveActiveCurriculumVersion` no longer has a program-wide fallback branch — it resolves a cohort's curriculum version by program+course only. The New/Edit Curriculum Version form's Course field is now required (previously optional with a "Program-wide" choice). | — |
| 2026-07-16 | BR-49 | **Curriculum Version list screen rebuilt to the standard master-list pattern:** previously gated behind a mandatory program dropdown before showing anything, card-only, with an inline expand-under-card Clone form and no Delete at all. Now shows all versions across programs by default (Program is an optional filter, alongside a new Status filter), with dual card/table view (`cms-view-toggle`, matching Program/Course/Subject), server-side search/sort/pagination, and cards/table rows showing a term/subject content summary. Clone moved to a `cms-flyout-panel` (source-version summary + live name-uniqueness check + single confirm step) usable from either view; inline expand-under-card removed entirely. Added a Delete action, blocked with a tooltip when the version has course offerings against it (see the Clone Change Log row below for the underlying delete/usage-check endpoint). Controller `@PreAuthorize` also rewired from the blanket `CURRICULUM_MANAGE` to the granular `CURRICULUM_CREATE`/`CURRICULUM_EDIT`/`CURRICULUM_DELETE` permissions (already existed unused in the DB since V242; every role holding `CURRICULUM_MANAGE` already had all three via that migration's backfill, so no access changed). The standalone New/Edit Version form also gained the same real-time name-uniqueness check. | — |
| 2026-07-16 | BR-49 | **Curriculum Version Clone now deep-copies content:** `POST /curriculum-versions/{id}/clone` previously created an empty version stub sharing only the program/course — every `curriculum_term_courses` row, elective group, and attendance threshold had to be rebuilt by hand after every clone, which defeated the point of "cloning" a version to iterate on it for a new academic year. `CurriculumVersionService.cloneCurriculumVersion` now copies all term/subject mappings, elective groups (copied first to build an old→new id map for remapping), and per-mapping attendance thresholds into the new version. Also added: paginated/searchable/sortable `GET /curriculum-versions/page` and a `name-exists` uniqueness endpoint (scoped per program+course), and a `DELETE /curriculum-versions/{id}` that blocks with 409 if course offerings reference the version. No schema change; existing tables only. | — |
| 2026-07-13 | BR-48 | **Documentation gap backfill** (retroactive — no code change): documents the JWT revoked-token denylist (`revoked_tokens`, `RevocationJwtValidator` chained into JWT decoding, `POST /auth/revoke` on logout, hourly purge; V243) — self-service only, no admin-forced logout of another user's session exists. | — |
| 2026-07-13 | BR-47 | **Documentation gap backfill** (retroactive — no code change): documents the India Location & Country Master (Country → State → District hierarchy, V149-V151/V158-V159) — only `country` on enquiries/faculty/students is an actual FK; state/district remain free text populated from the cascading dropdowns. | — |
| 2026-07-13 | BR-46 | **Documentation gap backfill** (retroactive — no code change): documents the Designation Master (replaced a hardcoded enum, V201/V202/V229) — conforms to BR-32's lifecycle contract but was never added to that BR's scope table. | — |
| 2026-07-13 | BR-45 | **Documentation gap backfill** (retroactive — no code change): documents automatic enquiry pre-payment credit application against student semester fees at collection time (`enquiry_credit_applications`, V209/V211), plus the one-time V187 backfill that preceded it. | — |
| 2026-07-13 | BR-44 | **Documentation gap backfill** (retroactive — no code change): documents the MinIO file storage migration (`StorageService`/`MinioStorageService`, `storage_key` columns, admin-only `/admin/migrate-storage` endpoint, V249) — the backend now hard-fails to boot without a reachable MinIO endpoint, and old `bytea` columns are deliberately kept as a fallback until manually verified. | — |
| 2026-07-13 | BR-43 | **Documentation gap backfill** (retroactive — no code change): documents Retro Admit / Legacy Direct Admit (`POST /students/retro-admit`) — atomic Student+Enquiry+Admission creation bypassing the enquiry pipeline, with optional FIFO-allocated historical fee/payment backfill, distinct from BR-31's bulk Excel import. | — |
| 2026-07-13 | BR-42 | **Documentation gap backfill** (retroactive — no code change): documents faculty's extended profile fields (identity/bank/address/experience, V93/V199), the `faculty_documents` table, and the document-type requirements engine (designation/speciality/qualification OR-matched rules, V126) that BR-26's review badges and verification locks are built on top of. | — |
| 2026-07-13 | BR-41 | **Documentation gap backfill** (retroactive — no code change): documents roll number auto-generation/manual assignment (`RollNumberGeneratorService`, V111) and the number-sequence engine redesign (`number_series_definitions`/`number_sequence_counters` replacing `application_number_sequences`, V244–V246) that BR-27's admission numbers already rely on. | — |
| 2026-07-13 | BR-40 | **Documentation gap backfill** (retroactive — no code change): documents the role/user-customizable Dashboard & Analytics widget system (four escalating widget tiers, per-role default + per-user override layout, `DASHBOARD_CUSTOMIZE` permission) shipped across V119–V190 but never given a BR. | — |
| 2026-07-13 | BR-38, BR-39 | **Documentation gap backfill** (retroactive — no code change): BR-38 documents the base Library circulation module (catalogue, issue/return/renewal, fines, periodicals) shipped at V196–V198/V250 but never given a BR; BR-39 documents the Permission Model V2 redesign (tiers + granular per-operation screen permissions, V241/V242/V247) that BR-35/36/37/38 already assume exists. | — |
| 2026-07-15 | BR-37 | **Label design revamp:** every barcode label (PNG preview, PDF batch sheet, and ZPL for all three print transports) now renders four rows — "SKSCON" institution header, barcode, truncated book/journal title, and a footer combining accession number + shelf location (e.g. "2002  ·  C3 / R2"). Shelf location is book-only (BR-35's rack/shelf hierarchy was never extended to periodicals, so journal labels omit that part of the footer). `LibraryBarcodeService.LabelItem` gained a `shelfLocation` field; `LibraryBookController.toLabelItem` populates it from the book's existing `rackName`/`shelfName`, matching the "`{rackName} / {shelfName}`" format already used on the Book Catalogue's Shelf column. Long titles are truncated with an ellipsis (character-budget heuristic for ZPL/PDF, exact pixel-width measurement for the PNG preview) since the layout must fit uniformly whether a ZPL row holds 1, 2, or 4 labels across (each cell renders independently at the fixed single-label width, so the per-cell template needed no per-count variation). | — |
| 2026-07-15 | BR-37 | **Post-implementation fixes (first real-hardware test against a USB Zebra ZD230):** (1) `LibrarySettingUpdateRequest.settingValue` was `@NotBlank`, rejecting the intentionally-blank `barcode_printer_ip` before `LibrarySettingService.updateByKey`'s own blank-tolerant logic ever ran — any Library Settings save failed with 400 whenever the printer IP was empty (i.e. always, outside NETWORK mode). Changed to `@NotNull` (still rejects a missing field, allows empty string). (2) `PrintService.printElement` now accepts an optional `pageSizeMm` override so BROWSER-mode printing pins the print page to the configured label width/height instead of defaulting to A4/Letter; the barcode preview dialog passes it through. (3) `PrintService.printElement` fired `window.print()` on iframe-load/500ms-timeout without waiting for `<img>` elements (e.g. a blob: URL barcode PNG) to finish decoding — invisible on a normal browser (the print-preview dialog gives it time), but on a kiosk-mode Chrome profile (`--kiosk --disable-print-preview`, used for unattended label/receipt printing) the print fires instantly and silently rasterized a blank label. Now waits for all images in the print document (2s cap) before printing. (4) Confirmed Zebra's "Browser Print" agent (LOCAL_AGENT mode) has no Linux build — on a Linux circulation-desk machine, a USB Zebra printer must go through CUPS via BROWSER mode instead; LOCAL_AGENT remains correct only for Windows/Mac. (5) Nginx CSP's `connect-src 'self'` blocked LOCAL_AGENT's `fetch('http://localhost:9100/write')` call to the local Browser Print agent on Windows/Mac deployments — added `http://localhost:9100` to `connect-src` in `nginx.conf`/`nginx.conf.template`. | — |
| 2026-07-13 | BR-37 | Added a `barcode` column (distinct from `accession_number`) to books/periodicals with Code128 PNG/PDF generation, plus a configurable print-transport setting (`barcode_printer_mode`: BROWSER default / NETWORK / LOCAL_AGENT) so the same "Print" button can stream plain ZPL to a networked or USB-attached thermal label printer instead of the browser print dialog. Migrations V260, V263. | — |
| 2026-07-08 | BR-35 | Replaced `library_books.shelf_location` (free text) with a real Library → Rack → Shelf hierarchy; added Rack/Shelf master screens, book transfer (single + bulk, blocked while `ISSUED`) with audit history, and converted the Search Catalogue tab from unpaginated client-side filtering to server-side pagination with a shelf filter. Migrations V251–V256. | — |
| 2026-06-23 | BR-34 | **Rewrote OneBook integration against OneBook's real published API spec** (previously placeholder field names/auth, now confirmed wrong): JWT auth flow replaces HTTP Basic; outbound create payload rewritten to OneBook's invoice/document-register shape (`payeeType: OTHERS`, `invoiceNumber`/`documentNumber` = a newly generated refund/commission/disbursement number, `documentId` = source entity PK, `paymentRegisterDocumentType: PAYMENT`/`REFUND`, `transactionType: CREDIT` always); removed response-body id/success parsing since the real synchronous response carries neither — success is now HTTP-status-only. Replaced the single placeholder `/webhooks/onebook/payment-status` webhook with the two real OneBook callbacks (`posting-track-update`, `posting-track-completion`), both correlated by the generated `invoiceNumber` rather than an echoed `referenceId`. Added `ApplicationNumberSequenceService.nextCommissionNumber`/`nextDisbursementNumber`; `FeeRefund.refundNumber`/`Enquiry.commissionNumber`/`ScholarshipDisbursement.disbursementNumber` now reuse the number generated at push time instead of regenerating on completion. Supplier Master Sync (pharmacy-only piece of the same spec) explicitly out of scope. Migration V236. | — |
| 2026-06-23 | BR-34 | OneBook outbound push now parses the synchronous response body for OneBook's own id and a success/failure indicator (previously only an HTTP-exception check, which would have silently treated a logical "rejected" response as transmitted). The id is stored as `onebookTxnId` and is now also a valid correlation key for the inbound webhook — `OneBookWebhookService.process` and `OneBookPaymentRequestRepository.findByOnebookTxnId` look up by `transactionId` when `referenceId` is absent or unmatched, since OneBook's callback may use only its own id rather than echoing ours back. Response field names (`id`/`transactionId`/`status`/`success`) are best-guess placeholders pending OneBook's actual API docs — flagged in BR-34 for confirmation. | — |
| 2026-06-23 | BR-34 | Documented OneBook payment gateway integration (previously undocumented though already built): outbound push for commission/refund/scholarship payment registers, inbound `/webhooks/onebook/payment-status` callback, config keys, permissions. Webhook updated to accept either a single payment register or a JSON array of registers in one call (OneBook's stated contract — "payment registers only, single or list"), processing each independently and returning a per-register result array. Flagged scholarship rejection as a known lifecycle gap (no visible status/retry, unlike commission and refund) pending a design decision. | — |
| 2026-06-22 | BR-32, BR-33 | Added `Institution` master (sister-concern institutions of SKSCON) with standard CRUD/uniqueness/lifecycle pattern (added to BR-32 scope). `StaffReferrer.institution` (free text) replaced with `institution_id` FK; added `employeeCode` field. Name and employee code uniqueness rescoped from global to per-institution. Migrations V232–V234. | — |
| 2026-06-20 | BR-26 | Added `DOCUMENT_VERIFIED_OVERRIDE` permission (DEV_ADMIN/SUPPORT_ADMIN/ADMIN/COLLEGE_ADMIN only, migration V230) allowing an authorized reviewer to force-replace a `VERIFIED` faculty, admission, or enquiry document via `force=true` on the existing upload endpoint, resetting it to `UPLOADED` for re-verification. Also added the previously-missing VERIFIED lock to `EnquiryDocumentService.uploadFile` (it had none) so enquiry documents now follow the same lock/override model; "Force Replace" surfaced on the Document Verification queue screen and the Enquiry detail Documents tab. Self-service uploads remain hard-locked once `VERIFIED`. | — |
| 2026-06-09 | BR-7 | Student fee payment history now includes approved refund vouchers as negative reversal entries (with original receipt reference) so cashflow and reversals are visible in one timeline. | — |
| 2026-06-03 | BR-31 | **Student Data Import — Legacy Migration:** Added full BR for bulk student migration. Documents 9-item pre-conditions checklist (programs, courses, academic years, cohorts + seat allocations, fee structures, fee states, WALK_IN referral type, country seed); recommended course-wise import strategy with step-by-step workflow and rationale (error isolation, cohort verification, fee structure uniformity per course); fee history multi-payment row explanation (`year_1_fee`..`year_6_fee` = annual fee split per programme year, not per-payment; subsequent rows per student leave fee structure columns blank); 44-column Students sheet with all personal/demographic/family/address/registration/classification fields; Qualifications sheet (8 fields); Fee History sheet (16 fields); Step 2 defaults panel including `admission_category`; cohort assignment logic (error if cohort missing for course+AY); fee state inference from address state; boolean strict-format rule (`TRUE`/`FALSE` only); unique number conflict check for 4 registration numbers; 3-pass import execution flow; post-import state; and permissions (`IMPORT_DATA`). | — |
| 2026-05-25 | BR-10 | Admission/student document screens now prioritize missing required documents at the top, allow uploading newly required documents for existing admissions, and preserve previously collected documents that were later removed from program requirements as "not currently required" records. | — |
| 2026-05-25 | BR-10 | Admission printable template updated for one-page A4 print preview: Academic Qualifications are excluded from official View/Print/Download output, print text readability is improved, the document checklist remains two-column, and the submitted `PASSPORT_PHOTO` document is the source for the admission-form passport photo. | — |
| 2026-05-25 | BR-10 | Admission form view/print/download standardized on one printable template; document checklist now renders in two balanced columns using `ceil(document count / 2)` rows (20→10, 23→12, 31→16), and the download action generates PDF instead of HTML. | — |
| 2026-05-21 | BR-30, BR-3, BR-6, BR-23 | **BR-30 post-implementation fixes (review pass):** (1) Enquiry form — gender change now re-fetches fee; fee banner shows contextual "what's still needed" text; programs without courses correctly trigger fee calculation (`courseId` removed from null-guard in `applyAuthoritativeFees`); `updateCourseValidator` triggers fee load for no-course programs; `tryLoadFeeGuideline` guard prevents misleading "not found" when courses exist but none is selected. (2) Fee finalization — Quota filter dropdown added to toolbar; `filteredEnquiries` includes quota in text search; `applyEqualSplitFallback` uses actual program `durationYears` (not hardcoded 4); `discountReason` signal synced to FormControl; "Fee Basis" group label + divider added to info panel. (3) API layer — `applyAuthoritativeFees` error message now shows fee state name (not raw ID); `GET /fee-structures/grouped` extended to accept `quota`, `feeStateId`, `gender`, `studentType` as optional filters; `DataIntegrityViolationException` handler improved with specific messages for `uq_fee_structure_group` and `uq_fee_structure_group_fee_type` constraint violations. | — |
| 2026-05-21 | BR-30, BR-1, BR-3, BR-6, BR-12 | **Multi-dimension fee structure (BR-30):** Fee structure uniqueness key extended to `program + academicYear + course + quota + feeState + gender + studentType`. New `FeeState` master (Tamil Nadu = default, Other State = fallback); new `FeeStructureGroup` entity; `FeeStructure` items linked to group. `Enquiry` gains `admissionQuota` and `feeState` FK. Fee lookup on enquiry form uses 6 fields; state auto-derived from address; fallback to Other State if no exact match; submission blocked if no configuration found. Enquiry form adds Admission Quota dropdown (default: Management). Fee finalization shows all 4 dimensions as read-only context rows. Migrations V165–V168. BR-1 uniqueness rule amended; BR-3 fee-load flow rewritten; BR-6 finalization amended; BR-12 studentType noted as one of 4 dimensions. | — |
| 2026-05-18 | BR-29 | Added UI Validation & Form Behaviour Standards: boundary value / empty-space rules, code-field no-space rule, case-insensitive unique validation, dropdown/autocomplete UX, date-picker range rules, submit multi-click prevention, update button label, inactive/delete protection, table ordering, pagination spinner, and search scope. Implemented `cms-validators.ts` with `noConsecutiveSpaces`, `noInternalSpaces`, `trimmedMinLength`, `cmsFieldError`, `stripSpaces`, `collapseSpaces`. Applied across all master-data form components. | — |
| 2026-06-18 | BR-29.12 | Added Phase 1 Active Lifecycle Governance for Program/Course: dedicated status endpoints, parent-child deactivation guards, active dependency checks, and structured lifecycle conflict responses (`ACTIVE_CHILD_EXISTS`, `ACTIVE_REFERENCE_EXISTS`, `ANCESTOR_INACTIVE`). | — |
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

> **Status:** Preferences CRUD (`GET`/`PUT /notifications/preferences`) and in-app delivery are live for one category. [BR-53](#br-53-term-lifecycle-confirmation--overdue-alerting) shipped the first real notification-sending slice — `academicTermAlerts`, in-app only, via a real `notifications` feed/dismiss backend and a working toolbar bell. All other categories (`feeAlerts`, `admissionUpdates`, `examSchedule`, `attendanceAlerts`, `systemAnnouncements`, `documentReminders`/`profileReminders`) still have no sending trigger wired up, and the `EMAIL`/`BOTH` channels remain unimplemented for every category.

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

## BR-32: Master Lifecycle Status Management

### Business Rule

Master screens must use a unified lifecycle status contract for activate/deactivate/reactivate transitions.

### Scope

- `Scholarship Type`
- `India Location (Country / State / District)`
- `Blood Group`
- `Community`
- `Referral Type`
- `Agent`
- `Staff Referrer`
- `Institution`

### API Contract Standard

- Status updates are performed via `PATCH /{resource}/{id}/status`.
- Request payload shape: `{ "isActive": boolean, "reason": string | null }`.
- Response payload shape: `{ "id": number, "isActive": boolean, "updatedAt": timestamp }`.

### Lifecycle Guard Rules

- Deactivation is blocked with `409 CONFLICT` and `ACTIVE_REFERENCE_EXISTS` when active usage exists.
- Guarded references include:
  - `Referral Type` -> `Enquiry`
  - `Agent` -> `Enquiry`, `Agent Commission Guideline`, `Commission Payout`
  - `Staff Referrer` -> `Commission Payout`
- Reactivation does not auto-reactivate dependents.

### Backward Compatibility

- Existing `/deactivate` and `/reactivate` endpoints remain available but delegate to the same status-update service logic.

---

## BR-33: Institution Master & Staff Referrer Institution Scoping

### Business Rule

Staff Referrers may only be linked to a known sister-concern institution of SKSCON, selected from an admin-managed `Institution` master — not entered as free text. A referrer's `name` and `employeeCode` only need to be unique **within their institution**, not globally.

### Scope

- New `Institution` master (`institutions` table; `name`, `code`, `description`, `isActive`) — same CRUD/uniqueness/lifecycle pattern as other masters (see BR-32).
- `StaffReferrer.institution` (free-text `VARCHAR`) replaced with `institution_id` FK to `institutions` (`NOT NULL`).
- `StaffReferrer.employeeCode` added (`NOT NULL`).

### Uniqueness Rule

- `(institution_id, LOWER(name))` and `(institution_id, LOWER(employee_code))` are each unique. The previous global unique-name constraint on `staff_referrers` is removed.
- `/staff-referrers/name-exists` and `/staff-referrers/employee-code-exists` both require an `institutionId` query param to scope the check.

### Migration Notes

- Migrations V232 (institutions table + `INSTITUTION_VIEW`/`INSTITUTION_MANAGE` permissions), V233 (`institution_id` FK, backfilling any pre-existing free-text values into new `Institution` rows, with an `Unspecified` inactive fallback institution for blank values), V234 (`employee_code` column, backfilling pre-existing rows with `LEGACY-<id>` placeholders).
- Deactivated institutions remain visible (read-only) on existing referrer records but are not selectable for new ones.

---

## BR-34: OneBook Payment Gateway Integration

### Business Rule

OneBook is the college's external accounting/payment application. Three payment types originate in OneCMS and are routed through OneBook for actual money movement: **commission payouts** (to agents, staff referrers, faculty referrers), **fee refunds**, and **scholarship disbursements**. OneCMS never marks one of these as paid on its own — it transmits a payment register to OneBook and waits for OneBook to report the outcome back via callbacks into OneCMS's own endpoints. The integration is config-gated and disabled by default until a college's real OneBook credentials are entered.

> **Contract source:** Rewritten against OneBook's actual published "Payment Register" API spec (a generic contract used across integrating applications — OnePharmacy is its documented example client; OneCMS uses the same endpoints with its own field values). The original build (pre-2026-06-23) used best-guess placeholder field names and HTTP Basic Auth; both are now confirmed wrong and replaced below. Supplier Master Sync (a pharmacy-only piece of the same spec) is explicitly out of scope for OneCMS.

### Authentication

Every outbound call requires a fresh JWT — `OneBookIntegrationService.authenticate()` calls `POST {onebook.api_url}/authserver/api/auth` with `{Username, password, branchId, organizationId, zoneName}` and reads `token` from the response. **Not cached** — a new token is requested on every push (chosen for simplicity over the alternative of caching the 24h-valid token).

### Outbound — OneCMS Creates a Payment Register

| Payment Type | Triggered From | Service Method |
|---|---|---|
| Commission | Commission Explorer — approve / push to OneBook | `OneBookIntegrationService.pushCommissionPayment` |
| Fee Refund | Fee Refund List — push to OneBook | `OneBookIntegrationService.pushRefundPayment` |
| Scholarship Disbursement | Scholarship Application — disburse via OneBook | `OneBookIntegrationService.pushScholarshipPayment` |

Each push:
1. Generates an `invoiceNumber` up front — the refund number (`RFD-yyyy-NNNNN`), a new commission number (`COM-yyyy-NNNNN`), or a new disbursement number (`DSB-yyyy-NNNNN`) via `ApplicationNumberSequenceService`. This number is generated **once**, stored on `OneBookPaymentRequest.invoiceNumber`, sent to OneBook, and reused verbatim as the domain entity's own number (`FeeRefund.refundNumber`, `Enquiry.commissionNumber`, `ScholarshipDisbursement.disbursementNumber`) only once OneBook confirms the payment — never regenerated a second time.
2. Creates a `OneBookPaymentRequest` row ("payment register") — `PENDING` status, recipient bank details, amount, the generated `invoiceNumber`, plus an internal-only `referenceId` (`OB-yyyyMMdd-XXXXXXXX`, used only in OneCMS's own UI/logs, never sent to or matched against OneBook).
3. Calls `POST {onebook.api_url}/one-book/api/payment-registers-add-from-other-applications` with `Authorization: Bearer {jwt}`, body wrapped in a one-element JSON array per OneBook's documented shape. Field mapping:

   | OneBook Field | OneCMS Value |
   |---|---|
   | `applicationName` | `onebook.app_name` (e.g. `ONECMS`) |
   | `payerName` | `onebook.paper_name` (e.g. `SKS College Of Nursing`) |
   | `payeeType` | Always `OTHERS` — agents/staff/faculty/students have no supplier-master equivalent in OneBook |
   | `sourcePayeeId`, `supplierId` | Recipient's own entity id (agent/staff/faculty/student id) — reused for both since there's no supplier master to reference |
   | `payeeName` | Recipient's name |
   | `invoiceNumber`, `documentNumber` | The generated refund/commission/disbursement number (same value for both) |
   | `documentId` | The source entity's own primary key (enquiry id / refund id / scholarship application id) |
   | `paymentRegisterDocumentType` | `PAYMENT` for commission and scholarship; `REFUND` for fee refunds |
   | `transactionType` | Always `CREDIT` |
   | `netBillAmount`, `payableAmount` | The payout amount; `paidAmount` is `0.00` at creation |
   | `invoiceDate`, `dueDate` | Push date (no separate due-date concept for these payout types) |
   | `invoiceFilePath` | Empty string — no invoice file exists for these payout types |
   | `branchId`, `organizationId` | `onebook.branch_id`, `onebook.org_id` |
   | `createdBy`, `modifiedBy` | The approving/disbursing user |

4. **The synchronous response carries no register id** — just an ack (`{"message": "..."}`). Success/failure is judged purely on HTTP status: any 2xx is `TRANSMITTED`; any non-2xx or network exception is `FAILED`. The register's actual OneBook-assigned id and final payment outcome arrive later via the two inbound callbacks below.
5. **Bank-detail guard:** the push is blocked before any API call if the recipient is missing name, account number, bank name, or IFSC. Enquiry-sourced refunds are blocked entirely since enquiry records carry no bank details — those must be refunded manually.

**Out of scope for this round:** OneBook's edit/delete-register and fetch-by-id endpoints are not wired up — there is no existing flow anywhere in OneCMS that cancels or edits a commission/refund/scholarship payout once it's already been transmitted (rejection is backend- and UI-gated to `PENDING` only — see `CommissionExplorerService.reject()`), so there is nothing to call them from yet. Revisit if a post-push cancellation feature is ever added.

### Inbound — OneBook Calls Back Into OneCMS

Unlike a single generic webhook, OneBook's real contract calls back into **two different endpoints OneCMS exposes**, both authenticated the same way as before — `X-OneBook-Secret` header checked against `onebook.webhook_secret`:

| Endpoint | When OneBook Calls It | Payload (key fields) |
|---|---|---|
| `PUT /webhooks/onebook/posting-track-update` | Immediately after accepting a new payment register | `invoiceNumber`/`documentNumber` (correlation key), `oneBookPaymentRegisterId`, `status`, `comment` |
| `PUT /webhooks/onebook/posting-track-completion` | Once the payment itself is completed or fails | `invoiceNumber`/`documentNumber`, `status`, `paymentNumber`, `bankName`, `paymentMode`, `transactionNumber`, `paymentDate`, `paymentBy`, `batchNumber` |

Both accept a JSON array body (OneBook's documented shape, even for a single register) and reply with a flat `{"message": "true"}` ack, matching OneBook's documented response — not a per-entry result array. Each entry in the array is still processed independently server-side (`OneBookWebhookService.processPostingTrackUpdate` / `processPostingTrackCompletion`); an unmatched entry is logged and skipped rather than failing the whole call.

**Correlation key:** matched purely by `invoiceNumber` (`OneBookPaymentRequestRepository.findByInvoiceNumber`) — this is the one identifier OneCMS generates that's guaranteed unique and is the same value sent as both `invoiceNumber` and `documentNumber` in the original create call.

On `posting-track-completion`, OneBook's reported status maps to an internal status, then propagates to the originating record:

| OneBook Status | Internal Status | Effect |
|---|---|---|
| `SUCCESS`, `COMPLETED`, `PAID` | `PAID` | Commission → enquiry `commissionPaymentStatus = PAID`, `Enquiry.commissionNumber` set to the generated invoice number. Refund → refund finalized (`APPROVED`), refund/installment rows stamped with payment date, mode, transaction reference; `FeeRefund.refundNumber` set to the same invoice number generated at push time (not regenerated). Scholarship → `ScholarshipDisbursement` record created from the register's stored metadata (academic year, term, remarks), `disbursementNumber` set to the same invoice number. |
| `FAILED`, `REJECTED`, `CANCELLED`, `ERROR` | `FAILED` | Commission → `commissionPaymentStatus = FAILED` (visible/retryable in Commission Explorer). Refund → `status = PAYMENT_FAILED` (visible/retryable in Fee Refund List). Scholarship → **logged only; not yet surfaced or retryable in any screen — see Known Gap below.** |
| Anything else | `PROCESSING` | No status change on the source record; awaiting a further callback. |

The `OneBookPaymentRequest` register row always records the raw OneBook status, payment number, bank name, payment mode/by, batch number, paid date, and raw response JSON regardless of payment type.

### Known Gap — Scholarship Rejection Has No Visible Lifecycle End

Commission and refund both have a list screen that shows `FAILED` status and lets the user retry the push. Scholarship disbursement does not: a `FAILED`/`REJECTED` callback for a scholarship register is logged server-side only — there is no field on `StudentScholarship` or `ScholarshipDisbursement` that reflects it, and no screen lists pending/failed OneBook scholarship registers. This needs a design decision (where the failure surfaces, whether a retry action is added) before it can be called complete — tracked for follow-up, not fixed as part of this entry.

### Configuration

| Config Key | Purpose |
|---|---|
| `onebook.enabled` | Master switch. `false` by default — commissions/refunds/scholarships are settled manually until turned on. |
| `onebook.api_url`, `onebook.username`, `onebook.password` | OneBook API base URL and auth-server credentials. |
| `onebook.org_id`, `onebook.branch_id`, `onebook.app_name`, `onebook.paper_name`, `onebook.zone_name` | Identifiers OneBook uses to attribute the payment to this institution and authenticate the JWT request. |
| `onebook.webhook_secret` | Shared secret OneBook sends in `X-OneBook-Secret` on both inbound callbacks. |
| `onebook.integration_date` | Informational/audit only. |

Configured from Settings → Integrations (`IntegrationsSettingsComponent`); requires `SYSTEM_CONFIG_MANAGE`.

### Permissions

- `COMMISSION_MANAGE` — approve/reject a commission and trigger the OneBook push.
- `COMMISSION_SETTLE` — record the actual payout for an already-approved commission, separate from approve/reject authority; granted to DEV_ADMIN, SUPPORT_ADMIN, ADMIN, COLLEGE_ADMIN.
- Refund/scholarship pushes use the existing refund (`FEE_REFUND_APPROVE` family) and scholarship disbursement (`SCHOLARSHIP_DISBURSE`) permissions — no new permission was needed for those.

### Migration Notes

- V223 seeded the integration config with a Bearer-token/tenant-ID model; V226 replaced it with username/password + org/branch/app/paper; V236 added `onebook.zone_name` and the columns needed for the real contract (`invoice_number`, `onebook_payment_number`, `onebook_bank_name`, `onebook_payment_by`, `onebook_batch_number` on `onebook_payment_requests`; `commission_number` on `enquiries`; `disbursement_number` on `scholarship_disbursements`).
- V224 created `onebook_payment_requests`; V225 added request-metadata JSON (used to recover academic year/term/remarks for scholarship disbursement creation on the success callback).
- V227 added the 6 bank-detail fields to `Student` required by the bank-detail guard.
- V231 added commission rejection reason/by/at to `Enquiry` plus the `REJECTED` `CommissionPaymentStatus` value.
- No migration was needed for the new commission/disbursement number sequences — `ApplicationNumberSequenceService` auto-creates a sequence row on first use of a new series code.

---

## BR-35: Library Rack/Shelf Master, Book Transfer & Multi-Library Schema

### Business Rule

A book's physical location is tracked as a real 3-level hierarchy — **Library → Rack → Shelf** (a tier within a rack, e.g. Top/Middle/Bottom) — instead of the free-text `shelf_location` field used at initial Library module launch (V196). Staff can create/manage Racks and Shelves, filter the Book Catalogue and Search Catalogue by shelf, and transfer one or many books to a different shelf with a full audit trail. A book currently `ISSUED` cannot be transferred (mirrors the existing rule blocking deletion of an issued book) — it must be returned first.

Only one physical `Library` exists today (seeded "Main Library"), but the schema is deliberately multi-library-ready (`library_id` FKs throughout) so a second library can be added later without another structural migration. There is no Library management screen yet — with exactly one row, a CRUD UI would have no purpose; `GET /libraries` exists only to populate the Library dropdown in the Rack form and the Transfer dialog.

### Scope

- New `Library` master (`libraries` table; `name`, `code`, `address`, `isActive`) — schema + seed row only, no CRUD screen (see above).
- New `LibraryRack` master (`library_racks` table; `library_id` FK, `name`, `code`, `description`, `isActive`) — full CRUD screen at `/library/racks`, same pattern as other masters (async name/code uniqueness, scoped per-library).
- New `LibraryShelf` master (`library_shelves` table; `rack_id` FK, `name`, `code`, `description`, `isActive`) — full CRUD screen at `/library/racks/:rackId/shelves`, nested under its parent Rack, uniqueness scoped per-rack.
- `library_books.shelf_location` (free text) replaced by `library_id` (`NOT NULL`) and `shelf_id` (nullable — a book can belong to a library without yet being assigned a specific shelf) FKs.
- New `library_book_shelf_transfers` audit table logging every transfer (old/new library, rack, shelf, who, when, notes) — modeled on `student_program_transfers` (BR pattern from student program transfers).
- Book Catalogue (`/library/books`) and Search Catalogue (the "Search Catalogue" tab in My Library, `/library/my-issues`) both gained a Rack → Shelf cascading filter. Search Catalogue was also converted from an unpaginated full-catalogue client-side fetch/filter to real server-side pagination (`GET /library/books/page`) — the previous implementation loaded every `AVAILABLE` book into the browser on every visit, which does not scale as the catalogue grows.
- Book Catalogue gained bulk selection (checkboxes) and a "Transfer Selected" toolbar action, plus a per-row "Transfer" action (disabled while `ISSUED`), both opening a shared transfer dialog (Library → Rack → Shelf cascading pick + notes). Bulk transfer reports partial success — books that are `ISSUED` are skipped with a reason, the rest still transfer.

### Permissions

- `LIBRARY_SHELF_VIEW` / `LIBRARY_SHELF_MANAGE` — gate the Rack and Shelf master screens (both masters share one permission pair since they're one conceptual screen). Staff-only (LIBRARIAN, COLLEGE_ADMIN, ADMIN), not granted to FACULTY/STUDENT.
- `LIBRARY_TRANSFER` — gates the transfer action (new, dedicated permission rather than reusing `LIBRARY_CATALOGUE_MANAGE`, so transfer rights can be granted independently of general catalogue editing).
- The unpaginated `GET /library/racks` and `GET /library/shelves` (used only to populate filter/picker dropdowns) are instead gated on the existing broad `LIBRARY_CATALOGUE_VIEW`/`LIBRARY_ISSUE_VIEW` permissions students and faculty already hold, so Search Catalogue's shelf filter works for them without granting the new staff-facing permission.

### Migration Notes

- V251 (`libraries` table + seed "Main Library"), V252 (`library_racks` table), V253 (`library_shelves` table).
- V254 — the data-safe backfill: since books were already imported into production with real `shelf_location` values before this feature existed, this migration auto-creates one Rack per distinct existing `shelf_location` value (under Main Library) with a single default "General" shelf tier, backfills every book's `shelf_id`/`library_id` accordingly, then drops the old `shelf_location` column. Staff can re-organize into finer rack/shelf tiers afterwards via the new master screens. **Must be run against a fresh `pg_dump` backup, per the project's production data safety rule, since it transforms existing production data.**
- V255 (`library_book_shelf_transfers` audit table), V256 (`LIBRARY_SHELF_VIEW`/`LIBRARY_SHELF_MANAGE`/`LIBRARY_TRANSFER` permissions, auto-assigned to roles already holding `LIBRARY_CATALOGUE_MANAGE`, plus the mandatory DEV_ADMIN/SUPPORT_ADMIN catch-all sync).

---

## BR-36: Excess Bank Payment with Auto-Generated, Non-Rejectable Refund

### Business Rule

Payment collection has always hard-capped the amount at total outstanding (`PaymentCollectionService`, `EnquiryPaymentService`) — a cashier cannot record more than what is actually due. This is unchanged for cash, card, UPI and cheque. For **bank-disbursed payments only** (Demand Draft, Bank Transfer — the rails typically used for education-loan disbursements from a bank), a permission-gated cashier may now opt in to collect an amount **above** total outstanding when the bank has released more than the student currently owes.

The excess portion is never silently dropped and never manually requested as a refund — it is carved out automatically, in the same transaction as the payment, as a `fee_refunds` row tagged `source = AUTO_EXCESS`. This auto-generated refund **cannot be rejected or deleted by staff** (enforced in `FeeRefundService.rejectRefund()`), only approved/paid out — because unlike a manual refund, the money was genuinely received and is unconditionally owed back once the excess is confirmed. Approving an `AUTO_EXCESS` refund does not soft-flag any `FeeInstallment`/`EnquiryPayment` rows (unlike a manual refund) since the excess was never allocated to a fee in the first place.

Because the existing refund model allows only one active (non-`REJECTED`) refund per receipt (`uq_fee_refunds_active_receipt`), a receipt carrying an active `AUTO_EXCESS` refund cannot also have a manual full-receipt refund initiated against it until the excess refund is resolved (paid out) — there is no separate "cancel the whole payment" flow in this system; reversing a payment is always done via the existing refund request flow, which this constraint naturally sequences.

### Scope

- `CollectPaymentRequest.allowExcess` (opt-in, defaults to unset/false) — only honored by `PaymentCollectionService.collectAdvancePayment` (the per-student, non-term-gated "Advance Payment" flow — the term-gated bulk "Collect Payment" list and enquiry pre-admission payments are unaffected/out of scope).
- Requires the payment mode to be `DEMAND_DRAFT` or `BANK_TRANSFER`, and the caller to hold `FEE_COLLECT_EXCESS`, checked server-side even though the frontend also gates the checkbox on both conditions.
- The receipt (`PaymentReceipt.amountPaid`) records the **full amount physically received** (matching the bank/DD reference), not just the portion applied to fees — the unapplied excess is represented separately by the `AUTO_EXCESS` `FeeRefund` row, and shows up in the student's receipt history as a negative "Refund" line once approved (existing `getReceipts()` behavior, unchanged).
- Frontend: Student Fee Detail → Advance Payment form gains an "Allow payment above total outstanding (bank excess)" checkbox, visible only in Advance mode, for DD/Bank Transfer, to users holding `FEE_COLLECT_EXCESS`. The confirmation modal and Fee Refund List both surface the excess amount / an "Auto" source chip; the Reject action is hidden (and blocked server-side) for `AUTO_EXCESS` refunds.

### Permissions

- `FEE_COLLECT_EXCESS` (new, category FINANCE, screen "Collect Payment", tier 4 — same default tier as `FEE_COLLECT`) — dedicated permission per the operation-wise permission mapping rule; not a reuse of `FEE_COLLECT`. Role assignment beyond the DEV_ADMIN/SUPPORT_ADMIN catch-all is handled via the DB-only Role Management module, not hardcoded.

### Migration Notes

- V259 — adds `fee_refunds.source VARCHAR(20) NOT NULL DEFAULT 'MANUAL'` and seeds `FEE_COLLECT_EXCESS` with the mandatory DEV_ADMIN/SUPPORT_ADMIN catch-all sync.

### Explicitly Out of Scope

- **General partial refunds** (refunding less than the full receipt amount) were evaluated and declined — the existing refund model derives `refund_amount` as 100% of the receipt and caps refunds at one per receipt; building true partial refunds would require reworking outstanding-balance math and the uniqueness constraint for a capability this feature doesn't need. The `AUTO_EXCESS` refund is a separate, additive carve-out, not a step toward general partial refunds.
- **A dedicated payment/receipt cancellation ("void") flow.** None exists anywhere in the system today; this feature does not introduce one. Reversing a payment — excess or otherwise — is always done through the existing refund request flow.

---

## BR-37: Barcode Generation & Configurable Label-Printer Output

### Business Rule

Every book and periodical carries a dedicated `barcode` value — distinct from `accession_number`, defaulting to it at creation but independently editable for stock that needs a custom prefix/suffix — rendered as a Code128 barcode. The librarian can print a single item's barcode (preview dialog) or a batch of selected items (label sheet), and the actual **print transport** for both is a global, librarian-configurable setting rather than hardcoded: `BROWSER` (default — today's browser print dialog, unchanged), `NETWORK` (the backend streams plain ZPL directly to a networked thermal label printer's raw socket), or `LOCAL_AGENT` (the browser forwards ZPL to a local Browser-Print-style agent for a USB-attached printer). The same "Print" button is used in all three modes — only the transport underneath changes.

ZPL generation is deliberately vendor-neutral (no Zebra-specific extensions), since Zebra, TSC, Godex, and many TVS thermal label-printer models all implement or emulate plain ZPL — the college is not committed to a single printer brand.

### Scope

- `library_books` / `library_periodicals` gain a `barcode` column (unique per table), separate from `accession_number`.
- `LibraryBarcodeService` generates: Code128 PNG (single-item preview/print, rendered at 300 DPI with a guaranteed ≥2mm quiet zone around the bars regardless of how small the configured label is) and PDF label sheets (batch, grid sized to the configured label mm dimensions) for `BROWSER` mode; plain ZPL (single item, and batch grouped into rows of `barcode_labels_per_row`) plus a raw-socket ZPL sender for `NETWORK` mode.
- New global settings (`library_settings` table): `barcode_label_width_mm` / `barcode_label_height_mm` (sticker size, existing), `barcode_printer_mode` (`BROWSER` default / `NETWORK` / `LOCAL_AGENT`), `barcode_printer_ip` / `barcode_printer_port` (`NETWORK` only — IP is validated server-side as a private/RFC1918 or loopback address, both at settings-save time and again immediately before every socket send), `barcode_labels_per_row` (1/2/4 — describes how many labels are physically die-cut across the loaded roll; batch printing only).
- Frontend `LibraryPrintTransportService` is the single place that resolves the configured mode and branches accordingly — the barcode preview dialog's "Print" button and both list screens' bulk "Print Selected Labels" action all share it, so `BROWSER` mode's existing behavior is untouched no matter what the other two modes do.
- `LOCAL_AGENT` forwards ZPL via a raw `fetch()` to `http://localhost:9100/write` — Browser Print's fixed default local endpoint, a separate address from the configurable `barcode_printer_port` (which is server-side only, used for `NETWORK` mode).

### Permissions

- Printing over any transport reuses the existing `LIBRARY_CATALOGUE_PRINT_BARCODE` / `LIBRARY_PERIODICAL_PRINT_BARCODE` permissions — same operation, different delivery mechanism, not a new one, per the operation-wise permission mapping rule.
- Configuring the printer transport lives on the existing Library Settings screen, gated by the existing `LIBRARY_SETTINGS_MANAGE` permission — no new permission needed.

### Migration Notes

- V260 — adds the `barcode` columns (with backfill + uniqueness), `barcode_label_width_mm` / `barcode_label_height_mm` settings, and the `LIBRARY_CATALOGUE_PRINT_BARCODE` / `LIBRARY_PERIODICAL_PRINT_BARCODE` permissions (with the mandatory DEV_ADMIN/SUPPORT_ADMIN catch-all sync).
- V263 — adds `barcode_printer_mode` / `barcode_printer_ip` / `barcode_printer_port` / `barcode_labels_per_row` settings. No new permissions.

### Explicitly Out of Scope

- **Direct browser-to-printer device access (WebUSB/WebSerial).** Evaluated and declined in favor of the OS print dialog (`BROWSER`) and a vendor local-agent (`LOCAL_AGENT`) — both avoid writing and maintaining low-level, per-printer-model device driver code inside this app.

---

## BR-38: Library Circulation — Catalogue, Issue/Return, Fines & Periodicals

> **Documentation gap backfill.** This is the base Library module BR-35 (Rack/Shelf), BR-37 (Barcode/Printer), and the Change Log's own migration notes (`student_program_transfers` reference) all assume already exists — it was built at V196–V198 but never documented. Written retroactively from the shipped code, not a new requirement.

### Business Rule

The Library module tracks a Book Catalogue (Accession Register), circulation (issue/return/renewal) for both students and faculty, an overdue fine log, and a Journals & Periodicals register — all gated behind a new `LIBRARIAN` role introduced specifically for this module.

**Issuing** a book or periodical requires the item to be `AVAILABLE` and checks two librarian-configurable limits from `library_settings`, separately for students vs. faculty: the loan period (`student_loan_days` = 14 / `faculty_loan_days` = 30, default) and the maximum concurrent items a member may hold (`student_max_books` = 2 / `faculty_max_books` = 3, default). The due date is `issued_date + loan_days`. A physical copy can only be actively issued to one person at a time — enforced by a partial unique index on `(book_id)` / `(periodical_id)` where status is `ISSUED`/`OVERDUE`, not just application logic.

**Returning** an item flips it back to `AVAILABLE`. If returned after its due date, a `LibraryFine` row is auto-created — `overdue_days × fine_per_day` (`fine_per_day` setting, default ₹1/day) — starting life as `PENDING`. Fines are then manually resolved to `WAIVED` or `COLLECTED`; there is no automatic tie-in to the cashier/fee-collection register (see Explicitly Out of Scope).

**Renewing** is blocked once `renewal_count` reaches `max_renewals` (default 2) or if the item was already returned; a successful renewal extends the due date by another full loan period from today, increments `renewal_count`, and resets an `OVERDUE` issue back to `ISSUED`.

A **daily scheduled job** (1 AM) auto-flips any `ISSUED` row whose due date has passed to `OVERDUE` — this only affects status/visibility; the fine itself is still computed once, at actual return time, not accrued day-by-day while overdue.

### Scope

- `library_books` — the Accession Register: accession number, bibliographic fields (title/authors/publisher/ISBN/edition/etc.), call number, shelf location (superseded by the Rack/Shelf hierarchy in BR-35), subject category, source of supply (Purchase/Donation/Exchange), and status (`AVAILABLE`/`ISSUED`/`LOST`/`DAMAGED`/`WITHDRAWN`).
- `library_issues` — one table for both member types (`member_type` discriminator + a `CHECK` enforcing exactly one of `student_id`/`faculty_id` is set), and (since V250) exactly one of `book_id`/`periodical_id`.
- `library_fines` — one fine per issue (`UNIQUE` on `issue_id`), `PENDING → WAIVED | COLLECTED`.
- `library_periodicals` — Journal/Periodical Register (national/international, organization, volume/issue, subscription status). Originally tracked only in aggregate (`copies_count`, one subscription-level status); V250 gave periodicals their own per-copy `accession_number` + `status` so individual copies can be issued/returned exactly like books.
- `library_settings` — the typed key-value config store this and every later Library BR (35, 37) has continued to add settings to.
- Front-desk lookups: circulation lookup by accession number, and scan-to-return (`lookupActiveIssueByCode`) resolving a scanned barcode or accession number to its active issue.
- "My Library" (self-service issue history for the logged-in student/faculty member, scoped server-side to their own records) and "Issue Book" (a quick-issue entry point) are separate nav screens layered on this same circulation data.

### Permissions

- New `LIBRARIAN` role (hierarchy level 5), granted every `LIBRARY_*` permission.
- `LIBRARY_CATALOGUE_VIEW`/`MANAGE`, `LIBRARY_ISSUE_VIEW`/`MANAGE`, `LIBRARY_FINE_VIEW`/`MANAGE`, `LIBRARY_PERIODICAL_VIEW`/`MANAGE`, `LIBRARY_SETTINGS_MANAGE`, `LIBRARY_REPORT_VIEW`, `LIBRARY_IMPORT` — granted in full to `DEV_ADMIN`/`SUPPORT_ADMIN`/`ADMIN`/`COLLEGE_ADMIN` alongside `LIBRARIAN`.
- `FACULTY`/`STUDENT` get only `LIBRARY_CATALOGUE_VIEW` + `LIBRARY_ISSUE_VIEW` — "own issue history only" is enforced in the service layer (`findMyIssues`, keyed by the caller's own Keycloak username), not by a separate permission.
- `MY_LIBRARY_VIEW` and `LIBRARY_QUICK_ISSUE` (gating the "My Library" and "Issue Book" nav entries) were added later, under the Permission Model V2 redesign (BR-39/V247) — not part of the original V196–V198 rollout.

### Migration Notes

- V196 — creates all five core tables (`library_books`, `library_issues`, `library_fines`, `library_periodicals`, `library_settings`) with their seed settings.
- V197 — adds the `LIBRARIAN` role, all `LIBRARY_*` permissions, and the role-permission assignments described above.
- V198 — a one-off catch-all granting `DEV_ADMIN` every permission that earlier migrations had missed (not Library-specific, just shipped alongside this module).
- V250 — gives periodicals their own accession number + per-copy status and lets `library_issues` reference a periodical instead of a book (exactly one of the two, enforced by `CHECK` + a second partial unique index mirroring the book one).

### Explicitly Out of Scope

- **Automatic fine-to-cashier integration.** V196's own header comment notes fines are "tracked now; cashier integration is Phase 2" — waiving/collecting a fine today is a manual status change on `library_fines`, with no linkage into `PaymentReceipt`/the fee-collection register. Still the case as of this writing.

---

## BR-39: Permission Model V2 — Tiers & Granular Screen-Level Permissions

> **Documentation gap backfill.** BR-24 documents the original decision to move RBAC fully into the database; this is the second-generation redesign of that same system, now the permission architecture actually running in production, referenced here as "V247" by BR-38 above.

### Business Rule

Two changes layered on top of BR-24's DB-only RBAC. First, every permission gained a `tier` (an integer ranking) so that granting/delegating a permission can itself be tier-gated — a role can only be assigned permissions at or below the tier its own management screen allows, preventing privilege escalation through the Role Management UI. Second, permissions that used to be one broad `_MANAGE` code per screen were split into one dedicated code per operation — `_CREATE`/`_EDIT`/`_DELETE`/`_EXPORT` (and screen-specific operations like `_PRINT_BARCODE`, `_TRANSFER`, `_VIEW_HISTORY` added by later BRs) — each carrying its own `screen_label` so the Role Management screen can group every operation belonging to one conceptual screen together, regardless of how many discrete permission codes back it.

This is the origin of the "operation-wise permission mapping" hard gate every later BR (35, 36, 37, 38) already follows: a new button/action always gets its own permission code, never a reuse of an existing one.

### Scope

- `permissions.tier` (integer) — controls delegable scope; a Permission Tiers screen (`/permission-tiers`) lets admins manage tier assignments directly.
- `permissions.screen_label` — a display-grouping string (e.g. "Book Catalogue", "Issue Desk"/"Issue Explorer") so Role Management can present many granular codes as one logical screen's permission set.
- Existing broad `_MANAGE` permissions across the app were split into granular per-operation codes; older code paths were migrated screen-by-screen (not all at once).

### Migration Notes

- V241 — introduces the `tier` column and the Permission Tiers screen's backing data.
- V242 — the granular screen-permission split (per-operation codes) and initial `_EXPORT` backfill.
- V247 — per-screen isolation pass + introduces `screen_label` for Role Management grouping; also where `MY_LIBRARY_VIEW`/`LIBRARY_QUICK_ISSUE` (BR-38) were added.
- V248, V257–V258, V261–V262 — later screens/modules (including Library's own export, view-history, and screen-label-rename migrations) adopting this same pattern.

---

## BR-40: Role/User-Customizable Dashboard & Analytics Widget System

> **Documentation gap backfill.** Fully shipped, actively growing feature (37+ widgets across four tiers as of V190) with no BR of its own.

### Business Rule

Every role has a default, ordered dashboard layout — a grid of widgets, each spanning 1–4 columns and 1–2 rows — configured in the database rather than hardcoded per-role in the frontend. A user holding `DASHBOARD_CUSTOMIZE` can save a personal layout that overrides their role's default entirely (not merged with it); deleting all of a user's personal widget rows reverts them to seeing the role default again.

Each widget's data comes from its own `/dashboard/data/*` endpoint. None of these introduce a dedicated "can see this widget" permission — each reuses whichever existing feature-view permission is topically relevant (e.g. the admission-funnel widget requires `ENQUIRY_VIEW`/`STUDENT_VIEW`/`REPORT_VIEW`), so a dashboard never surfaces data a user couldn't already see on that feature's own screen.

Widgets were built in four escalating tiers of sophistication, seeded progressively rather than all at once: **Tier 1** admin analytics (admission funnel, fee collection target, dues aging, program admissions), **Tier 2** analytics (agent performance, program revenue mix, scholarship burn, doc verification backlog), **Tier 3** strategic/monthly views (geographic admissions heatmap, YoY admissions, refund/cancellation rate, payment mode breakdown, student-faculty ratio, lab utilization heatmap, cohort retention, a top-line KPI strip), and **Tier 4** passive alerts (anomaly banner, seat-capacity warnings, compliance-document expiry, an audit mini-feed) — plus standalone admission/seat-fill KPI stat cards added afterward.

### Scope

- `role_dashboard_widget_configs` (role default) / `user_dashboard_widget_configs` (personal override) — identical shape: `widget_key`, `widget_order`, `col_span` (1–4), `row_span` (1–2), optional `config_json`.
- `DASHBOARD_CUSTOMIZE` — gates saving/deleting a personal layout (`PUT`/`DELETE /dashboard/config`); viewing the effective layout (`GET /dashboard/config`) only requires being authenticated.
- Frontend `widget-registry.ts` plus a dynamic renderer (`dashboard.ts`) that reads whichever config is in effect and lays widgets out accordingly. A hardcoded `DEFAULT_WIDGET_KEYS` fallback exists for a role with zero DB rows — this masked a real seeding bug once (see Migration Notes, V143).
- `compliance_documents` (authority, document name, reference number, expiry date, status) and `programs.seat_capacity` — the minimal schema Tier 4's alert widgets need.

### Permissions

- `DASHBOARD_CUSTOMIZE` is the only dashboard-specific permission in the system. Granted to `DEV_ADMIN`/`SUPPORT_ADMIN`/`ADMIN`/`COLLEGE_ADMIN` (who can also assign it to other roles) and to `FACULTY`/`FRONT_OFFICE`/`CASHIER` (personalize their own only, since they can't edit roles at/above their level). `STUDENT` is intentionally excluded — students never get dashboard customization.
- Every widget data endpoint is gated by an existing feature permission, not a new one (see Business Rule).

### Migration Notes

- V119/V120 — first cut: a simple ordered widget-key list per role. V120 immediately reworked the primary key because Hibernate's `@OrderColumn` needs to freely rewrite `widget_order`, which the original composite PK blocked.
- V131/V132 — replaced the simple list with the metadata-rich config shape (span + JSON) that ships today, split into role-default and per-user-override tables.
- V133/V136 — `DASHBOARD_CUSTOMIZE` permission and its role grants.
- V135 — aligns `col_span`/`row_span` column types with the Java entity fields (`SMALLINT` → `INTEGER`) after a Hibernate schema-validation mismatch.
- V143 — a real production bug fix: V134 seeded widgets for a role named `COLLEGE_ADMIN` (uppercase), but the actual role (from V123) was `collegeadmin` (lowercase) — leaving that role with zero configured widgets and silently falling back to the frontend's hardcoded default list. This migration re-seeds under the correct role name.
- V145–V148 — the Tier 1–4 widget rollout; V148 also adds the `compliance_documents`/`seat_capacity` schema Tier 4 needs.
- V162, V190 — additional admission and seat-fill KPI stat-card widgets, appended after the tiered rollout.

### Explicitly Out of Scope

- **A Compliance Documents management screen.** `compliance_documents` has schema, a repository, and backs the Tier 4 compliance-alert widget, but there is no CRUD screen to create or edit these rows yet — a real gap worth its own decision if a full Compliance module is wanted, not something this BR resolves.

---

## BR-41: Number Sequence Redesign & Roll Number Generation/Assignment

> **Documentation gap backfill.** Two related identifier-generation mechanisms, built alongside BR-27's admission numbers but never documented themselves.

### Business Rule

**Roll numbers** are assigned to students in the format `[CollegeCode][CourseCode][Year][Sequence]` — e.g. `959` + `65` + `2026` + `004` = `959652026004` — generated from a per-course-per-year counter plus a global college code (`ROLL_NUMBER_COLLEGE_CODE` system configuration) and a per-course 2-digit code (`courses.roll_number_code`). Staff can preview a batch before committing, then generate-and-assign roll numbers in bulk for every student still missing one; generation is protected by a pessimistic lock on the counter row so two concurrent bulk-generate requests can't hand out the same number twice. A single student's roll number can also be assigned or corrected manually, bypassing generation entirely (just validated and written as given).

**The generic number-sequence engine** — what BR-27's admission numbers, receipt numbers, and the commission/refund/disbursement numbers from BR-34/36 are all generated from — was redesigned from one flat table into two: `number_series_definitions` (one row per series *type*: its prefix, separator, digit padding, and whether the scope key appears in the rendered number) and `number_sequence_counters` (one row per series × scope period, auto-incremented). This moved series configuration out of migrations and into a "Number Series" management screen, so adding or adjusting a series no longer requires a code change.

### Scope

- `roll_number_sequences` (`course_id`, `academic_year`, `last_sequence`) — one counter per course per year.
- `RollNumberGeneratorService` — preview vs. generate-and-assign use the same computation; only the latter persists and increments the counter.
- `number_series_definitions` / `number_sequence_counters` replace the single `application_number_sequences` table (originally introduced for admission numbers), migrated with every existing counter carried over exactly — no gaps, no restarts. `APP_TIMEZONE` (system config) drives how the engine computes scope-period boundaries (e.g. an academic-year or month scope key) in the correct timezone.
- `students.university_registration_number` / `students.umis_number` — two more externally-issued identifier fields added alongside this work; stored and uniqueness-checked, not generated by this engine.
- Course Master briefly carried two separate per-course codes serving the identical purpose (`roll_number_code` and `admission_number_code`); consolidated down to `roll_number_code` alone as the single field `ApplicationNumberSequenceService` now reads for both.

### Permissions

- `ROLL_NUMBER_ASSIGN` gates only the "Assign Roll Numbers" nav entry — the screen's actual actions (manual assign, bulk assign, generate, preview) all run under the pre-existing `STUDENT_CREATE` permission, not a dedicated one. Worth knowing if the two are ever granted to different roles: holding `ROLL_NUMBER_ASSIGN` without `STUDENT_CREATE` would show the screen but every action on it would 403.
- `NUMBER_SERIES_VIEW` / `NUMBER_SERIES_MANAGE` (new, category SETTINGS) — granted to `DEV_ADMIN`/`SUPPORT_ADMIN`/`ADMIN`/`COLLEGE_ADMIN`.

### Migration Notes

- V111 — `roll_number_sequences` + `courses.roll_number_code`.
- V112 — unrelated column additions (`university_registration_number`, `umis_number`) shipped in the same migration.
- V142 — the original single-table `application_number_sequences` (admission numbers).
- V157 — adds `admission_number_code` (a second, temporarily separate per-course code) plus a configurable separator/scope-inclusion flag on the rendered format.
- V182–V184 — data-safety hardening: placeholder-then-`NOT NULL` on both course codes, then tightening `roll_number_code` to exactly 2 characters.
- V193 — consolidates `admission_number_code` into `roll_number_code`, dropping the duplicate column.
- V244 (schema), V245 (permissions + management screen), V246 (drops the old `application_number_sequences` table once the new engine was validated in production) — the three-phase number-sequence redesign.

---

## BR-42: Faculty Extended Profile & Document-Type Requirements Engine

> **Documentation gap backfill.** BR-26 documents the document *review/verification-lock* UX built on top of this — badges, lock rules, override — but never the schema or configuration engine underneath it that BR-26 explicitly assumes ("derived from faculty document rows and document-type requirements").

### Business Rule

Faculty profiles carry a full HR-adjacent record beyond the original core fields: identity/demographics (PAN, Aadhaar, date of birth, gender, marital status, nationality, religion, blood group), bank details (account number, IFSC, branch, bank name, account holder, account type), a postal address (mirroring the `Address` shape already used for students), a six-way experience breakdown (teaching/clinical × UG/PG/PhD, in years), highest qualification, and an NRTS number (a nursing-council registration identifier, unique when present).

`faculty_documents` holds one row per `(faculty, document_type)` — scanned uploads with a status lifecycle, reviewer/timestamp metadata, and the file itself — the exact table BR-26's badges and verification locks operate on.

Which document types are actually **required** for a given faculty member is not hardcoded — it's driven by `faculty_document_type_requirements`, a rules table admins configure (Faculty Doc Config screen). Each rule names a document type plus at least one of: designation, speciality, or highest qualification. A document type is required for a faculty member if **any** rule matches **any one** of their designation/speciality/qualification (an OR match, not requiring all three) — e.g. a rule scoped only to speciality "Cardiology" makes that document mandatory for every Cardiology faculty member regardless of designation.

### Scope

- `faculty` — the columns listed above added across two migrations (V93, V199).
- `faculty_documents` (`faculty_id`, `document_type`, `status`, `remarks`, `verified_by`/`verified_at`, file metadata + bytes) — unique per faculty × document type.
- `faculty_document_type_requirements` (`document_type`, `designation_id`, `speciality_id`, `qualification`) — at least one criterion required per rule (`CHECK` constraint). Originally stored `designation` as free text; migrated to a `designation_id` FK into the Designation Master when that master was introduced (V201, a separate undocumented feature of its own).

### Permissions

- Reading the configured requirements (`GET /faculty-document-type-requirements`) requires only being authenticated — no dedicated permission.
- Creating/deleting a requirement rule requires `FACULTY_DOC_CONFIG_MANAGE` (added under the Permission Model V2 rollout, V247/BR-39 — this feature predates that permission by over a hundred migrations; V126 shipped without any dedicated gate on the config screen until V247 added one).

### Migration Notes

- V93 — extended profile fields (identity/demographics/bank/address/experience) and the `faculty_documents` table.
- V126 — `faculty_document_type_requirements` and `faculty.highest_qualification`.
- V199 — `faculty.nrts_number` (unique when present).
- V201 — converts the rule table's free-text `designation` column to `designation_id`, once the Designation Master existed (not itself part of this BR).

---

## BR-43: Retro Admit (Legacy Direct Admit)

> **Documentation gap backfill.** Distinct from BR-31's bulk Excel import — both backfill legacy fee history, but this is a single interactive form for one student at a time, not a spreadsheet-driven mass migration.

### Business Rule

Retro Admit is a dedicated, permission-gated path for onboarding a legacy student who never went through the enquiry pipeline — one form submission atomically creates a `Student`, a synthetic `Enquiry` (status `ADMITTED`, `admission_source = DIRECT_ADMIT`), and an `Admission`, in a single transaction, rejecting outright if the given email is already in use. The synthetic Enquiry exists purely so downstream code that joins through Enquiry (reporting, the admission-funnel dashboard widget from BR-40) has a row to reference — `admission_source` is what lets that reporting tell a real pipeline enquiry apart from one manufactured by this feature.

The same submission can optionally reconstruct the student's historical fee ledger: a total-fee figure per program year generates the same `StudentFeeAllocation`/`SemesterFee` structure a normal enquiry-to-admission conversion produces, and a list of historical payments is then applied **FIFO** against those generated semester-fee slots — each producing a `FeeInstallment` row and a backdated receipt via the same `UnifiedReceiptService` used for live payment collection, so historical payments appear in the student's receipt history exactly as if collected today (transaction reference is still mandatory for electronic payment modes, per BR-19).

### Scope

- `POST /students/retro-admit` — the full personal/demographic/family/address/declaration field set, materially overlapping BR-31's Excel column set but entered via one interactive form with full field-level validation rather than a spreadsheet row.
- `yearFees` (optional, one `{yearNumber, totalFee}` entry per program year) and `payments` (optional, `{paymentDate, amount, paymentMode, receiptNumber?, transactionReference, remarks}` — a null `receiptNumber` auto-generates one from the payment date's year) are both entirely optional; a Retro Admit with neither just creates the Student/Enquiry/Admission trio with no fee history.
- Response reports how many years got a fee record and how many payment rows were created, plus the total historical amount applied — a quick confirmation of what was actually backfilled.

### Permissions

- `RETRO_ADMIT` — originally seeded as `LEGACY_ADMIT`, renamed by V203 (same permission row, cosmetic rename only). Granted to `COLLEGE_ADMIN` and `FRONT_OFFICE`.

### Migration Notes

- V194 — adds `enquiries.admission_source` (`ENQUIRY_FLOW` default / `DIRECT_ADMIT`).
- V195 — seeds the `LEGACY_ADMIT` permission, granted to `COLLEGE_ADMIN`/`FRONT_OFFICE`.
- V203 — renames the permission code and display name to `RETRO_ADMIT`; also runs a `DEV_ADMIN`/`SUPPORT_ADMIN` catch-all sync covering any permission gaps since V129.

---

## BR-44: MinIO File Storage Migration

> **Documentation gap backfill.** Already flagged in project notes as a known documentation gap — this makes it official.

### Business Rule

File blobs — enquiry/admission documents, faculty documents, user profile/cover photos — that were originally stored as `bytea` columns directly in Postgres are migrated to MinIO object storage, with the database keeping only a `storage_key` (the object's full path, e.g. `aadhar_card/123-abc-filename.pdf`) rather than the file bytes themselves.

This is not a lazy or optional dependency: `MinioStorageService` checks/creates its configured bucket in a `@PostConstruct` hook, so **the backend fails to start outright** if MinIO isn't reachable at boot — any connection failure is wrapped in a hard `IllegalStateException`, not deferred to first use. (The project's root `docker-compose.yml` has no bundled MinIO service, so local development requires a separately reachable MinIO instance — a live known friction point, not resolved by this feature.)

Moving the actual blob data is a manual, admin-triggered, re-runnable operation, deliberately kept out of the Flyway migration path — potentially large binary transfers aren't something you want silently coupled to a schema tool's transaction/rollback semantics. The original `file_data` columns are **not** dropped by any migration; they're kept as a safety fallback until the object-storage migration is verified complete, to be nulled out manually afterward.

### Scope

- `StorageService` — a storage-backend abstraction (`upload`/`download`/`downloadBytes`/`delete`/`exists`), implemented by `MinioStorageService`.
- Three affected tables: `enquiry_documents` (already had a `storage_key` column from V15/V124's document-table unification — reused, not newly added here), `faculty_documents` and `app_users` (`storage_key`/`profile_photo_key`/`cover_photo_key` added by V249).
- `StorageMigrationService` — batched migration across all three tables, retrying each file up to 5 times before marking it failed; safe to re-run since rows with a `storage_key` already set are skipped.
- `GET /admin/migrate-storage/status` (read-only pending-count check) and `POST /admin/migrate-storage/run` (executes synchronously, returns a per-table succeeded/failed/skipped report with failed row IDs) — a one-time operational tool, not a general feature screen.
- New required external configuration: `cms.minio.endpoint` / `access-key` / `secret-key` / `bucket`.

### Permissions

- Both endpoints require `hasRole('DEV_ADMIN')` — a hardcoded Spring Security role check, not the DB-driven permission system BR-24 established as this project's RBAC model. Worth knowing if DEV_ADMIN's meaning ever needs to change: this endpoint won't follow it the way a `@perm.has(...)` check would.

### Migration Notes

- V249 — adds the `storage_key`/`profile_photo_key`/`cover_photo_key` columns actually missing at that point (`enquiry_documents` already had its own since V15/V124).

### Explicitly Out of Scope

- **Dropping the old `bytea` columns.** Deliberately deferred until the object-storage migration is manually verified complete on each environment — not something this feature does automatically.

---

## BR-45: Enquiry Payment Credit Application to Student Fee Demands

### Business Rule

Money an enquiry pays before ever becoming a student — pre-admission payments captured against the enquiry itself in `enquiry_payments` — is never lost or left for manual reconciliation. Once that enquiry converts to a student, every fee-collection attempt first treats the enquiry's total pre-payment as available credit and applies it against the student's semester fees (earliest open semester first), **ahead of** whatever new amount is actually being collected in that same call.

Available credit is never stored as a running balance column — it's recomputed fresh on every collection: `total enquiry payments − sum of all credit already applied for that enquiry`. This means partial credit that outlives one collection call correctly carries forward to the next. Each time credit is actually consumed against a semester, a new `enquiry_credit_applications` row records exactly how much, against which semester fee, and which of the enquiry's original payment receipt(s) it's attributed to — one credit application can be sourced from multiple enquiry-stage payments, joined into a single receipt-number string.

A semester only accepts a genuinely new collection once term rules say it's "open" for collection — but credit application still checks (and can fully satisfy) a non-open future semester's balance first, so a semester that's already fully covered by credit doesn't wrongly block collection of the currently-open one. A non-open semester with a real unpaid balance still blocks collecting anything for later semesters, same as if no credit were involved.

### Scope

- `enquiry_credit_applications` — one row per credit application: enquiry, student, semester fee, amount applied, source receipt number(s), timestamp.
- `PaymentCollectionService` — recomputes remaining credit on every collection call and applies it semester-by-semester within each semester's remaining capacity, before any newly-tendered amount is applied to that same semester.
- A one-time SQL backfill (V187, a plain `DO` block, not an ongoing mechanism) retroactively applied existing `enquiry_payments` totals as credit directly against `fee_demands`/`student_term_enrollments` for students who had already converted before this per-semester tracking existed — chronological by due date, any surplus left unapplied for the cashier to handle manually. This predates and is structurally separate from the `enquiry_credit_applications`-tracked mechanism V209 introduced.
- `GET /enquiries/{id}/credit-applications` and `GET /students/{studentId}/credit-applications` — read endpoints surfacing this audit trail (used by the Enquiry detail and Student Fee Detail screens) so staff can see exactly where a student's opening-balance credit came from.

### Permissions

- No new permissions — credit application happens automatically inside the existing fee-collection flow. The two read endpoints reuse `ENQUIRY_VIEW` and `STUDENT_FEE_VIEW` respectively.

### Migration Notes

- V187 — the one-time backfill described above (data-only, no schema change).
- V209 — creates `enquiry_credit_applications`, the ongoing tracking table.
- V211 — widens `receipt_number` to `VARCHAR(100)` to fit the joined multi-receipt string described above.

---

## BR-46: Designation Master

> **Documentation gap backfill.** A standard master that already conforms to BR-32's lifecycle contract but was never added to that BR's Scope table, and never given one of its own.

### Business Rule

Faculty designation (Professor, Associate Professor, Lecturer, etc.) was originally a hardcoded Java enum; it's now a DB-managed master like any other configurable list in the system, editable without a code deployment. It follows the exact same lifecycle-status contract (`PATCH /{id}/status`, `{ isActive, reason }`) and name/code uniqueness-validation pattern BR-32 already established for other masters — it should really be in BR-32's Scope table alongside Scholarship Type, Blood Group, Community, etc., and isn't; recorded here rather than silently added there.

### Scope

- `designations` (`name`, `code`, `description`, `is_active`, timestamps) — full CRUD screen, paginated list, `name-exists`/`code-exists` uniqueness checks.
- `faculty.designation_id` and `faculty_document_type_requirements.designation_id` — both converted from a raw enum/free-text column to a proper FK once this master existed (the latter conversion already noted in BR-42).
- `is_active` was added later (V229, alongside the same column for Speciality in the same migration) — not part of the original rollout, brought this master in line with BR-32's lifecycle pattern after the fact.

### Permissions

- `DESIGNATION_VIEW` / `DESIGNATION_MANAGE` (category MASTER) — full management granted to `DEV_ADMIN`/`SUPPORT_ADMIN`/`ADMIN`/`COLLEGE_ADMIN`; `FACULTY`/`FRONT_OFFICE`/`CASHIER` get view-only (needed to render the designation dropdown in faculty forms).

### Migration Notes

- V201 — creates `designations`, seeds the 11 original enum values (enum name becomes `code`), converts `faculty.designation` and `faculty_document_type_requirements.designation` to FKs.
- V202 — `DESIGNATION_VIEW`/`DESIGNATION_MANAGE` permissions and role grants.
- V229 — adds `is_active`.

---

## BR-47: India Location & Country Master

> **Documentation gap backfill.** BR-32 already documents this master's activate/deactivate lifecycle rules (it's in that BR's Scope table); this BR covers the hierarchy, schema, and permissions BR-32 doesn't.

### Business Rule

Country → State → District is a real three-level DB-managed hierarchy — not free text — backing every cascading location dropdown across enquiry, student, and faculty forms. It started as States → Districts scoped to India only (all 28 states + 8 union territories, ~780 districts, LGD-aligned 2024 data seeded directly in a migration); a Country level was added afterward, with India seeded as the founding row (`id = 1`, ISO code `IN`) and every existing state retroactively linked to it, rescoping state name/code uniqueness from global to per-country.

Only the **country** field on `enquiries`/`faculty`/`students` was actually converted to a `country_id` FK referencing this master. **State and district on those same records remain plain free text** — populated *from* the cascading dropdowns at data-entry time, not stored as a live reference — so renaming a state or district in the master does not retroactively update any address record that already selected the old value.

### Scope

- `location_countries` (`name`, `iso_code`, `is_active`) — top of the hierarchy; India seeded as row 1.
- `india_states` (`name`, `code`, `country_id` FK, `is_active`) — uniqueness on name/code scoped per-country (not globally) since the Country level was introduced.
- `india_districts` (`state_id` FK, `name`, `is_active`) — unique per state.
- Full CRUD under `/india` (countries, states, districts) — nested creation endpoints (a state under a country, a district under a state) alongside flat list/lookup endpoints. Every `GET` requires only authentication, no permission check, so the cascading dropdowns work for any role regardless of whether they hold `INDIA_LOCATION_VIEW`; only create/update/delete require `INDIA_LOCATION_MANAGE`.

### Permissions

- `INDIA_LOCATION_VIEW` / `INDIA_LOCATION_MANAGE` (category MASTER) — full management to `DEV_ADMIN`/`SUPPORT_ADMIN`/`ADMIN`/`COLLEGE_ADMIN`; view-only to `FRONT_OFFICE`/`FACULTY`. The nav entry also references `_CREATE`/`_EDIT`/`_DELETE`/`_EXPORT` variants (consistent with the Permission Model V2 granular split, BR-39), but every backend endpoint here is still gated by the single `INDIA_LOCATION_MANAGE`, not split further.

### Migration Notes

- V149 — `india_states`/`india_districts` tables.
- V150 — seeds all 28 states + 8 UTs and their districts.
- V151 — `INDIA_LOCATION_VIEW`/`MANAGE` permissions and role grants.
- V158 — adds `location_countries`, seeds India as row 1, links every existing state to it, rescopes state uniqueness to per-country.
- V159 — converts `enquiries`/`faculty`/`students.country` from free text to a `country_id` FK (defaulting unmatched/new rows to India); state/district are left as free text.

---

## BR-48: JWT Revoked-Token Tracking (Logout Denylist)

### Business Rule

JWTs issued by Keycloak (BR-24's identity model) are normally stateless and stay valid until their natural expiry, even after a user logs out client-side — there is no built-in server-side "logout" for a bearer token. This feature closes that gap with a denylist: `POST /auth/revoke` (called by the frontend on logout) inserts the **current request's own token** — identified by its `jti` claim, read from the authenticated principal itself, not a request body — into `revoked_tokens` along with its natural expiry. From then on, a custom `OAuth2TokenValidator` chained into JWT validation rejects that token on every subsequent request with `invalid_token`, even though it hasn't naturally expired. An hourly job purges rows whose expiry has already passed, since a token past its own expiry is already rejected by Spring's standard expiry check — keeping it in the revoked list forever would just be dead weight.

### Scope

- `revoked_tokens` (`jti` unique, `expires_at`, `revoked_at`).
- `TokenRevocationService` — `revoke(jti, expiresAt)` (idempotent — a no-op if already revoked), `isRevoked(jti)`, and an hourly `@Scheduled` purge of expired rows.
- `RevocationJwtValidator` — an `OAuth2TokenValidator` chained alongside Spring's standard issuer/expiry checks in the resource-server's JWT decoder, so every authenticated request (not just login) is checked against the denylist.
- `POST /auth/revoke` — called from the frontend's logout flow via a raw `fetch()` rather than the app's normal `HttpClient`, specifically to avoid a circular dependency on the auth interceptor that would otherwise attach to that same call.

### Permissions

- None — any authenticated user may revoke their own current token. This is inherently self-scoped; there is no admin-triggered "force logout" of a *different* user's session anywhere in this feature (see Explicitly Out of Scope).

### Migration Notes

- V243 — creates `revoked_tokens` with supporting indexes on `jti` and `expires_at`.

### Explicitly Out of Scope

- **Admin-forced logout of another user's session.** Revocation is always self-service, scoped to the caller's own bearer token — there is no endpoint anywhere that lets an admin revoke a token belonging to someone else.

---

## BR-49: INC Nursing Curriculum Compliance — Per-Semester Hours, Electives, Attendance Thresholds & Batches

### Business Rule

Indian Nursing Council (INC) curricula require the system to express things the pre-existing curriculum/attendance model could not: a subject's Theory/Lab/Clinical contact hours and its INC category (Core/Foundational/Elective) both vary **by the semester/curriculum a subject is mapped into**, not just by the subject itself — e.g. Nursing Foundations is Theory/Lab-heavy in Term I but Clinical-heavy in Term II under the same subject record. This is why hours and Subject Type live on the curriculum-mapping row (`curriculum_term_courses`), not on the `Subject` master.

**Component-type hours.** Each curriculum mapping carries independent `theoryHours`/`labHours`/`clinicalHours` (default 0), edited via Theory/Lab/Clinical checkboxes on the Curriculum Map screen — unchecking a component zeroes and disables its hour field rather than requiring every subject to fill in all three.

**Choice-based electives.** A `CurriculumElectiveGroup` (scoped to one curriculum version + term) groups the mutually-exclusive `CurriculumSemesterCourse` rows a student may choose between. Bulk course-registration generation (`CourseRegistrationServiceImpl.generateRegistrationsForTermInstance`) automatically **skips** any offering whose mapping is marked elective — those are left for an admin to assign one-by-one via the new Elective Assignment screen, which rejects a second pick within the same elective group for the same student (idempotent if the same offering is re-submitted). An offering with no resolved curriculum mapping (legacy/unresolved) is treated as non-elective, preserving prior bulk-generate behaviour exactly.

**Per-component attendance thresholds.** Attendance minimums (e.g. 80% Theory / 100% Clinical) are resolved per curriculum mapping + `AttendanceType` (now `THEORY`/`LAB`/`CLINICAL`), walking student → course registration → offering → curriculum mapping → an `attendance_thresholds` override row, falling back to a 75% default at any missing step. `AttendanceService.getAttendanceReport()` now returns one entry **per component type** with attendance data, instead of one blended percentage across all types — a student can meet an 80% Theory requirement while failing a 100% Clinical one, which a single number would hide.

**Batches.** Lab/clinical roster splitting (e.g. 60 students → 3 batches of 20) is a real `Batch` entity scoped to a specific term's `CourseOffering` (not the curriculum mapping, since a batch is a per-term roster split, not curriculum-design metadata), with an enforced capacity (service-layer check, not a DB constraint — consistent with this codebase's existing style, e.g. `Cohort` seat limits) and real student membership (`batch_students`). `LabSchedule` can reference a `Batch` via a new nullable `batch_id`, populated alongside the pre-existing free-text `batch_name` column so existing conflict-check queries keep working unchanged. The Batch Manage dialog (opened from a Course Offering row) nudges — but does not require — assigning a coordinator faculty member to each batch.

### Scope

- `curriculum_term_courses` gains `theory_hours`/`lab_hours`/`clinical_hours`, `subject_type` (`SubjectType` enum: CORE/FOUNDATIONAL/ELECTIVE), `is_elective`, `elective_group_id`.
- `curriculum_elective_groups` — new table; `CurriculumElectiveGroupController`/`Service` (create/list/delete, delete guarded against groups still referenced by a mapping row).
- New `PUT /curriculum-semester-courses/{id}` — the Curriculum Map screen previously only supported add/remove of a mapping row, never editing hours/type/elective in place.
- `attendance_thresholds` — new table keyed on `(curriculum_term_course_id, attendance_type)`; `AttendanceThresholdController`/`Service` (`resolveThreshold`, CRUD), inline-edited on the Curriculum Map screen's mapping row (not a standalone screen).
- `course_offerings.curriculum_term_course_id` — new nullable FK + backfill, resolving an offering back to its curriculum mapping (needed for threshold resolution and elective detection); `CourseOfferingDto` now surfaces `isElective`/`subjectType`/`electiveGroupId`/`electiveGroupName`.
- `batches` + `batch_students` — new tables; `BatchController`/`Service` (create/update/deactivate, roster add/remove with capacity enforcement); Batch Manage dialog nested under the Course Offering list (not a standalone global master screen, since a batch only makes sense scoped to one term's offering).
- `lab_schedules.batch_id` — new nullable FK; the Lab Schedule form gained an optional "Batch (from roster)" dropdown that autofills the pre-existing free-text batch name field.
- `CourseRegistrationServiceImpl.assignElectiveChoice(enrollmentId, courseOfferingId)` + `POST /course-registrations/elective-assignment`; new **Elective Assignment** screen (academic year → term → elective group → per-student assignment), the first course-registration UI this app has ever had — the pre-existing `generateCourseRegistrations` frontend method was dead code with no calling component.
- Faculty-role scoping deliberately reuses existing constructs rather than a new multi-role join table: theory instructor stays on `CourseOffering.facultyId`, lab instructor stays on `LabSchedule.faculty` (already one per lab-schedule row), batch/clinical coordinator is the new `Batch.coordinatorFacultyId`.

### Permissions

- `CURRICULUM_ELECTIVE_GROUP_VIEW`/`MANAGE` — auto-assigned to existing `CURRICULUM_VIEW`/`MANAGE` holders.
- `ATTENDANCE_THRESHOLD_VIEW`/`MANAGE` — auto-assigned to existing `ATTENDANCE_VIEW`/`MANAGE` holders.
- `BATCH_VIEW`/`MANAGE` — auto-assigned to existing `COURSE_VIEW`/`MANAGE` holders.
- `COURSE_REGISTRATION_ELECTIVE_ASSIGN` — new, dedicated permission for the admin single-pick action (per the operation-wise permission mapping rule, this does **not** reuse `ADMISSION_CREATE`); auto-assigned to existing `ADMISSION_CREATE` holders.
- Editing hours/subject-type/elective/sort-order on an existing mapping row (`PUT /curriculum-semester-courses/{id}`) reuses the pre-existing `CURRICULUM_MANAGE` — already the single permission gating every other structural change to this table, so no new code was needed there.
- The bulk `POST /course-registrations/generate`, drop, and `GET` endpoints deliberately stay on `ADMISSION_VIEW`/`ADMISSION_CREATE` — not retrofitted onto dedicated codes in this pass, to avoid silently changing access for roles (e.g. `COLLEGE_ADMIN`) that hold `ADMISSION_CREATE` today for unrelated reasons.

### Migration Notes

- V265 — creates `curriculum_elective_groups`.
- V266 — adds `theory_hours`/`lab_hours`/`clinical_hours`/`subject_type`/`is_elective`/`elective_group_id` to `curriculum_term_courses`.
- V267 — `CURRICULUM_ELECTIVE_GROUP_VIEW`/`MANAGE` permissions.
- V268 — creates `attendance_thresholds`.
- V269 — adds `course_offerings.curriculum_term_course_id` + backfill via the existing `(curriculum_version_id, term_number, subject_id)` unique-constraint match.
- V270 — `ATTENDANCE_THRESHOLD_VIEW`/`MANAGE` permissions.
- V271 — creates `batches` + `batch_students`.
- V272 — adds `lab_schedules.batch_id` (nullable, additive alongside `batch_name`).
- V273 — `BATCH_VIEW`/`MANAGE` permissions.
- V274 — `COURSE_REGISTRATION_ELECTIVE_ASSIGN` permission.
- `CLINICAL` was added to the `AttendanceType` Java enum with **no migration** — `attendances.type` is a plain unconstrained `VARCHAR`, confirmed by reading `V18__create_attendances_table.sql` before assuming a schema change was needed.

### Explicitly Out of Scope

- **A general lecture timetable system.** Only `LabSchedule` (lab/practical scheduling) was extended — there was no lecture-timetable construct before this BR and none was built; "Timetable Automation" in the original ask was specifically about lab-block scheduling, which `LabSchedule` already covered.
- **Hard cutover of `lab_schedules.batch_name`.** The free-text column is kept and kept in sync alongside the new `batch_id` FK indefinitely in this pass; dropping it or making `batch_id` required is a separate future migration once all rows are backfilled and the frontend fully relies on the dropdown.
- **Retrofitting `CourseRegistrationController`/`StudentTermEnrollmentController` onto dedicated permission codes.** They continue to reuse `ADMISSION_VIEW`/`ADMISSION_CREATE` for their pre-existing endpoints (see Permissions above) — pre-existing granularity debt, not something this BR introduced or was asked to fix.
- **Student self-service elective selection.** Elective assignment is always admin-entered on the new Elective Assignment screen; there is no student-facing portal flow.
- **A generic multi-faculty-role join table** (e.g. `CourseOfferingFaculty`). Theory instructor, lab instructor, and batch coordinator each reuse an existing single-value field rather than a new queryable many-role construct — revisit only if a future need arises for one faculty holding multiple concurrent roles on one offering in a reportable way.

---

## BR-51: Export Filter/Sort Transparency & Reliability

### Business Rule

Every Excel/PDF export in the app now renders as: **report heading → applied filters (omitted entirely when none are active) → sort order → column headers → data rows**, so a downloaded file is self-explanatory about what it contains without needing to cross-reference the screen state at the time of export. The export button on every list screen is disabled whenever the current filtered result set is empty, preventing a header-only file with zero data rows.

Filter and sort forwarding to export was audited and brought to parity with each screen's on-screen list across all 16 export endpoints in the app. The worst gap found was Enquiry: its export endpoint called an entirely different, narrower query (`findByDateRange`/`findAll`) than the on-screen `/page` endpoint, so exported rows could silently include statuses (e.g. `ADMITTED`, `NOT_INTERESTED`) that weren't even visible on screen under the default filter — fixed by adding `EnquiryService.findAllMatching(...)`, an unpaged sibling of `findPage` sharing the identical `Specification` filter-building block, so export and the on-screen list can never diverge again.

### Scope

- `com.cms.util.export` (new package) — `ExportMetadata` (heading/filter-lines/sort-line builder), `ExcelExportUtil` and `PdfExportUtil` (shared POI/OpenPDF style creation, metadata-block rendering, header-row rendering — the same dark-blue-header/zebra-row palette previously copy-pasted independently across all 16 export services), and `ExportResponseFactory` (collapses the format-branch/filename/`Content-Type`/`Content-Disposition`/exception-handling block that was duplicated across all 17 export controller endpoints).
- All 16 `*ExportService` classes refactored to take an `ExportMetadata` parameter and call into the shared util instead of re-declaring POI/OpenPDF styling.
- Sort forwarding added to every export endpoint that was missing it: Fee Refunds, Receipts, Fee Explorer, Faculty, Commission Explorer, Admission, Student, Equipment, Scholarship Type, Staff Referrer, Agent (each previously silently dropped the on-screen sort order at export time).
- Enquiry export rebuilt on `findAllMatching(...)` to share the exact filter `Specification` as the on-screen `/page` endpoint (see Business Rule above).
- Empty-data export guard added to all 16 screens' `cms-export-button` (`[disabled]="exporting() || totalElements === 0"`), plus a matching early-return + toast in each `onExport()`.
- FK-id filters (program, course, academic year, rack, shelf, speciality) are resolved to their display name server-side for the metadata block via a single `findById` lookup per active filter, not per row.

### Permissions

None — no new endpoints, buttons, or role-gated behavior; every screen's existing `*_EXPORT` permission continues to gate its export action unchanged.

### Migration Notes

None — presentation-layer only, no schema changes.

---

## BR-52: Student Promotion / Progression

### Business Rule

Prior to this BR, no code path existed to move an existing student from one academic year/term into the next — `AcademicYearService.create()` only created brand-new `Cohort`s for new admissions. A second discovery during implementation: `StudentTermEnrollmentServiceImpl.generateEnrollmentsForTermInstance()` (wired into `TermInstanceService.updateTermInstance()`'s transition to `OPEN`) already auto-advanced *every* active student the moment a term opened, computing year-of-study purely from calendar years-since-admission — with **zero regard for arrears, attendance, or exam eligibility**. That blind auto-advance (and its downstream course-registration/fee-demand generation) has been **removed** from the term-open transition; course offering generation is unaffected since it's curriculum-driven, not per-student. Student Promotion is now the sole path that creates the next term's `StudentTermEnrollment`, `CourseRegistration`, and `FeeDemand` rows.

The feature follows the real **INC (Indian Nursing Council) / Dr. MGR Medical University** promotion model, verified via research against current regulations rather than assumed:

- **Subject-wise arrears.** A student can be promoted to the next year while carrying a failed subject forward (`PROMOTED_WITH_ARREARS`). All arrears must be cleared before the student is allowed to enter the program's **Final Year term** — a hard, non-overridable block (`ARREARS_AT_FINAL_YEAR_GATE`).
- **Max duration.** A student cannot be promoted/graduated past **double the program's normal duration** (e.g. 8 years for a 4-year B.Sc. Nursing) — a hard, non-overridable block (`MAX_DURATION_EXCEEDED`).
- **Per-subject detention.** Low attendance (using the existing configurable `AttendanceThresholdService` thresholds — no hardcoded percentage) creates an arrear for that specific subject; it does not by itself force a whole-year repeat. A manual `DETAINED_REPEAT` override remains available for admin discretion (e.g. a max-duration breach, or a case-by-case decision).
- **Pass/fail is external-marks-only for v1.** `ExamResult` gains a persisted `outcome` (`PASS`/`FAIL`), derived from `marksObtained >= 50%` of `Examination.maxMarks` once a result is `PUBLISHED`. INC's rule that internal and external marks must each be passed separately cannot be implemented yet — **Continuous/Internal Assessment (CIA) marks do not exist anywhere in this system.** This is a known, accepted v1 limitation, not an oversight; revisit once CIA marks are built.
- **Mandatory preview before commit.** `POST /student-promotions/preview` is a read-only computation (per-student attendance, carried/new/total arrears, exam outcomes, recommended decision, block reasons) that must be reviewed before `POST /student-promotions/execute` — an irreversible bulk mutation on live student, enrollment, registration, and fee-demand records. `execute` always re-validates every submitted decision against a freshly recomputed preview server-side; it never trusts client-supplied eligibility.
- **Cohort-driven term detection.** Picking a cohort is the only mandatory input for the common case — the screen auto-detects which term instance(s) that cohort currently has `ENROLLED` students in (`GET /student-promotions/active-terms?cohortId=`) and auto-suggests the chronologically next destination term (`GET /student-promotions/suggested-next-term?fromTermInstanceId=`), skipping the academic-year/term cascade entirely when there's exactly one active term (the normal case). The full manual cascade remains available as a fallback (a brand-new cohort with no enrollment yet, no next term created, or a non-standard rollover) — either auto-shown when nothing can be detected, or opted into via "Choose different terms manually".
- **Cohort-level `GRADUATED` status stays manual** (explicit decision) — promotion only ever sets `Student.status = GRADUATED` for individual students at their program's final term with no arrears; it never touches `Cohort.status`.
- **Max-duration breach is block-only.** No automatic `Student.status` change (e.g. `WITHDRAWN`/`EXPELLED`) is applied — that terminal outcome is a separate manual admin action outside this feature.

### Decision Outcomes

| Outcome | Effect |
|---|---|
| `PROMOTED` | No arrears. Old `StudentTermEnrollment` marked `COMPLETED`; a new one created for the destination term (`semesterNumber + 1`); `Student.semester` updated. |
| `PROMOTED_WITH_ARREARS` | Same enrollment transition as `PROMOTED`, but the student carries one or more failed subjects forward. |
| `DETAINED_REPEAT` | No new enrollment. The current `StudentTermEnrollment` stays `ENROLLED`; the student reappears unchanged in the next promotion cycle's preview. |
| `GRADUATED` | Only legal at the program's final term with no arrears. Enrollment marked `COMPLETED`; `Student.status = GRADUATED`. No new enrollment created. |
| `EXCLUDED` | Skipped entirely — no `StudentPromotionDecision` row is written, reported back for transparency. |

Every non-`EXCLUDED` decision writes an audit row to `student_promotion_decisions` (student, cohort, from/to term instance, outcome, a snapshot of arrear subject IDs at decision time, decided-by, decided-at, remarks) — the system's first lifecycle-transition audit trail; `CohortStatus.GRADUATED` and `StudentStatus.GRADUATED` existed as enum values but were previously dead code with no real code path setting them.

### Scope

- `ExamResult.outcome` (new `ExamOutcome` enum column) — computed in `ExamResultService.create()`/`update()`.
- `StudentPromotionDecision` (new entity/table) + `student_promotion_decision_arrears` (arrear-subject snapshot, `@ElementCollection`).
- `StudentPromotionService`/`Impl` — `previewPromotion()`, `executePromotion()`, `getHistoryByCohort()`/`getHistoryByStudent()`.
- `StudentPromotionController` — `GET /student-promotions/active-terms?cohortId=`, `GET /student-promotions/suggested-next-term?fromTermInstanceId=`, `POST /student-promotions/preview`, `POST /student-promotions/execute`, `GET /student-promotions/history?cohortId=|studentId=`.
- `TermInstanceService.updateTermInstance()` — the `OPEN` transition no longer calls `generateEnrollmentsForTermInstance`/`generateRegistrationsForTermInstance`/`generateDemandsForTermInstance`; only `generateOfferingsForTermInstance` remains automatic.
- Frontend: `features/student-promotion/` — a select → preview → result screen (cohort-driven term auto-detection with a manual cascade fallback, a per-student editable decision table, bulk execute with optional course-registration/fee-demand generation), added to the Academics nav group.

### Permissions

- `STUDENT_PROMOTION_VIEW` / `STUDENT_PROMOTION_MANAGE` — new, dedicated (per the operation-wise permission mapping rule), category `EXAMINATION`, auto-granted to existing `EXAM_RESULT_VIEW`/`EXAM_RESULT_MANAGE` holders respectively.

### Migration Notes

- V284 — adds `exam_results.outcome`; backfills existing `PUBLISHED` rows.
- V285 — creates `student_promotion_decisions` + `student_promotion_decision_arrears`.
- V286 — `STUDENT_PROMOTION_VIEW`/`MANAGE` permissions.

### Explicitly Out of Scope

- **Internal/Continuous Assessment (CIA) marks** and the INC rule requiring internal + external to each be passed separately — CIA doesn't exist in this system yet; promotion's pass/fail is external-marks-only for v1.
- **Automatic `Cohort.status = GRADUATED`** — stays a manual, separate action.
- **Automatic terminal student status on max-duration breach** — `execute` blocks the transition only; `WITHDRAWN`/`EXPELLED` is a manual follow-up action.

---

## BR-53: Term Lifecycle Confirmation & Overdue Alerting

### Business Rule

Found during a live debugging session: `2025-2026`'s term instances sat at `PLANNED` while `2026-2027`'s were already fully `LOCKED` — an admin never advanced/enrolled the earlier year on schedule, and nothing in the system surfaced that. Two changes close this gap, both scoped to the `TermInstance` lifecycle (`PLANNED → OPEN → LOCKED`, see `TermInstanceService.validateStatusTransition()` — one-directional, no skipping):

1. **Consequence confirmation.** Advancing a term's status now requires confirming a dialog describing what that transition actually does, so an admin isn't guessing:
   - `PLANNED → OPEN`: "will generate course offerings from the curriculum, making it available for course registration and fee collection."
   - `OPEN → LOCKED`: "is permanent and cannot be undone. It deactivates all course offerings for this term. Make sure exam results are published and fee collection is finalized first."
   Academic Year *creation* itself is not gated by a confirmation — nothing depends on it until a term inside it is advanced.
2. **Overdue alerting.** A daily scheduled job (`AcademicTermAlertService`, cron `0 0 6 * * *`) raises an in-app notification when a `TermInstance` is still `PLANNED` within 14 days of its `startDate`. The alert auto-resolves once an admin advances that term past `PLANNED` — regardless of whether anyone dismissed it first.

This is also the **first shipped slice of BR-28**'s notification-sending backend (pending since 2026-05-16) — see that section for how it fits into the broader category/preference system.

### Notification Design

- Notifications are **broadcast-style**, not per-user rows — `notifications` table holds one row per alert instance; visibility to a given caller is computed at read time from their own category preference plus (for `academicTermAlerts` specifically) whether they hold `ACADEMIC_YEAR_MANAGE`.
- **Idempotent per source.** A partial unique index (`source_type, source_id, category_key WHERE resolved_at IS NULL`) guarantees at most one active alert per term, so the daily job never spams duplicates.
- **Per-user dismissal**, not global — `notification_dismissals (notification_id, user_id)` — so each admin who sees a broadcast alert can independently dismiss their own copy of it without hiding it from others.
- New category `academicTermAlerts` added to `NotificationPreferenceService.CATEGORY_DEFAULTS` (default: on) — respects the existing BR-28 opt-out mechanism.

### Scope

- `Notification` / `NotificationDismissal` (new entities) — `notifications`, `notification_dismissals` tables.
- `AcademicTermAlertService` — daily `@Scheduled` job; raises/auto-resolves `academicTermAlerts` notifications for `PLANNED` terms starting within 14 days.
- `NotificationService` — `getFeed()` (preference + permission + dismissal filtered), `dismiss()`.
- `NotificationPreferenceController` (existing `/notifications` route) — new `GET /notifications/feed`, `POST /notifications/{id}/dismiss`, both self-service (`isAuthenticated()` only, same as the existing `/notifications/preferences` endpoints — no dedicated RBAC permission, per that existing precedent).
- Frontend: the toolbar notification bell (`app.html`/`app.ts`) — previously a hardcoded `notificationCount = signal(0)` with no menu wired to it — now shows a real unread count and a dropdown feed with per-item dismiss.
- `academic-year-form.component.ts`'s `advanceTermStatus()` — wrapped with the existing shared `ConfirmDialogComponent` (same pattern already used for delete confirmations across the app).

### Permissions

- No new permission — `academicTermAlerts` visibility reuses the existing `ACADEMIC_YEAR_MANAGE` permission that already gates the Academic Years screen; viewing/dismissing your own notification feed is self-service, matching the existing `/notifications/preferences` endpoints.

### Migration Notes

- V287 — creates `notifications` + `notification_dismissals`.

### Explicitly Out of Scope

- **Email/other delivery channels** for `academicTermAlerts` — in-app only for now; BR-28's `EMAIL`/`BOTH` channel option exists in the preference schema but nothing sends mail yet for any category.
- **Role-filtered category visibility for the other BR-28 categories** (`feeAlerts`, `admissionUpdates`, etc.) — still unimplemented; this BR only wires up `academicTermAlerts` end-to-end.
- **Real-time push** — the feed loads once on app init; no polling or websocket refresh.

---

> **⚠️ Documentation Policy:** Any changes to business rules, workflows, status transitions, fee logic, or operational processes described in this document must be reflected here **before** the corresponding code change is merged. This document, along with the milestone trackers and manual test cases, must always remain in sync with the implementation.
