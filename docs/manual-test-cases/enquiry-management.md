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
     "studentName": "Jane Doe",
     "phone": "9876543210",
     "email": "jane@example.com",
     "interestedProgramId": 1,
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
