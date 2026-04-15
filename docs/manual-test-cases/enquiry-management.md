# Enquiry Management — Manual Test Cases

## TC-ENQ-001: List all enquiries

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Application is running

**Steps:**
1. Send a GET request to `/api/v1/enquiries`
2. Verify the response status is 200 OK
3. Verify the response contains a list of enquiries

**Expected Result:**
- A list of enquiries is returned with all fields including student name, contact info, interested program, status, and source

**Status:** NOT TESTED

---

## TC-ENQ-002: Create a new enquiry

**Preconditions:**
- User is logged in with ROLE_ADMIN
- At least one program exists

**Steps:**
1. Send a POST request to `/api/v1/enquiries` with body:
   ```json
   {
     "name": "Jane Doe",
     "phone": "9876543210",
     "email": "jane@example.com",
     "programId": 1,
     "enquiryDate": "2024-06-15",
     "source": "WALK_IN",
     "status": "ENQUIRED",
     "remarks": "Interested in B.Sc Nursing"
   }
   ```
2. Verify the response status is 201 Created
3. Verify the returned object has the correct values

**Expected Result:**
- Enquiry is created with status ENQUIRED and returned with an ID

**Status:** NOT TESTED

---

## TC-ENQ-003: Get enquiry by ID

**Preconditions:**
- User is logged in with ROLE_ADMIN
- At least one enquiry exists

**Steps:**
1. Send a GET request to `/api/v1/enquiries/{id}`
2. Verify the response status is 200 OK

**Expected Result:**
- The enquiry details are returned

**Status:** NOT TESTED

---

## TC-ENQ-004: Update an enquiry

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists

**Steps:**
1. Send a PUT request to `/api/v1/enquiries/{id}` with updated status and remarks
2. Verify the response status is 200 OK
3. Verify the enquiry fields are updated

**Expected Result:**
- Enquiry is updated successfully (e.g., status changed from NEW to CONTACTED)

**Status:** NOT TESTED

---

## TC-ENQ-005: Update enquiry status workflow

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists with status ENQUIRED

**Steps:**
1. Update enquiry status to CONTACTED
2. Update enquiry status to FEE_DISCUSSED
3. Update enquiry status to INTERESTED
4. Update enquiry status to CONVERTED
5. Verify each transition succeeds

**Expected Result:**
- Enquiry status follows the workflow: NEW → CONTACTED → FEE_DISCUSSED → INTERESTED → CONVERTED

**Status:** NOT TESTED

---

## TC-ENQ-006: Delete an enquiry

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists

**Steps:**
1. Send a DELETE request to `/api/v1/enquiries/{id}`
2. Verify the response status is 204 No Content

**Expected Result:**
- Enquiry is deleted

**Status:** NOT TESTED

---

## TC-ENQ-007: Filter enquiries by status

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Enquiries with different statuses exist

**Steps:**
1. Send a GET request to `/api/v1/enquiries?status=NEW`
2. Verify only enquiries with status ENQUIRED are returned

**Expected Result:**
- Filtered enquiry list is returned

**Status:** NOT TESTED

---

## TC-ENQ-008: Frontend — Enquiry list page

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Navigate to `/enquiries`
2. Verify the enquiry list page loads with a table
3. Verify search/filter functionality works
4. Verify status badges are displayed (e.g., NEW, CONTACTED)

**Expected Result:**
- Enquiries are displayed in a sortable, filterable table with status indicators

**Status:** NOT TESTED

---

## TC-ENQ-009: Frontend — Enquiry form (create/edit)

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Navigate to `/enquiries/new`
2. Fill in student name, phone, email, select program, source, and remarks
3. Click Save
4. Verify redirect to enquiry list
5. Verify the new enquiry appears with status ENQUIRED

**Expected Result:**
- New enquiry is created and shown in the list

**Status:** NOT TESTED

---

## TC-ENQ-010: Frontend — Convert enquiry to student

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists with status INTERESTED or CONVERTED

**Steps:**
1. Navigate to `/enquiries`
2. Click "Convert to Student" action on an enquiry
3. Verify a student form opens pre-populated with enquiry data (name, email, phone, program)
4. Complete additional student fields and save

