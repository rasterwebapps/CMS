# Enquiry Payment, Status History & Conversion Manual Test Cases

## TC-EPAY-001: Collect full payment for a finalized enquiry

**Preconditions:**
- User is logged in with ROLE_ADMIN
- An enquiry exists in FEES_FINALIZED status with finalizedNetFee set

**Steps:**
1. POST `/api/v1/enquiries/{id}/payments` with body: `{"amountPaid": <full net fee>, "paymentDate": "2024-07-01", "paymentMode": "CASH"}`
2. Verify response status is 200 OK
3. Verify response contains `receiptNumber` matching `RCP-YYYYMMDD-XXXXXXXX`
4. Verify `newStatus` is `FEES_PAID`
5. GET `/api/v1/enquiries/{id}` and verify status is `FEES_PAID`

**Expected Result:**
- Payment is recorded, receipt number generated, enquiry status becomes FEES_PAID

**Status:** NOT TESTED

---

## TC-EPAY-002: Collect partial payment

**Preconditions:**
- Enquiry in FEES_FINALIZED status with finalizedNetFee > amount being paid

**Steps:**
1. POST `/api/v1/enquiries/{id}/payments` with amountPaid less than finalizedNetFee
2. Verify response `newStatus` is `PARTIALLY_PAID`
3. Verify enquiry status is now `PARTIALLY_PAID`

**Expected Result:**
- Status becomes PARTIALLY_PAID when payment is less than net fee

**Status:** NOT TESTED

---

## TC-EPAY-003: Reject payment for ineligible enquiry status

**Preconditions:**
- Enquiry in ENQUIRED status

**Steps:**
1. POST `/api/v1/enquiries/{id}/payments` with any payment details
2. Verify response status is 400 Bad Request

**Expected Result:**
- Payment rejected with error mentioning FEES_FINALIZED or PARTIALLY_PAID requirement

**Status:** NOT TESTED

---

## TC-EPAY-004: Get payment history for enquiry

**Preconditions:**
- At least one payment exists for an enquiry

**Steps:**
1. GET `/api/v1/enquiries/{id}/payments`
2. Verify response is a list of payments ordered by date descending

**Expected Result:**
- Returns all payments for the enquiry

**Status:** NOT TESTED

---

## TC-EHIST-001: View status history after enquiry creation

**Preconditions:**
- An enquiry was created

**Steps:**
1. GET `/api/v1/enquiries/{id}/status-history`
2. Verify at least one history entry exists
3. Verify first entry has `fromStatus: null`, `toStatus: "ENQUIRED"`, `changedBy: "system"`

**Expected Result:**
- Status history shows the initial creation transition

**Status:** NOT TESTED

---

## TC-EHIST-002: Status history records payment status changes

**Preconditions:**
- A payment was collected on an enquiry

**Steps:**
1. GET `/api/v1/enquiries/{id}/status-history`
2. Verify an entry with `toStatus: "FEES_PAID"` or `"PARTIALLY_PAID"` exists
3. Verify `changedBy` matches the collector username

**Expected Result:**
- History entry is present with payment collector as changedBy

**Status:** NOT TESTED

---

## TC-EDOC-001: Submit documents when all mandatory docs present

**Preconditions:**
- Enquiry in FEES_PAID status
- All 5 mandatory documents (TENTH_MARKSHEET, TWELFTH_MARKSHEET, TRANSFER_CERTIFICATE, AADHAR_CARD, PASSPORT_PHOTO) added with UPLOADED or VERIFIED status

**Steps:**
1. POST `/api/v1/enquiries/{id}/submit-documents`
2. Verify response status is 200 OK
3. Verify enquiry status is now DOCUMENTS_SUBMITTED

**Expected Result:**
- Enquiry transitions to DOCUMENTS_SUBMITTED

**Status:** NOT TESTED

---

## TC-EDOC-002: Submit documents fails when mandatory docs missing

**Preconditions:**
- Enquiry in FEES_PAID status
- Missing at least one mandatory document

