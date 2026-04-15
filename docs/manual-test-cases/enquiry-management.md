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
     "status": "NEW",
     "remarks": "Interested in B.Sc Nursing"
   }
   ```
2. Verify the response status is 201 Created
3. Verify the returned object has the correct values

**Expected Result:**
- Enquiry is created with status NEW and returned with an ID

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
- An enquiry exists with status NEW

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
2. Verify only enquiries with status NEW are returned

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
5. Verify the new enquiry appears with status NEW

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
- An enquiry exists with status NEW

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
- Enquiries exist with status NEW

**Steps:**
1. Navigate to `/enquiries`
2. Click on the status chip for an enquiry with status NEW
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