**Expected Result:**
- Student is created with data from the enquiry; enquiry status is updated to CONVERTED

**Status:** NOT TESTED

---

## TC-ENQ-011: Return 404 for non-existent enquiry

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Send a GET request to `/api/v1/enquiries/99999`
2. Verify the response status is 404 Not Found

**Expected Result:**
- A 404 error response is returned

**Status:** NOT TESTED

---

## TC-ENQ-012: Filter enquiries by date range

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Enquiries exist with various enquiry dates

**Steps:**
1. Send a GET request to `/api/v1/enquiries?fromDate=2024-06-01&toDate=2024-06-30`
2. Verify the response status is 200 OK
3. Verify only enquiries with `enquiryDate` between 2024-06-01 and 2024-06-30 are returned

**Expected Result:**
- Only enquiries within the specified date range are returned

**Status:** NOT TESTED

---

## TC-ENQ-013: Filter enquiries by date range and status

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Enquiries exist with various dates and statuses

**Steps:**
1. Send a GET request to `/api/v1/enquiries?fromDate=2024-06-01&toDate=2024-06-30&status=NEW`
2. Verify the response status is 200 OK
3. Verify only NEW enquiries within the date range are returned

**Expected Result:**
- Only enquiries matching both the date range and status filter are returned

**Status:** NOT TESTED

---

## TC-ENQ-014: Update enquiry status via PATCH endpoint

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists with status ENQUIRED

**Steps:**
1. Send a PATCH request to `/api/v1/enquiries/{id}/status?status=CONTACTED`
2. Verify the response status is 200 OK
3. Verify the returned enquiry has status CONTACTED

**Expected Result:**
- Enquiry status is updated to CONTACTED

**Status:** NOT TESTED

---

## TC-ENQ-015: Frontend — Date range filter on enquiry list

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Enquiries exist with various dates

**Steps:**
1. Navigate to `/enquiries`
2. Verify the date range filter defaults to the current month (first day to last day)
3. Change the From Date and To Date using the date pickers
4. Verify the list updates to show only enquiries within the selected date range
5. Click the refresh/reset button next to the date filters
6. Verify the date range resets to the current month

**Expected Result:**
- Enquiry list filters by date range; reset button restores default month range

**Status:** NOT TESTED

---

## TC-ENQ-016: Frontend — Status change from list via chip dropdown

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Enquiries exist with status ENQUIRED

**Steps:**
1. Navigate to `/enquiries`
2. Click on the status chip for an enquiry with status ENQUIRED
3. Verify a dropdown menu appears with valid next statuses (CONTACTED, NOT_INTERESTED, CLOSED)
4. Select CONTACTED from the dropdown
5. Verify a success snackbar message appears
6. Verify the chip now shows CONTACTED

**Expected Result:**
- Status is updated inline from the list view without navigating away

**Status:** NOT TESTED

---

## TC-ENQ-017: Frontend — CONVERTED status cannot be changed

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists with status CONVERTED

**Steps:**
1. Navigate to `/enquiries`
2. Verify the CONVERTED status chip does NOT show a dropdown arrow
3. Verify clicking the chip does NOT open a status change menu

**Expected Result:**
- CONVERTED status is read-only and cannot be changed from the list view

**Status:** NOT TESTED

---

## TC-ENQ-018: Frontend — Fee structure panel on enquiry form

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Programs and fee structures exist in the system

**Steps:**
1. Navigate to `/enquiries/new`
2. Select a program from the Program dropdown
3. Verify the Fee Structure panel appears on the right side
4. Verify it shows: program name, duration in years, total program fees
5. Verify year-wise fee split is displayed (total ÷ duration years)
6. Verify fee breakdown shows individual fee types with amounts
7. Verify mandatory fees are marked with an asterisk (*)

**Expected Result:**
- Fee structure panel dynamically loads and displays when a program is selected

**Status:** NOT TESTED

---

## TC-ENQ-019: Frontend — Agent selection for AGENT_REFERRAL source

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Active agents exist in the system

