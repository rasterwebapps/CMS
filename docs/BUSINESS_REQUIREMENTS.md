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

The fee structure is defined **per program per academic year**. Since fees may vary from year to year, every fee structure entry must be scoped to a specific academic year. When viewing or calculating fees, the system must always reference the fee structure for the **current academic year** associated with the selected program.

### Key Points

1. A fee structure record is uniquely identified by the combination of **program + academic year + fee type**.
2. Fee structures from previous academic years are retained for historical reference but are not used for new enquiries or admissions.
3. When a new academic year begins, administrators must create new fee structure entries for each program. Previous year entries are **not** automatically carried forward.
4. The fee structure screen must allow filtering by both program and academic year.

### Entities Involved

- `FeeStructure` — has `program` (FK) and `academicYear` (FK) fields
- `Program` — defines the program for which fees are configured
- `AcademicYear` — scopes the fee structure to a specific year

### Roles

- **ROLE_ADMIN** — can create, update, and delete fee structures

---

## BR-2: Year-wise Fee Boxes per Program Duration

### Business Rule

When selecting a program in the fee structure screen, the system must dynamically generate **year-wise fee input boxes** based on the program's `durationYears` field. Each box represents one year of the program (e.g., "First Year", "Second Year", "Third Year" for a 3-year program).

### Key Points

1. The number of year-wise fee boxes equals the `durationYears` value of the selected program.
2. Each box is labeled sequentially: "First Year", "Second Year", "Third Year", "Fourth Year", etc.
3. Each year-wise amount is stored as a separate record in the database, linked to the fee structure.
4. The sum of all year-wise amounts should equal the total fee for that fee type.
5. This allows institutions to have different fee amounts for different years of the same program.

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

When the front office selects a program on the enquiry screen, the system must display the **fee structure for that program in the current academic year** as a guideline panel on the side. This helps the front office personnel inform the enquirer about the fees.

### Key Points

1. The fee guideline panel is **read-only** on the enquiry screen — it is for reference only.
2. It shows the total fee, year-wise breakdown (from BR-2), and individual fee type amounts.
3. The guideline values are fetched from the fee structure for the **current (active) academic year**.
4. If no fee structure exists for the selected program in the current academic year, the panel shows a message: "No fee structure defined for this program in the current academic year."
5. The chosen/displayed guideline values must be **saved with the enquiry record** so they can be presented to the admin during fee finalization (BR-6).

### Screen Layout

```
┌─────────────────────────────────┬──────────────────────────────┐
│ Enquiry Form                    │ Fee Structure Guideline       │
│                                 │                              │
│ Name: [___________]             │ Program: B.Tech (CSE)        │
│ Phone: [__________]             │ Academic Year: 2026-27       │
│ Email: [__________]             │ Duration: 4 Years            │
│ Program: [▼ Select Program]     │                              │
│ Source: [▼ Select Source]       │ Total Fee: ₹4,00,000        │
│ Referral Type: [▼ Select]       │                              │
│                                 │ Year-wise Breakdown:         │
│ Remarks: [___________]          │  First Year:  ₹1,20,000     │
│                                 │  Second Year: ₹1,00,000     │
│ [Referral Additional: ₹5,000]  │  Third Year:  ₹90,000       │
│                                 │  Fourth Year: ₹90,000       │
│ Final Calculated Fee: ₹4,05,000│                              │
│                                 │ Fee Types:                   │
│ [Save] [Cancel]                 │  Tuition: ₹3,50,000 *       │
│                                 │  Lab Fee: ₹30,000 *         │
│                                 │  Library: ₹10,000 *         │
│                                 │  Exam Fee: ₹10,000          │
└─────────────────────────────────┴──────────────────────────────┘
```

### Roles

- **ROLE_ADMIN** — can access enquiry form with fee guideline
- Front office users (mapped to ROLE_ADMIN or a future ROLE_FRONT_OFFICE)

---

## BR-4: Referral Type Master

### Business Rule

Referral types must be managed as a **separate master entity** where administrators can add, edit, activate, and deactivate referral types. This replaces the fixed `EnquirySource` enum approach for referral categorization.

### Key Points

1. The referral type master is a standalone CRUD entity (not an enum).
2. Default referral types to be seeded: `WALK_IN`, `PHONE`, `ONLINE`, `AGENT_REFERRAL`, `STAFF`, `ALUMNI`, `PARENT`, `ADVERTISEMENT`.
3. Each referral type has a `guidelineValue` (BigDecimal) — this represents any additional fee amount (commission, discount, or surcharge) associated with that referral type.
4. A `guidelineValue` of zero means no additional fee impact.
5. Referral types can be activated/deactivated (soft delete). Only active types appear in the enquiry form dropdown.
6. The enquiry screen's referral/source field must fetch data from this master table instead of a hardcoded enum.

