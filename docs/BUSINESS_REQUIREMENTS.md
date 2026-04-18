# 📋 CMS Business Requirements

> **College Management System** — Business rules, workflow definitions, and functional requirements for all modules. This document is the single source of truth for business logic and must be updated whenever any business or workflow change is made.

---

## 📋 Table of Contents

- [General Documentation Policy](#-general-documentation-policy)
- [BR-1: Fee Structure & Academic Year](#br-1-fee-structure--academic-year)
- [BR-2: Year-wise Fee Boxes per Program Duration](#br-2-year-wise-fee-boxes-per-program-duration)
- [BR-3: Fee Structure Guideline on Enquiry Screen](#br-3-fee-structure-guideline-on-enquiry-screen)
- [BR-4: Referral Type Master](#br-4-referral-type-master)
- [BR-5: Referral Guideline Amount & Final Fee Calculation](#br-5-referral-guideline-amount--final-fee-calculation)
- [BR-6: Admin Fee Finalization Workflow](#br-6-admin-fee-finalization-workflow)
- [BR-7: Payment Collection by Accounting Team](#br-7-payment-collection-by-accounting-team)
- [BR-8: Enquiry Status Workflow](#br-8-enquiry-status-workflow)
- [BR-9: Document Submission](#br-9-document-submission)
- [BR-10: Convert Enquiry to Student](#br-10-convert-enquiry-to-student)
- [BR-11: Student Explorer with Filters](#br-11-student-explorer-with-filters)
- [BR-12: Student Type on Enquiry](#br-12-student-type-on-enquiry)
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

### Entities Involved

- `FeeStructure` — has `program` (FK) and `academicYear` (FK) fields
- `Program` — defines the program for which fees are configured
- `AcademicYear` — scopes the fee structure to a specific year

### Roles

- **ROLE_ADMIN** — can create, update, and delete fee structures

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
6. This allows institutions to have different fee amounts for different years of the same program.
7. The year box count is driven by program duration; course selection only scopes which fee structure to save (per program+course+academic year).

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

### Roles

- **ROLE_ADMIN** — can define year-wise fee amounts

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

### Roles

- **ROLE_ADMIN** — can access enquiry form with fee guideline
- Front office users (mapped to ROLE_ADMIN or a future ROLE_FRONT_OFFICE)

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
- `POST /api/v1/referral-types` — Create (ROLE_ADMIN)
- `PUT /api/v1/referral-types/{id}` — Update (ROLE_ADMIN)
- `DELETE /api/v1/referral-types/{id}` — Soft delete / deactivate (ROLE_ADMIN)

### Roles

- **ROLE_ADMIN** — full CRUD on referral types

---

## BR-5: Referral Commission Amount & Final Fee Calculation

### Business Rule

When a referral type is selected on the enquiry screen, if the referral type has `hasCommission=true`, an **additional amount box** must be displayed showing the commission amount. The system must calculate and show the **final fee** by adding the commission amount to the program's total fee.

### Key Points

1. If the selected referral type's `hasCommission` is **false**, no additional box is shown.
2. If `hasCommission` is **true**, an additional box labeled "Commission Amount" is displayed with the value pre-filled from `commissionAmount` (editable by the user).
3. The **final calculated fee** = Program Total Fee (from fee structure guideline) + Commission Amount.
4. The final calculated fee is displayed prominently on the enquiry form.
5. Both the commission amount and the final calculated fee are saved with the enquiry record.
6. This calculated fee serves as the **starting point** for admin fee finalization (BR-6).

### Calculation Formula

```
Final Fee = Total Program Fee (from fee structure) + Commission Amount (from referral type, if hasCommission=true)
```

### Example

| Scenario | Program Fee | Referral Type | Has Commission | Commission Amount | Final Fee |
|----------|------------|---------------|----------------|-------------------|-----------|
| Walk-in enquiry | ₹4,00,000 | Walk-In | No | ₹0 | ₹4,00,000 |
| Agent referral | ₹4,00,000 | Agent Referral | Yes | ₹15,000 | ₹4,15,000 |
| Staff referral | ₹4,00,000 | Staff | No | ₹0 | ₹4,00,000 |

---

## BR-6: Admin Fee Finalization Workflow

### Business Rule

The enquiry screen is used by the **front office** to capture initial data. Once the enquiry is submitted and the student expresses interest in joining (status = INTERESTED), the **admin** reviews and finalizes the fee structure on the **Fee Finalization Screen**. The guideline values from the enquiry are pre-populated; the admin may only apply a **discount** — fees cannot be increased above the guideline.

### Key Points

1. The front office submits the enquiry with: student details, program + course selection, fee guideline values, referral type, commission amount (if applicable), and final calculated fee.
2. All guideline values chosen by the front office are **saved with the enquiry**.
3. The **Fee Finalization Screen** shows a list of enquiries in **INTERESTED** status.
4. The admin selects an enquiry to finalize, and the form is pre-populated with:
   - Total fee from enquiry's `finalCalculatedFee` (or `feeGuidelineTotal`) — **read-only, not editable**
   - Student type context (Day Scholar / Hosteler)
   - Commission info (if referral type has commission)
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
| `feeGuidelineTotal` | Fee Structure | Total fee from fee structure guideline |
| `feeGuidelineYearWise` | Fee Structure | JSON/related records of year-wise breakdown |
| `referralTypeId` | Referral Type Master | Selected referral type |
| `referralAdditionalAmount` | Referral Type | Pre-filled from guideline, may be edited |
| `finalCalculatedFee` | Computed | feeGuidelineTotal + referralAdditionalAmount |
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

### Roles

- **ROLE_ADMIN** — can finalize fees

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

### Roles

- **ROLE_ADMIN** — can collect payments and view all payment data

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

## BR-9: Document Submission

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

- `EnquiryDocument` (new or linked to existing `AdmissionDocument`) — tracks document submission per enquiry
- Existing `AdmissionDocument` entity already supports document types and verification workflow

### Roles

- **ROLE_ADMIN** — can verify documents and track submission status
- Front office — can record document submission

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

### Roles

- **ROLE_ADMIN** — can convert enquiry to student

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

### Roles

- **ROLE_ADMIN** — full access to student explorer
- **ROLE_FACULTY** — can view students in their assigned courses/programs

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

### Roles

- **ROLE_ADMIN** — can set/update student type on enquiry form

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

Step 5: DOCUMENT SUBMISSION
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

## 📝 Change Log

| Date | BR ID(s) | Change Description | Changed By |
|------|----------|-------------------|------------|
| 2026-04-17 | BR-1, BR-2, BR-3, BR-12 | Fee structure and enquiry enhancements: (1) BR-1 updated — one fee structure group per course+academic year enforced; (2) BR-2 updated — year boxes based on program durationYears, all 8 fee types shown; (3) BR-3 updated — enquiry shows total fee only (no split), filtered by student type; (4) BR-12 added — student type (DAY_SCHOLAR/HOSTELER) on enquiry, controls fee inclusion | — |
| 2026-04-16 | BR-3, BR-4, BR-5, BR-6, BR-7 | Enquiry-to-Fee Workflow enhancements: (1) BR-3 updated for program→course→fee flow with course selection; (2) BR-4 updated — `guidelineValue` replaced with `hasCommission` boolean + `commissionAmount`, `source` enum dropped in favor of `referralType` FK; (3) BR-5 updated to reflect commission-based calculation; (4) BR-6 updated — fee finalization is now enquiry-driven, lists INTERESTED enquiries; (5) BR-7 updated — payment collection lists FEES_FINALIZED enquiries, payments tracked against enquiry | — |
| 2026-04-15 | BR-1 to BR-11 | Initial business requirements documented for fee structure, enquiry workflow, referral types, payment collection, document submission, and student explorer | — |

---

> **⚠️ Documentation Policy:** Any changes to business rules, workflows, status transitions, fee logic, or operational processes described in this document must be reflected here **before** the corresponding code change is merged. This document, along with the milestone trackers and manual test cases, must always remain in sync with the implementation.