**Steps:**
1. Navigate to `/enquiries/new`
2. Select AGENT_REFERRAL as the Source
3. Verify the Agent dropdown field appears
4. Select an agent from the dropdown
5. Change the source to WALK_IN
6. Verify the Agent dropdown field disappears
7. Submit the form and verify the agent is saved (or cleared) correctly

**Expected Result:**
- Agent field is conditionally shown only for AGENT_REFERRAL source

**Status:** NOT TESTED

---

## TC-ENQ-020: Frontend — Convert enquiry to student from list

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists with status INTERESTED or FEE_DISCUSSED

**Steps:**
1. Navigate to `/enquiries`
2. Verify the "Convert to Student" (school icon) action button appears for INTERESTED/FEE_DISCUSSED enquiries
3. Verify the button does NOT appear for NEW, CONTACTED, CONVERTED, NOT_INTERESTED, or CLOSED enquiries
4. Click the "Convert to Student" button
5. Verify navigation to `/students/new?fromEnquiry={enquiryId}`

**Expected Result:**
- Convert action navigates to the student creation form with the enquiry ID as a query parameter

**Status:** NOT TESTED

---

## TC-ENQ-021: Filter enquiries by source

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Enquiries exist with various sources (WALK_IN, PHONE, ONLINE, AGENT_REFERRAL)

**Steps:**
1. Send a GET request to `/api/v1/enquiries?source=WALK_IN`
2. Verify the response status is 200 OK
3. Verify only enquiries with source WALK_IN are returned

**Expected Result:**
- Filtered enquiry list by source is returned

**Status:** NOT TESTED

---