**Steps:**
1. POST `/api/v1/enquiries/{id}/submit-documents`
2. Verify response status is 400 Bad Request
3. Verify response body contains `missingDocumentTypes` list

**Expected Result:**
- Returns 400 with list of missing document types

**Status:** NOT TESTED

---

## TC-ESUMM-001: Get enquiry summary

**Preconditions:**
- Enquiry exists with finalized fees and some payments made

**Steps:**
1. GET `/api/v1/enquiries/{id}/summary`
2. Verify response contains `enquiry`, `totalAmountPaid`, `outstandingAmount`, `documentCount`, `documentTypes`

**Expected Result:**
- Returns complete summary with payment and document information

**Status:** NOT TESTED

---

## TC-ECONV-001: Convert enquiry to student using new atomic endpoint

**Preconditions:**
- Enquiry in DOCUMENTS_SUBMITTED status with a program assigned
- Email address not already in use by another student

**Steps:**
1. GET `/api/v1/enquiries/{id}/conversion-prefill` to see pre-filled data
2. POST `/api/v1/enquiries/{id}/convert` with body: `{"firstName":"Ravi","lastName":"Kumar","email":"ravi@college.edu","semester":1,"admissionDate":"2024-07-01"}`
3. Verify response status is 201 Created
4. Verify enquiry status is CONVERTED
5. Verify `convertedStudentId` is set in response
6. GET `/api/v1/students/{studentId}` and verify `rollNumber` is null (deferred)

**Expected Result:**
- Student created without roll number, enquiry status set to CONVERTED

**Status:** NOT TESTED

---

## TC-ECONV-002: Conversion rejected when not in DOCUMENTS_SUBMITTED status

**Preconditions:**
- Enquiry in FEES_PAID status

**Steps:**
1. POST `/api/v1/enquiries/{id}/convert` with valid body
2. Verify response status is 400 Bad Request

**Expected Result:**
- Error message mentions DOCUMENTS_SUBMITTED requirement

**Status:** NOT TESTED

---

## TC-ROLL-001: Assign roll number to student

**Preconditions:**
- Student exists without a roll number (rollNumber is null)
- User is logged in with ROLE_ADMIN

**Steps:**
1. PATCH `/api/v1/students/{id}/roll-number` with body: `{"rollNumber":"CS2024001"}`
2. Verify response status is 200 OK
3. Verify response contains `rollNumber: "CS2024001"`

**Expected Result:**
- Roll number assigned successfully

**Status:** NOT TESTED

---

## TC-ROLL-002: Reject duplicate roll number assignment

**Preconditions:**
- Roll number "CS2024001" already assigned to another student

**Steps:**
1. PATCH `/api/v1/students/{id}/roll-number` with body: `{"rollNumber":"CS2024001"}`
2. Verify response status is 400 Bad Request

**Expected Result:**
- Error message mentions roll number already in use

**Status:** NOT TESTED

---

## TC-ROLL-003: Bulk assign roll numbers

**Preconditions:**
- Multiple students exist without roll numbers
- User logged in with ROLE_ADMIN

**Steps:**
1. POST `/api/v1/students/bulk-assign-roll-numbers` with body: `{"assignments":[{"studentId":1,"rollNumber":"CS2024001"},{"studentId":2,"rollNumber":"CS2024002"}]}`
2. Verify response status is 200 OK
3. Verify response is a list with both students having roll numbers

**Expected Result:**
- All students in the batch get roll numbers assigned

**Status:** NOT TESTED

---

## TC-ROLL-004: Find students without roll numbers

**Preconditions:**
- Some students exist without roll numbers

**Steps:**
1. GET `/api/v1/students/without-roll-number`
2. Verify response is a list of students with `rollNumber: null`
3. Test with `?courseId=1` filter and verify only course-filtered results

**Expected Result:**
- Returns only students where rollNumber is null, optionally filtered by course/program

**Status:** NOT TESTED
