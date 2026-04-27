# Phase 5 — Fee Collection Manual Test Cases

## TC-FEE5-001: Generate fee demands for a term instance

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN
- A TermInstance exists in OPEN status
- Fee structures are configured for the relevant program and academic year
- A billing schedule is configured for the term type (ODD/EVEN)
- At least one student has an ENROLLED StudentTermEnrollment for the term

**Steps:**
1. Send `POST /api/fee-demands/generate?termInstanceId={id}`
2. Verify response contains `{ "demandsCreated": N }` where N > 0
3. Send `GET /api/fee-demands?termInstanceId={id}`
4. Verify the list contains one demand per enrolled student
5. Verify each demand has `status: "UNPAID"`, `paidAmount: 0`, and non-zero `totalAmount`
6. Verify `dueDate` matches the billing schedule configured for the term

**Expected Result:**
- Fee demands are created, one per enrolled student
- `totalAmount` = sum of FeeStructureYearAmount.amount for the student's yearOfStudy
- `dueDate` = TermBillingSchedule.dueDate for the matching AY + termType

**Status:** NOT TESTED

---

## TC-FEE5-002: Generate fee demands is idempotent

**Preconditions:**
- Fee demands have already been generated for a term instance

**Steps:**
1. Send `POST /api/fee-demands/generate?termInstanceId={id}` again
2. Verify response contains `{ "demandsCreated": 0 }`
3. Verify no duplicate demands appear for `GET /api/fee-demands?termInstanceId={id}`

**Expected Result:**
- Second call returns 0 created, no duplicates

**Status:** NOT TESTED

---

## TC-FEE5-003: Auto-generate demands when term is OPEN

**Preconditions:**
- A TermInstance is in PLANNED status
- Fee structures and billing schedules are configured

**Steps:**
1. Send `PATCH /api/term-instances/{id}/status` with `{ "status": "OPEN" }`
2. Verify response `200 OK`
3. Send `GET /api/fee-demands?termInstanceId={id}`
4. Verify fee demands were auto-generated (same as manual generate)

**Expected Result:**
- When term status advances to OPEN, demands are auto-generated along with enrollments, course offerings, and course registrations

**Status:** NOT TESTED

---

## TC-FEE5-004: Error when no fee structure configured

**Preconditions:**
- A TermInstance is OPEN
- No FeeStructure is configured for the program + AY combination

**Steps:**
1. Send `POST /api/fee-demands/generate?termInstanceId={id}`
2. Verify response is `400` or `422`
3. Verify error message mentions "No fee plan configured"

**Expected Result:**
- Generation fails with descriptive error message

**Status:** NOT TESTED

---

## TC-FEE5-005: Error when no billing schedule configured

**Preconditions:**
- A TermInstance is OPEN
- No TermBillingSchedule is configured for the academic year + term type

**Steps:**
1. Send `POST /api/fee-demands/generate?termInstanceId={id}`
2. Verify response is `400` or `422`
3. Verify error message mentions "billing schedule"

**Expected Result:**
- Generation fails with descriptive error message

**Status:** NOT TESTED

---

## TC-FEE5-006: Record a fee payment (on-time, no late fee)

**Preconditions:**
- A FeeDemand exists with `status: "UNPAID"`
- Payment date is before `dueDate + graceDays`

**Steps:**
1. Send `POST /api/term-fee-payments` with body:
   ```json
   {
     "feeDemandId": 1,
     "paymentDate": "2026-07-25",
     "amountPaid": 50000,
     "paymentMode": "CASH"
   }
   ```
2. Verify response `201 Created` with `lateFeeApplied: 0`
3. Verify `receiptNumber` follows pattern `RCP-YYYYMMDD-NNNN`
4. Verify the `FeeDemand` status is updated to `PAID` (if fully paid) or `PARTIAL`

**Expected Result:**
- Payment recorded with 0 late fee
- Receipt number generated automatically
- Demand status updated

**Status:** NOT TESTED

---

## TC-FEE5-007: Record a fee payment (late, FLAT late fee)

**Preconditions:**
- A FeeDemand exists with `status: "UNPAID"`, due date `2026-07-31`
- TermBillingSchedule has `graceDays: 7`, `lateFeeType: FLAT`, `lateFeeAmount: 500`
- Payment date is `2026-08-15` (after `2026-07-31 + 7 = 2026-08-07`)

**Steps:**
1. Send `POST /api/term-fee-payments` with `paymentDate: "2026-08-15"`, `amountPaid: 50000`
2. Verify `lateFeeApplied: 500` in response
3. Verify `totalCollected = amountPaid + lateFeeApplied = 50500`

**Expected Result:**
- Late fee of ₹500 applied (flat)

**Status:** NOT TESTED

---

## TC-FEE5-008: Record a fee payment (late, PER_DAY late fee)

**Preconditions:**
- TermBillingSchedule has `graceDays: 0`, `lateFeeType: PER_DAY`, `lateFeeAmount: 100`
- Due date `2026-07-31`, payment date `2026-08-05` (5 days late)

**Steps:**
1. Send `POST /api/term-fee-payments` with `paymentDate: "2026-08-05"`, `amountPaid: 50000`
2. Verify `lateFeeApplied: 500` (5 days × ₹100)

**Expected Result:**
- Late fee = 5 × 100 = ₹500

**Status:** NOT TESTED

---