### Data Model (New)

```
ReferralType:
  - id (PK)
  - name (VARCHAR, unique, e.g., "Staff", "Agent Referral")
  - code (VARCHAR, unique, e.g., "STAFF", "AGENT_REFERRAL")
  - guidelineValue (DECIMAL/BigDecimal, default 0)
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

## BR-5: Referral Guideline Amount & Final Fee Calculation

### Business Rule

When a referral type is selected on the enquiry screen, if the referral type has a non-zero `guidelineValue`, an **additional amount box** must be displayed showing the referral guideline amount. The system must calculate and show the **final fee** by adding the referral guideline amount to the program's total fee.

### Key Points

1. If the selected referral type's `guidelineValue` is **zero**, no additional box is shown.
2. If the `guidelineValue` is **greater than zero**, an additional box labeled "Referral Additional Amount" is displayed with the value pre-filled (editable by the user).
3. The **final calculated fee** = Program Total Fee (from fee structure guideline) + Referral Additional Amount.
4. The final calculated fee is displayed prominently on the enquiry form.
5. Both the referral additional amount and the final calculated fee are saved with the enquiry record.
6. This calculated fee serves as the **starting point** for admin fee finalization (BR-6).

### Calculation Formula

```
Final Fee = Total Program Fee (from fee structure) + Referral Guideline Amount (from referral type)
```

### Example

| Scenario | Program Fee | Referral Type | Guideline Value | Final Fee |
|----------|------------|---------------|-----------------|-----------|
| Walk-in enquiry | ₹4,00,000 | Walk-In | ₹0 | ₹4,00,000 |
| Agent referral | ₹4,00,000 | Agent Referral | ₹15,000 | ₹4,15,000 |
| Staff referral | ₹4,00,000 | Staff | ₹5,000 | ₹4,05,000 |

---

## BR-6: Admin Fee Finalization Workflow

### Business Rule

The enquiry screen is used by the **front office** to capture initial data. Once the enquiry is submitted and the student expresses interest in joining, the **admin** reviews and finalizes the fee structure. The guideline values from the enquiry are presented to the admin, who can modify them.

### Key Points

1. The front office submits the enquiry with: student details, program selection, fee guideline values, referral type, referral additional amount, and final calculated fee.
2. All guideline values chosen by the front office are **saved with the enquiry**.
3. When the admin opens the fee finalization screen, the enquiry's saved guideline values are presented as the starting point.
4. The admin can:
   - **Increase** the fee amount (e.g., special program charges)
   - **Provide a discount** (reduce the fee)
   - **Modify year-wise fee distribution**
   - **Override the referral additional amount**
5. The admin's finalized values are saved separately (not overwriting the original enquiry values) for audit purposes.
6. Upon finalization, the enquiry status automatically transitions to **FEES_FINALIZED**.

### Data to Capture in Enquiry

| Field | Source | Description |
|-------|--------|-------------|
| `feeGuidelineTotal` | Fee Structure | Total fee from fee structure guideline |
| `feeGuidelineYearWise` | Fee Structure | JSON/related records of year-wise breakdown |
| `referralTypeId` | Referral Type Master | Selected referral type |
| `referralAdditionalAmount` | Referral Type | Pre-filled from guideline, may be edited |
| `finalCalculatedFee` | Computed | feeGuidelineTotal + referralAdditionalAmount |

### Data to Capture in Finalization

| Field | Source | Description |
|-------|--------|-------------|
| `finalizedTotalFee` | Admin input | Admin's final fee amount |
| `discountAmount` | Admin input | Discount applied (if any) |
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

After admin finalization (BR-6), the finalized fee data is presented to the **accounting team / cashier** for payment collection. The student may pay the full amount or partial amounts in installments using various payment modes.

### Key Points

1. The cashier sees the finalized fee breakdown: net fee, year-wise amounts, and due dates.
2. Payment can be collected in **full** or in **parts** (installments).
3. Supported payment modes: `CASH`, `CARD`, `UPI`, `NET_BANKING`, `CHEQUE`, `DEMAND_DRAFT`, `SCHOLARSHIP`.
4. Each payment generates a unique **receipt number**.
5. Excess payment on one year's fee automatically **carries forward** to the next year.
6. Late payments beyond the due date incur **penalties** (configurable rate, default ₹100/day).
7. The system tracks total paid, pending amount, and payment status per year.

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
| 2026-04-15 | BR-1 to BR-11 | Initial business requirements documented for fee structure, enquiry workflow, referral types, payment collection, document submission, and student explorer | — |

---

> **⚠️ Documentation Policy:** Any changes to business rules, workflows, status transitions, fee logic, or operational processes described in this document must be reflected here **before** the corresponding code change is merged. This document, along with the milestone trackers and manual test cases, must always remain in sync with the implementation.
