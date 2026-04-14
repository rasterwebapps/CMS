# Student Fee Workflow — Manual Test Cases

## TC-SFW-001: Finalize student fee allocation

**Preconditions:**
- User is logged in with ROLE_ADMIN
- A student exists (e.g., id=1, rollNumber=CS2024001, program=B.Sc CS, 4 years duration)
- Application is running

**Steps:**
1. Send a POST request to `/api/v1/student-fees/finalize` with body:
   ```json
   {
     "studentId": 1,
     "totalFee": 835000,
     "discountAmount": 0,
     "discountReason": null,
     "agentCommission": 0,
     "yearFees": [
       { "yearNumber": 1, "amount": 235000, "dueDate": "2024-07-31" },
       { "yearNumber": 2, "amount": 200000, "dueDate": "2025-07-31" },
       { "yearNumber": 3, "amount": 200000, "dueDate": "2026-07-31" },
       { "yearNumber": 4, "amount": 200000, "dueDate": "2027-07-31" }
     ]
   }
   ```
2. Verify the response status is 201 Created
3. Verify `status` is `FINALIZED`, `netFee` is `835000`
4. Verify `semesterFees` has 4 entries with correct year numbers and amounts

**Expected Result:**
- Fee allocation is created and finalized with 4 year-wise semester fees
- Each semester fee has the correct amount and due date

**Status:** NOT TESTED

---

## TC-SFW-002: Reject duplicate fee allocation for same student

**Preconditions:**
- Fee allocation already exists for student id=1

**Steps:**
1. Send another POST to `/api/v1/student-fees/finalize` with studentId=1
2. Verify the response status is 409 Conflict or 400 Bad Request

**Expected Result:**
- Error message: "Fee allocation already exists for student: CS2024001"

**Status:** NOT TESTED

---

## TC-SFW-003: Finalize fee with discount and agent commission

**Preconditions:**
- A student exists without fee allocation

**Steps:**
1. POST `/api/v1/student-fees/finalize` with:
   ```json
   {
     "studentId": 2,
     "totalFee": 835000,
     "discountAmount": 50000,
     "discountReason": "Merit scholarship",
     "agentCommission": 25000,
     "yearFees": [
       { "yearNumber": 1, "amount": 200000, "dueDate": "2024-07-31" },
       { "yearNumber": 2, "amount": 195000, "dueDate": "2025-07-31" },
       { "yearNumber": 3, "amount": 195000, "dueDate": "2026-07-31" },
       { "yearNumber": 4, "amount": 195000, "dueDate": "2027-07-31" }
     ]
   }
   ```
2. Verify `netFee` = 785000 (835000 - 50000)
3. Verify `discountAmount` = 50000
4. Verify `agentCommission` = 25000

**Expected Result:**
- Net fee is correctly calculated after discount
- Discount reason and agent commission are stored

**Status:** NOT TESTED

---

## TC-SFW-004: Get semester breakdown for a student

**Preconditions:**
- Fee allocation finalized for student id=1

**Steps:**
1. GET `/api/v1/student-fees/1/semester-breakdown`
2. Verify response contains all 4 year-wise entries
3. Verify each entry shows: amount, dueDate, amountPaid=0, pendingAmount, paymentStatus="PENDING"

**Expected Result:**
- Semester breakdown returned with correct details
- All semesters show PENDING status initially

**Status:** NOT TESTED

---

## TC-SFW-005: Collect single installment payment

**Preconditions:**
- Fee allocation finalized for student id=1 with Year 1 = ₹235000

**Steps:**
1. POST `/api/v1/student-fees/1/collect` with:
   ```json
   {
     "amount": 100000,
     "paymentDate": "2024-06-15",
     "paymentMode": "UPI",
     "transactionReference": "TXN001",
     "remarks": "First installment"
   }
   ```
2. Verify response status is 201
3. Verify `amountPaid` = 100000
4. Verify `allocationSummary` mentions "Year 1"
5. GET `/api/v1/student-fees/1/semester-breakdown` and verify Year 1 shows amountPaid=100000, paymentStatus="PARTIAL"

**Expected Result:**
- Payment recorded against Year 1
- Receipt number generated
- Year 1 status changes to PARTIAL