## TC-ENQ-022: Fee structure guideline display on program selection

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Programs with fee structures for the current academic year exist
- Ref: [BR-3](../BUSINESS_REQUIREMENTS.md#br-3-fee-structure-guideline-on-enquiry-screen)

**Steps:**
1. Navigate to `/enquiries/new`
2. Select a program from the Program dropdown
3. Verify the Fee Structure Guideline panel appears on the right side
4. Verify it shows: program name, academic year, duration in years
5. Verify total program fee is displayed
6. Verify year-wise fee breakdown matches the program's duration (e.g., 4 boxes for a 4-year program)
7. Verify individual fee type amounts are shown with mandatory fees marked

**Expected Result:**
- Fee structure guideline panel dynamically loads when a program is selected, showing current academic year's fee structure with year-wise breakdown

**Status:** NOT TESTED

---

## TC-ENQ-023: No fee structure available for program

**Preconditions:**
- User is logged in with ROLE_ADMIN
- A program exists without fee structure for the current academic year

**Steps:**
1. Navigate to `/enquiries/new`
2. Select the program without fee structure
3. Verify the fee guideline panel shows "No fee structure defined for this program in the current academic year"

**Expected Result:**
- Appropriate message displayed when no fee structure exists

**Status:** NOT TESTED

---

## TC-ENQ-024: Referral type selection from master

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Referral types exist in the referral type master
- Ref: [BR-4](../BUSINESS_REQUIREMENTS.md#br-4-referral-type-master)

**Steps:**
1. Navigate to `/enquiries/new`
2. Verify the Referral Type dropdown is populated from the referral type master (not hardcoded)
3. Verify only active referral types are shown
4. Select a referral type (e.g., "Staff")

**Expected Result:**
- Referral type dropdown shows all active referral types from the master table

**Status:** NOT TESTED

---

## TC-ENQ-025: Referral additional amount box display

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Referral types exist: one with guidelineValue > 0 (e.g., "Staff" with ₹5,000) and one with guidelineValue = 0 (e.g., "Walk-In")
- A program with fee structure is selected
- Ref: [BR-5](../BUSINESS_REQUIREMENTS.md#br-5-referral-guideline-amount--final-fee-calculation)

**Steps:**
1. Navigate to `/enquiries/new` and select a program
2. Select the "Walk-In" referral type (guidelineValue = 0)
3. Verify NO additional amount box is shown
4. Select the "Staff" referral type (guidelineValue = ₹5,000)
5. Verify the "Referral Additional Amount" box appears with ₹5,000 pre-filled
6. Verify the Final Calculated Fee = Program Total Fee + ₹5,000

**Expected Result:**
- Additional amount box shown only for referral types with non-zero guidelineValue; final fee calculated correctly

**Status:** NOT TESTED

---

## TC-ENQ-026: Final fee calculation

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Program fee = ₹4,00,000; Referral type "Agent Referral" with guidelineValue = ₹15,000
- Ref: [BR-5](../BUSINESS_REQUIREMENTS.md#br-5-referral-guideline-amount--final-fee-calculation)

**Steps:**
1. Navigate to `/enquiries/new`
2. Select the program (₹4,00,000 total fee)
3. Select "Agent Referral" as referral type
4. Verify the additional amount box shows ₹15,000
5. Verify the Final Calculated Fee shows ₹4,15,000
6. Modify the additional amount to ₹20,000
7. Verify the Final Calculated Fee updates to ₹4,20,000

**Expected Result:**
- Final fee = Program Total Fee + Referral Additional Amount; updates dynamically when additional amount is changed

**Status:** NOT TESTED

---

## TC-ENQ-027: Save enquiry with fee guideline values

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Program and referral type selected with fee calculations visible

**Steps:**
1. Fill in all enquiry fields including name, phone, program, referral type
2. Click Save
3. Verify the enquiry is saved with status ENQUIRED
4. Retrieve the saved enquiry and verify `feeGuidelineTotal`, `referralAdditionalAmount`, and `finalCalculatedFee` are stored

**Expected Result:**
- All fee guideline values are persisted with the enquiry record

**Status:** NOT TESTED

---

## TC-ENQ-028: Enquiry status workflow — new statuses

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists with status ENQUIRED
- Ref: [BR-8](../BUSINESS_REQUIREMENTS.md#br-8-enquiry-status-workflow)

**Steps:**
1. Mark the enquiry as INTERESTED
2. Finalize fees (admin) → verify status changes to FEES_FINALIZED
3. Collect partial payment → verify status changes to PARTIALLY_PAID
4. Collect remaining payment → verify status changes to FEES_PAID
5. Submit all required documents → verify status changes to DOCUMENTS_SUBMITTED
6. Convert to student → verify status changes to CONVERTED

**Expected Result:**
- Status transitions follow: ENQUIRED → INTERESTED → FEES_FINALIZED → PARTIALLY_PAID → FEES_PAID → DOCUMENTS_SUBMITTED → CONVERTED

**Status:** NOT TESTED

---

## TC-ENQ-029: Admin fee finalization

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists with status INTERESTED and saved fee guideline values
- Ref: [BR-6](../BUSINESS_REQUIREMENTS.md#br-6-admin-fee-finalization-workflow)

**Steps:**
1. Open the fee finalization screen for the enquiry
2. Verify the enquiry's saved guideline values are displayed as starting point
3. Modify the total fee (e.g., add ₹10,000)
4. Add a discount of ₹5,000 with reason "Merit scholarship"
5. Adjust year-wise distribution
6. Click Finalize
7. Verify the enquiry status changes to FEES_FINALIZED
8. Verify finalized values are saved separately from original guideline values

**Expected Result:**
- Admin can review, modify, and finalize fees; original guideline values preserved for audit

**Status:** NOT TESTED

---

## TC-ENQ-030: Payment collection (full payment)

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists with status FEES_FINALIZED
- Ref: [BR-7](../BUSINESS_REQUIREMENTS.md#br-7-payment-collection-by-accounting-team)

**Steps:**
1. Open payment collection for the enquiry
2. Verify the finalized fee breakdown is displayed
3. Collect full payment via CASH
4. Verify a receipt number is generated
5. Verify the enquiry status changes to FEES_PAID

**Expected Result:**
- Full payment collected, receipt generated, status updated to FEES_PAID

**Status:** NOT TESTED

---

## TC-ENQ-031: Payment collection (partial payment)

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists with status FEES_FINALIZED
- Ref: [BR-7](../BUSINESS_REQUIREMENTS.md#br-7-payment-collection-by-accounting-team)

**Steps:**
1. Open payment collection for the enquiry
2. Collect partial payment (e.g., ₹50,000 of ₹4,00,000)
3. Verify the enquiry status changes to PARTIALLY_PAID
4. Verify pending amount is correctly calculated
5. Collect another partial payment
6. Verify both receipts are generated

**Expected Result:**
- Partial payments tracked, status reflects PARTIALLY_PAID, pending amount correctly calculated

**Status:** NOT TESTED

---

## TC-ENQ-032: Document submission tracking

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists with status FEES_PAID or PARTIALLY_PAID
- Ref: [BR-9](../BUSINESS_REQUIREMENTS.md#br-9-document-submission)

**Steps:**
1. Open document submission for the enquiry
2. Verify the list of required documents is shown (10th, 12th, TC, etc.)
3. Upload a 10th certificate document
4. Verify the document status changes from PENDING to SUBMITTED
5. Admin verifies the document → status changes to VERIFIED
6. Upload and verify all mandatory documents
7. Verify the enquiry status changes to DOCUMENTS_SUBMITTED

**Expected Result:**
- All documents tracked; status transitions to DOCUMENTS_SUBMITTED when all mandatory documents are submitted

**Status:** NOT TESTED

---

## TC-ENQ-033: Convert enquiry to student (enhanced)

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists with status DOCUMENTS_SUBMITTED
- Ref: [BR-10](../BUSINESS_REQUIREMENTS.md#br-10-convert-enquiry-to-student)

**Steps:**
1. Navigate to the enquiry list
2. Click "Convert to Student" for the DOCUMENTS_SUBMITTED enquiry
3. Verify the student creation form is pre-populated with enquiry data
4. Complete any additional fields
5. Save the student
6. Verify the enquiry status changes to CONVERTED
7. Verify the student record includes fee allocation linked to finalized fees
8. Verify a roll number is generated

**Expected Result:**
- Student created with all enquiry data, fee allocation linked, roll number assigned, enquiry marked CONVERTED

**Status:** NOT TESTED

---

## TC-ENQ-034: Conversion blocked for non-DOCUMENTS_SUBMITTED enquiry

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Enquiries exist with various statuses (ENQUIRED, INTERESTED, FEES_FINALIZED, etc.)

**Steps:**
1. Navigate to the enquiry list
2. Verify "Convert to Student" action is NOT available for ENQUIRED, INTERESTED, FEES_FINALIZED, FEES_PAID, or PARTIALLY_PAID statuses
3. Verify it is only available for DOCUMENTS_SUBMITTED status

**Expected Result:**
- Conversion to student is only allowed from DOCUMENTS_SUBMITTED status

**Status:** NOT TESTED

---

## TC-ENQ-035: Student explorer with filters

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Multiple students exist with different programs, statuses, and fee statuses
- Ref: [BR-11](../BUSINESS_REQUIREMENTS.md#br-11-student-explorer-with-filters)

**Steps:**
1. Navigate to the Student Explorer screen
2. Verify all students are listed by default
3. Filter by program → verify only students in that program are shown
4. Filter by department → verify filtering works
5. Filter by fee status (PAID/PARTIALLY_PAID/OVERDUE) → verify filtering works
6. Filter by student status (ACTIVE/ON_LEAVE/etc.) → verify filtering works
7. Search by name → verify search works
8. Search by roll number → verify search works
9. Verify pagination and sorting work

**Expected Result:**
- Student explorer shows all students with comprehensive filtering, search, pagination, and sorting

**Status:** NOT TESTED


---

## TC-ENQ-020: Create enquiry with referral type and fee guidelines

**Preconditions:**
- User is logged in with ROLE_ADMIN
- A program exists with fee structure configured for current academic year
- Referral type "STAFF" exists with guidelineValue = 5000

**Steps:**
1. Send a POST request to `/api/v1/enquiries` with body:
   ```json
   {
     "name": "Priya Kumar",
     "phone": "9876543210",
     "enquiryDate": "2024-06-15",
     "source": "WALK_IN",
     "programId": 1,
     "referralTypeId": 5,
     "feeGuidelineTotal": 100000,
     "referralAdditionalAmount": 5000,
     "finalCalculatedFee": 105000,
     "yearWiseFees": "[{\"year\":1,\"amount\":52500},{\"year\":2,\"amount\":52500}]"
   }
   ```
2. Verify the response status is 201 Created
3. Verify response contains referralTypeId, feeGuidelineTotal, referralAdditionalAmount, finalCalculatedFee

**Expected Result:**
- Enquiry is created with ENQUIRED status and all fee guideline data saved

**Status:** NOT TESTED

---

## TC-ENQ-021: Finalize fees for an enquiry

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists in INTERESTED status

**Steps:**
1. Send a POST request to `/api/v1/enquiries/{id}/finalize-fees` with body:
   ```json
   {
     "totalFee": 95000,
     "discountAmount": 5000,
     "discountReason": "Early bird discount"
   }
   ```
2. Verify the response status is 200 OK
3. Verify response contains finalizedTotalFee=95000, finalizedDiscountAmount=5000, finalizedNetFee=90000, status=FEES_FINALIZED

**Expected Result:**
- Enquiry status transitions to FEES_FINALIZED
- Finalized values are saved including who finalized and when

**Status:** NOT TESTED

---

## TC-ENQ-022: Reject fee finalization for wrong status

**Preconditions:**
- An enquiry exists in CONVERTED status

**Steps:**
1. Send a POST request to `/api/v1/enquiries/{id}/finalize-fees`

**Expected Result:**
- Returns 400/500 error indicating enquiry must be in ENQUIRED or INTERESTED status

**Status:** NOT TESTED

---

## TC-ENQ-023: Add document to an enquiry

**Preconditions:**
- An enquiry exists in FEES_PAID status

**Steps:**
1. Send a POST request to `/api/v1/enquiries/{id}/documents` with body:
   ```json
   {
     "documentType": "TENTH_MARKSHEET",
     "remarks": "10th mark sheet submitted"
   }
   ```
2. Verify response status is 201 Created
3. Verify enquiry status transitions to DOCUMENTS_SUBMITTED

**Expected Result:**
- Document is added and enquiry status automatically updates

**Status:** NOT TESTED

---

## TC-ENQ-024: List documents for an enquiry

**Preconditions:**
- An enquiry exists with at least one document

**Steps:**
1. Send a GET request to `/api/v1/enquiries/{id}/documents`

**Expected Result:**
- List of documents with their types and verification statuses is returned

**Status:** NOT TESTED

---

## TC-ENQ-025: Fee guideline panel on enquiry form (Frontend)

**Preconditions:**
- Programs exist with fee structures configured
- Referral types exist with some having non-zero guideline values

**Steps:**
1. Navigate to the enquiry form page
2. Select a program from the dropdown
3. Verify fee guideline panel appears showing year-wise fee breakdown
4. Select a referral type with a non-zero guideline value
5. Verify "Referral Additional Amount" box appears
6. Verify "Final Calculated Fee" shows sum of guideline total + referral amount

**Expected Result:**
- Fee guideline panel shows dynamic year-wise amounts based on program duration
- Referral additional amount only shows when guideline value > 0
- Final fee is correctly calculated

**Status:** NOT TESTED

---

## TC-ENQ-026: Enquiry status workflow transitions

**Preconditions:**
- An enquiry exists in ENQUIRED status

**Steps:**
1. Update status to INTERESTED → verify allowed
2. From INTERESTED, update to FEES_FINALIZED via finalize-fees → verify allowed
3. From FEES_FINALIZED, update to FEES_PAID → verify allowed
4. From FEES_PAID, add document → verify status becomes DOCUMENTS_SUBMITTED
5. From DOCUMENTS_SUBMITTED, convert to student → verify status becomes CONVERTED

**Expected Result:**
- Each status transition follows the workflow: ENQUIRED → INTERESTED → FEES_FINALIZED → FEES_PAID → DOCUMENTS_SUBMITTED → CONVERTED

**Status:** NOT TESTED

---

## TC-ENQ-027: Referral type selection in enquiry form (Frontend)

**Preconditions:**
- Active referral types exist in the system

**Steps:**
1. Navigate to the enquiry form
2. Verify referral type dropdown shows active referral types
3. Select a referral type
4. If the referral type has guidelineValue > 0, verify additional amount box appears
5. If guidelineValue = 0, verify no additional amount box shown

**Expected Result:**
- Referral types are loaded from the master table
- Additional amount box is conditionally shown based on guideline value

**Status:** NOT TESTED