## TC-FEE5-009: Cannot record payment on a PAID demand

**Preconditions:**
- A FeeDemand exists with `status: "PAID"`

**Steps:**
1. Send `POST /api/term-fee-payments` with that demand's ID
2. Verify response `422` or `400`
3. Verify error message mentions "already fully paid"

**Expected Result:**
- Payment rejected with descriptive error

**Status:** NOT TESTED

---

## TC-FEE5-010: Partial payment changes demand status to PARTIAL

**Preconditions:**
- A FeeDemand exists with `totalAmount: 50000`, `status: UNPAID`

**Steps:**
1. Send `POST /api/term-fee-payments` with `amountPaid: 20000`
2. Verify demand `status` becomes `PARTIAL`
3. Verify demand `paidAmount = 20000`
4. Verify demand `outstandingAmount = 30000`

**Expected Result:**
- Demand status = PARTIAL, outstanding = 30000

**Status:** NOT TESTED

---

## TC-FEE5-011: Get outstanding fees report

**Preconditions:**
- Multiple demands exist for a term, with various statuses

**Steps:**
1. Send `GET /api/fee-reports/outstanding?termInstanceId={id}`
2. Verify response only includes demands with `status != PAID`
3. Verify each item includes `outstandingAmount`, `studentName`, `cohortCode`

**Expected Result:**
- Only UNPAID and PARTIAL demands are returned

**Status:** NOT TESTED

---

## TC-FEE5-012: Get collection summary report

**Preconditions:**
- Multiple demands exist for a term across multiple programs

**Steps:**
1. Send `GET /api/fee-reports/collection-summary?termInstanceId={id}`
2. Verify response grouped by program
3. Verify each group shows: `totalDemands`, `totalAmount`, `collectedAmount`, `outstandingAmount`, `paidCount`, `partialCount`, `unpaidCount`

**Expected Result:**
- Aggregated summary per program is returned

**Status:** NOT TESTED

---

## TC-FEE5-013: Get late fee collection report

**Preconditions:**
- Some payments have `lateFeeApplied > 0`

**Steps:**
1. Send `GET /api/fee-reports/late-fee-collection?termInstanceId={id}`
2. Verify only payments with `lateFeeApplied > 0` are returned

**Expected Result:**
- Only late-fee payments shown

**Status:** NOT TESTED

---

## TC-FEE5-014: Get student fee ledger

**Preconditions:**
- A student has multiple term enrollments with demands and payments across terms

**Steps:**
1. Send `GET /api/fee-reports/student-ledger?studentId={id}`
2. Verify response contains `studentId`, `studentName`, `entries`
3. Verify each entry has `termLabel`, `totalAmount`, `paidAmount`, `outstandingAmount`, `dueDate`, `status`, `payments[]`

**Expected Result:**
- Full fee history across all terms shown

**Status:** NOT TESTED

---

## TC-FEE5-015: Frontend — Fee Demands section on Term Instance detail

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An academic year has a term in OPEN or LOCKED status
- Fee demands have been generated

**Steps:**
1. Navigate to `/academic-years/{id}`
2. Scroll to the "Fee Demands" section
3. Verify a table shows: Student, Cohort, Total, Due Date, Paid, Outstanding, Status
4. Use the status filter dropdown to filter by UNPAID/PARTIAL/PAID
5. Click "Record Payment" for an unpaid demand
6. Fill in: Amount Paid, Payment Date, Payment Mode, Remarks
7. Click "Record Payment"
8. Verify toast notification: "Payment recorded! Receipt: RCP-..."
9. Verify demand status updates in the table

**Expected Result:**
- Fee demands displayed and filterable
- Payment dialog works and updates demand status

**Status:** NOT TESTED

---

## TC-FEE5-016: Frontend — Generate Fee Demands button

**Preconditions:**
- A term is in OPEN status
- No fee demands exist yet

**Steps:**
1. Navigate to `/academic-years/{id}`
2. See the "Generate Fee Demands" button (only visible for OPEN terms)
3. Click the button
4. Verify toast: "Generated N fee demand(s)"
5. Verify the table populates with demands

**Expected Result:**
- Button visible for OPEN terms; generates demands on click

**Status:** NOT TESTED

---

## TC-FEE5-017: Frontend — Student Fee Ledger tab

**Preconditions:**
- A student has fee demands and payments

**Steps:**
1. Navigate to `/students/{id}`
2. Click the "Fee Ledger" tab
3. Verify per-term entries showing Total, Paid, Outstanding, Due Date, Status
4. Verify payment receipts listed under each entry

**Expected Result:**
- Fee ledger tab shows complete payment history per term

**Status:** NOT TESTED

---

## TC-FEE5-018: Frontend — Fee Reports page

**Preconditions:**
- Multiple students have fee demands and payments

**Steps:**
1. Navigate to `/fee-reports`
2. **Outstanding Fees tab**: Select AY and term, click "Load Report" → verify table
3. **Collection Summary tab**: Load report → verify grouped by program with counts
4. **Late Fees tab**: Load report → verify only late fee payments shown
5. **Student Ledger tab**: Enter a student ID, click "Load Ledger" → verify per-term entries
6. Click "CSV" export button → verify CSV downloads
7. Click "Print" button → verify print dialog opens

**Expected Result:**
- All four report types work correctly with proper data and export functionality

**Status:** NOT TESTED