**Status:** NOT TESTED

---

## TC-SFW-006: Collect payment with carry-forward across semesters

**Preconditions:**
- Fee allocation finalized for student id=1
- Year 1 has ₹135000 pending (already paid ₹100000 of ₹235000)

**Steps:**
1. POST `/api/v1/student-fees/1/collect` with `amount: 200000`
2. Verify `allocationSummary` mentions both "Year 1" and "Year 2"
3. GET semester breakdown: Year 1 should be PAID, Year 2 should be PARTIAL with ₹65000 paid

**Expected Result:**
- ₹135000 applied to Year 1 (making it fully paid)
- ₹65000 carry-forwarded to Year 2
- Both semesters updated correctly

**Status:** NOT TESTED

---

## TC-SFW-007: Reject payment when no pending fees

**Preconditions:**
- All semester fees are fully paid for the student

**Steps:**
1. POST `/api/v1/student-fees/1/collect` with `amount: 50000`
2. Verify error response: "No pending fees found"

**Expected Result:**
- Payment rejected because there are no pending semester fees

**Status:** NOT TESTED

---

## TC-SFW-008: Reject payment for non-finalized allocation

**Preconditions:**
- Student has a DRAFT fee allocation (if applicable), or no allocation at all

**Steps:**
1. POST `/api/v1/student-fees/1/collect` with `amount: 50000`
2. Verify error response about allocation not being finalized

**Expected Result:**
- Payment rejected because allocation is not in FINALIZED status

**Status:** NOT TESTED

---

## TC-SFW-009: Calculate penalties for overdue semester

**Preconditions:**
- Fee allocation finalized with Year 1 due date in the past (e.g., 30 days ago)
- Year 1 has unpaid balance

**Steps:**
1. GET `/api/v1/student-fees/1/penalties`
2. Verify `totalPenalty` is calculated at ₹100/day
3. Verify penalty detail shows: dailyRate=100, overdueDays≥30, penaltyStartDate=dueDate

**Expected Result:**
- Penalty calculated as ₹100 × number of overdue days
- Penalty details include start date, end date, and total

**Status:** NOT TESTED

---

## TC-SFW-010: No penalty for future due dates

**Preconditions:**
- Fee allocation finalized with all due dates in the future

**Steps:**
1. GET `/api/v1/student-fees/1/penalties`
2. Verify `totalPenalty` = 0
3. Verify `penalties` list is empty

**Expected Result:**
- No penalties created for semesters with future due dates

**Status:** NOT TESTED

---

## TC-SFW-011: No penalty for fully paid semesters

**Preconditions:**
- Semester with past due date is fully paid

**Steps:**
1. GET `/api/v1/student-fees/1/penalties`
2. Verify no penalty exists for the fully paid semester

**Expected Result:**
- Fully paid semesters do not incur penalties even if due date has passed

**Status:** NOT TESTED

---

## TC-SFW-012: Fee Explorer — search by roll number

**Preconditions:**
- Students with fee allocations exist

**Steps:**
1. GET `/api/v1/student-fees/explorer?search=CS2024001`
2. Verify response contains exactly 1 student
3. Verify totalFee, totalPaid, totalPending, totalPenalty, allocationStatus fields

**Expected Result:**
- Student found by exact roll number
- Fee summary shows correct totals

**Status:** NOT TESTED

---

## TC-SFW-013: Fee Explorer — search by partial roll number

**Preconditions:**
- Multiple students with roll numbers starting with "CS2024"

**Steps:**
1. GET `/api/v1/student-fees/explorer?search=CS2024`
2. Verify response contains matching students

**Expected Result:**
- All students with matching roll number prefix are returned

**Status:** NOT TESTED

---

## TC-SFW-014: Fee Explorer — list all students

**Preconditions:**
- Students exist in the system

**Steps:**
1. GET `/api/v1/student-fees/explorer` (no search param)
2. Verify response contains all students

**Expected Result:**
- All students returned with fee summaries
- Students without allocations show "NOT_ALLOCATED" status

**Status:** NOT TESTED

---

## TC-SFW-015: Get receipts for a student

**Preconditions:**
- Student has made payments

