# 📋 CMS Business Requirements

> **College Management System** — Business rules, workflow definitions, and functional requirements for all modules. This document is the single source of truth for business logic and must be updated whenever any business or workflow change is made.

---

## 📋 Table of Contents

- [General Documentation Policy](#-general-documentation-policy)
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

### Key Points

1. A fee structure record is uniquely identified by the combination of **program + academic year + fee type**.
2. **There must be only one fee structure group per course (or program) per academic year.** Creating a second group for the same combination is rejected by the system.
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

### Business Rule

When the front office selects a **program** and then a **course** on the enquiry screen, the system must automatically load and display the **fee for that course in the current academic year** as a read-only guideline. The total fee shown depends on the **student type** selected.

### Key Points

1. The fee guideline is **read-only** on the enquiry screen — it cannot be manually edited by the user.
2. **Flow**: Select Program → Select Course → (optionally) Select Student Type → Fee total auto-loads.
   - Courses shown in the dropdown are filtered to only the courses belonging to the selected program.
   - Fees are only loaded **after a course is selected** (selecting a program alone does not load fees).
3. **Only the total fee is displayed** — no year-wise breakdown or individual fee type amounts.
4. The guideline values are fetched from the fee structure for the **current (active) academic year** filtered by **course**.
5. If no fee structure exists for the selected course, a message is shown: "No fee structures configured for this course."
6. The total fee is saved with the enquiry record as `feeGuidelineTotal` for use during fee finalization (BR-6).
7. The backend is the source of truth: when an enquiry is created or updated, the backend recalculates the fee from the current active academic year's fee structures and does **not** trust client-submitted fee totals.

### Student Type Fee Rules (BR-12)

The total fee displayed depends on the **student type** chosen on the enquiry form:

| Student Type | Fee Types Included |
|---|---|
| **Day Scholar** | Generic fees (TUITION + LAB_FEE + LIBRARY_FEE + EXAMINATION_FEE + MISCELLANEOUS + LATE_FEE) + **TRANSPORT_FEE** |
| **Hosteler** | Generic fees (TUITION + LAB_FEE + LIBRARY_FEE + EXAMINATION_FEE + MISCELLANEOUS + LATE_FEE) + **HOSTEL_FEE** |
| **Not Specified** | Generic fees only (HOSTEL_FEE and TRANSPORT_FEE are excluded) |

- HOSTEL_FEE is included **only for Hostelers**.
- TRANSPORT_FEE is included **only for Day Scholars**.
- Generic fees are always included regardless of student type.
- When student type changes, the fee total updates automatically.

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
   - Student type context (Day Scholar / Hosteler)
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
| **DOCUMENTS_SUBMITTED** | Student has submitted required documents | Documents uploaded/verified (BR-9) |
| **CONVERTED** | Enquiry has been converted to a student record | Enquiry-to-student conversion (BR-10) |
| **CLOSED** | Enquiry closed without conversion | Admin manually closes |

### Status Transition Diagram

```
ENQUIRED
  ├── → INTERESTED → FEES_FINALIZED → FEES_PAID → DOCUMENTS_SUBMITTED → CONVERTED
  │                        │                │
  │                        │                └── → PARTIALLY_PAID → FEES_PAID
  │                        │
  │                        └── (admin can edit finalized fees at any time before CONVERTED)
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
| Documents submitted and verified | FEES_PAID / PARTIALLY_PAID | DOCUMENTS_SUBMITTED |
| Convert to student | DOCUMENTS_SUBMITTED | CONVERTED |
| Close enquiry | Any (except CONVERTED) | CLOSED |

### Note on Previous Statuses

The previous status values (`NEW`, `CONTACTED`, `FEE_DISCUSSED`) are being replaced by this enhanced workflow. The new statuses better reflect the actual business process from enquiry through admission.

---

## BR-9: Submit Documents

### Business Rule

After fees are paid (fully or partially), the student must submit required documents (e.g., 10th certificate, 12th certificate, ID proofs). Once all required documents are submitted and verified, the enquiry status transitions to **DOCUMENTS_SUBMITTED**.

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
2. Each document has a status: `PENDING`, `SUBMITTED`, `VERIFIED`, `REJECTED`.
3. The system tracks which documents are submitted and which are pending.
4. The enquiry status transitions to **DOCUMENTS_SUBMITTED** only when all mandatory documents are submitted.
5. Document verification can be done by the admin.

### Entities Involved

- `EnquiryDocument` (new or linked to existing `AdmissionDocument`) — tracks submit documents per enquiry
- Existing `AdmissionDocument` entity already supports document types and verification workflow

### Permissions

- `DOCUMENT_SUBMISSION_VIEW` — can view document submission status
- `DOCUMENT_SUBMISSION_MANAGE` — can record and verify submitted documents

---

## BR-10: Convert Enquiry to Student

### Business Rule

Once the enquiry reaches **DOCUMENTS_SUBMITTED** status, the system provides the option to **convert the enquiry to a student record**. This creates a full student entity in the system with all data captured during the enquiry process.

### Key Points

1. Conversion is only allowed from **DOCUMENTS_SUBMITTED** status.
2. The student record is created with:
   - Personal details from the enquiry (name, email, phone)
   - Program from the enquiry
   - Admission date set to the conversion date
   - Fee allocation linked to the finalized fee data
   - Documents linked from the enquiry
3. Upon conversion, the enquiry status transitions to **CONVERTED**.
4. The enquiry retains a reference to the created student (`convertedStudentId`).
5. The conversion is **irreversible** — once converted, the enquiry cannot be reverted.
6. A roll number is generated for the student based on the institution's numbering scheme.

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
   - **Department** — filter by department
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

### Business Rule

Every enquiry can optionally capture the **student type** — whether the student will be a **Day Scholar** or a **Hosteler**. This choice affects which fee types are included in the total fee displayed on the enquiry screen.

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
6. The DB role `collegeadmin` is the admission-focused operational role and receives only admission workflow, required master-view, student, and fee completion permissions.
7. Other operational roles and assignments are managed by administrators in the application instead of being pre-seeded as Keycloak realm roles.

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
  ↓  Admin verifies documents
  ↓  Status: DOCUMENTS_SUBMITTED

Step 6: CONVERSION TO STUDENT (Admin)
  ↓  Admin converts enquiry to student
  ↓  Student record created with all data
  ↓  Roll number assigned
  ↓  Fee allocation linked
  ↓  Status: CONVERTED

Step 7: STUDENT EXPLORER
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
| Employee Code / Designation / Department / Status / Joining Date | No — admin-only | N/A | N/A |
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

Faculty document verification must be visible from faculty discovery screens and must protect reviewed evidence from accidental overwrite.

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


## 📝 Change Log

| Date | BR ID(s) | Change Description | Changed By |
|------|----------|-------------------|------------|
| 2026-05-16 | BR-27 | Added permanent admission number generation on successful admission completion/confirmation, academic-year format `ADM-2526-0001`, immutable student reference, receipt display, searchable admission/student screens, and read-only number sequence registry | — |
| 2026-05-14 | BR-26 | Added derived faculty document-review summary on faculty discovery screens, review-status filtering, document workflow entry points, verification lock/audit rules, re-upload reset behavior, and the rule that document review must not create or overload `FacultyStatus` | — |
| 2026-05-14 | BR-25 | Added profile self-service rules: authenticated users can update only their own phone, blood group, and address; profile photos are stored on `app_users` for all roles with JPEG/PNG and 2 MB limits; admin-only academic/employment/login fields remain locked | — |
| 2026-05-12 | BR-24 | RBAC aligned to DB-driven authorization: Keycloak realm exports are identity-only, immutable default roles are limited to `DEV_ADMIN`/`SUPPORT_ADMIN`, and `collegeadmin` is scoped to admission-related DB permissions | — |
| 2026-05-06 | BR-3, BR-5, BR-6, BR-23 | Fixed enquiry fee over-calculation risk: fee guideline lookup is current-academic-year scoped and active-row-only; backend recalculates enquiry fee totals from authoritative fee structures on create/update; referral/agent commission is decoupled from student fee and tracked separately; fee finalization uses backend-calculated totals and enforces exact two-decimal monetary values with discount bounds; frontend aggregation uses integer paise arithmetic to prevent rounding drift | — |
| 2026-05-06 | BR-22 | Enquiry form mandatory fields (Phone, Country, State, Program, Course), Country/State pre-filled to India/Tamil Nadu; referral-linked person search: AGENT_REFERRAL→Agent dropdown (existing), ALUMNI/STUDENT→Student table search, FACULTY→Faculty table search; two new referral type seeds (STUDENT ₹500, FACULTY ₹500); `referred_student_id` and `referred_faculty_id` FK columns added to enquiries; course required conditionally based on program having courses | — |
| 2026-05-06 | BR-14 to BR-21 | Added scholarship management module: (BR-14) Scholarship Type master with INSTITUTION/GOVT_PORTAL application modes, PERCENTAGE/FIXED_AMOUNT/FULL_WAIVER discount types, year-of-study eligibility bounds, renewal flag, govt portal fields; (BR-15) Student scholarship eligibility profile with EWS income auto-flag (₹3,00,000 limit), DBT bank account & Aadhaar fields, admin verification workflow; (BR-16) Scholarship application lifecycle — PENDING → APPROVED → SANCTIONED (govt-portal only) / REJECTED / ON_HOLD / CANCELLED, renewal across academic years, year-of-study restriction; (BR-17) Scholarship disbursement recording with DIRECT_CREDIT / FEE_WAIVER / CHEQUE modes; (BR-18) Optional country/state/district location fields on Enquiry; (BR-19) Transaction reference mandatory for UPI, BANK_TRANSFER, CHEQUE payments via custom `@TransactionReferenceRequired` Bean Validation annotation; (BR-20) FeeType enum expanded with CLINICAL_FEE, BOOK_AND_PACKET_FEE, UNIFORM_AND_SHOES_FEE, UNIVERSITY_REGISTRATION_FEE; LAB_FEE renamed LABORATORY_FEE; (BR-21) Student entity gains isFirstGraduate, fatherEducation, motherEducation fields auto-mirrored from eligibility profile | — |
| 2026-04-27 | BR-13 | Added semester-wise fee collection: yearly fees auto-split into 2 semesters on finalization, payment cascade logic, single receipt per payment (fixed multiple-receipt bug), semester status as primary view in UI, receipt grouping by receipt number | — |
| 2026-04-17 | BR-1, BR-2, BR-3, BR-12 | Fee structure and enquiry enhancements: (1) BR-1 updated — one fee structure group per course+academic year enforced; (2) BR-2 updated — year boxes based on program durationYears, all 8 fee types shown; (3) BR-3 updated — enquiry shows total fee only (no split), filtered by student type; (4) BR-12 added — student type (DAY_SCHOLAR/HOSTELER) on enquiry, controls fee inclusion | — |
| 2026-04-16 | BR-3, BR-4, BR-5, BR-6, BR-7 | Enquiry-to-Fee Workflow enhancements: (1) BR-3 updated for program→course→fee flow with course selection; (2) BR-4 updated — `guidelineValue` replaced with `hasCommission` boolean + `commissionAmount`, `source` enum dropped in favor of `referralType` FK; (3) BR-5 updated to reflect commission-based calculation; (4) BR-6 updated — fee finalization is now enquiry-driven, lists INTERESTED enquiries; (5) BR-7 updated — payment collection lists FEES_FINALIZED enquiries, payments tracked against enquiry | — |
| 2026-04-15 | BR-1 to BR-11 | Initial business requirements documented for fee structure, enquiry workflow, referral types, payment collection, submit documents, and student explorer | — |

---

> **⚠️ Documentation Policy:** Any changes to business rules, workflows, status transitions, fee logic, or operational processes described in this document must be reflected here **before** the corresponding code change is merged. This document, along with the milestone trackers and manual test cases, must always remain in sync with the implementation.