**Steps:**
1. GET `/api/v1/student-fees/1/receipts`
2. Verify response contains list of receipts
3. Each receipt has: receiptNumber, amountPaid, paymentDate, paymentMode, semesterLabel

**Expected Result:**
- All receipts returned in reverse chronological order
- Each receipt has all required fields

**Status:** NOT TESTED

---

## TC-SFW-016: Get specific receipt by ID

**Preconditions:**
- Student has made payments with known receipt ID

**Steps:**
1. GET `/api/v1/student-fees/1/receipts/1`
2. Verify response contains the specific receipt details

**Expected Result:**
- Receipt returned with correct details for the student

**Status:** NOT TESTED

---

## TC-SFW-017: Immutability — finalized fee cannot be changed

**Preconditions:**
- Fee allocation finalized for student

**Steps:**
1. Verify there is no PUT/PATCH endpoint on `/api/v1/student-fees`
2. Verify the allocation cannot be deleted or modified after finalization
3. Semester fee amounts remain constant regardless of any operations

**Expected Result:**
- Once finalized, the fee allocation is immutable
- No API endpoint exists to modify finalized allocations

**Status:** NOT TESTED

---

## TC-SFW-018: Frontend — Fee Explorer screen

**Preconditions:**
- Application frontend is running
- User is logged in

**Steps:**
1. Navigate to `/student-fees` in the browser
2. Verify the explorer screen loads with a search bar
3. Enter a roll number and verify results appear
4. Click on a student row to view their details

**Expected Result:**
- Fee Explorer page shows search bar and results table
- Search by roll number/program works
- Clicking a student navigates to detail page

**Status:** NOT TESTED

---

## TC-SFW-019: Frontend — Student Fee Detail screen

**Preconditions:**
- Student has finalized fee allocation with payments

**Steps:**
1. Navigate to `/student-fees/{studentId}`
2. Verify summary cards show: Total Fee, Paid, Pending, Penalty
3. Verify semester breakdown shows year-wise cards with status badges
4. Verify receipts table shows payment history
5. Click "Collect Payment" button and verify dialog opens

**Expected Result:**
- Detail page shows comprehensive financial overview
- Semester cards show correct amounts, due dates, and status colors
- Receipt history is displayed

**Status:** NOT TESTED

---

## TC-SFW-020: Frontend — Fee Finalization screen

**Preconditions:**
- User is logged in as ROLE_ADMIN

**Steps:**
1. Navigate to `/student-fees/finalize`
2. Enter student ID, total fee, optional discount/agent commission
3. Add year-wise fee entries with amounts and due dates
4. Click "Finalize"
5. Verify success message and redirection

**Expected Result:**
- Admin can set up student fee allocation
- Year-wise split is correctly configured
- Finalization triggers API call and confirms success

**Status:** NOT TESTED

---

## TC-SFW-021: Frontend — Collect Payment dialog

**Preconditions:**
- User is on student fee detail page

**Steps:**
1. Click "Collect Payment" button
2. Enter amount, select payment mode, set date
3. Optionally add transaction reference and remarks
4. Click "Pay"
5. Verify success and page refresh

**Expected Result:**
- Payment dialog opens with form fields
- After submission, payment is recorded and page refreshes
- Receipt information is shown

**Status:** NOT TESTED

---

## TC-SFW-022: End-to-End Workflow

**Preconditions:**
- Clean system with a student enrolled in B.Sc CS (4 years)

**Steps:**
1. Admin finalizes fee: ₹235000 + ₹200000 + ₹200000 + ₹200000 = ₹835000
2. Student pays ₹100000 (installment 1) → Year 1 becomes PARTIAL
3. Student pays ₹135000 (installment 2) → Year 1 becomes PAID
4. Student pays ₹250000 → ₹200000 to Year 2 (PAID), ₹50000 carry-forward to Year 3 (PARTIAL)
5. Wait for Year 3 due date to pass without full payment
6. Check penalties → ₹100/day penalty calculated
7. Search student in explorer → verify all totals are correct
8. View receipts → 4 receipts generated

**Expected Result:**
- Complete workflow works end-to-end
- Carry-forward logic allocates excess payments correctly
- Penalties calculated on overdue amounts
- Explorer shows accurate financial summary

**Status:** NOT TESTED
