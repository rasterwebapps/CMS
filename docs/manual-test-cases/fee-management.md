# Manual Test Cases — Fee Management (R1-M4.1)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- At least one program and one student exist in the system

---

## Fee Structures

### TC-FEE-001: Navigate to Fee Structures List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Fee Structures" in the sidebar navigation |
| **Expected**| Fee Structures list page loads with a table showing columns: Name, Program, Semester, Amount (₹), Due Date, Actions |

---

### TC-FEE-002: Search Fee Structures

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Type a search term in the search field           |
| **Expected**| Table filters to show only matching fee structures; clear button appears |

---

### TC-FEE-003: Clear Search Filter

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the clear (✕) button in the search field   |
| **Expected**| Search field is cleared; all fee structures are shown again |

---

### TC-FEE-004: Sort Fee Structures

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click a column header (e.g., "Amount")           |
| **Expected**| Table rows sort by the clicked column; clicking again reverses sort order |

---

### TC-FEE-005: Paginate Fee Structures

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | If more than 10 records exist, use the paginator controls |
| **Expected**| Table shows the correct page of results; page size can be changed to 5, 10, or 25 |

---

### TC-FEE-006: Add Fee Structure — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Fee Structure" button                 |
| **Expected**| Fee Structure form loads with title "Add Fee Structure" and empty fields |

---

### TC-FEE-007: Add Fee Structure — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for Name, Program, Semester, and Amount fields |

---

### TC-FEE-008: Add Fee Structure — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Name, select Program, enter Semester (1–8), enter Amount, optionally fill Lab Fee Component and Due Date; click Create |
| **Expected**| Snackbar shows "Created"; redirected to fee structures list; new entry visible |

---

### TC-FEE-009: Edit Fee Structure

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit (pencil) icon on a fee structure row |
| **Expected**| Form loads with title "Edit Fee Structure"; all fields pre-populated with existing values |

---

### TC-FEE-010: Update Fee Structure

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Modify a field and click "Update"                |
| **Expected**| Snackbar shows "Updated"; redirected to fee structures list; changes reflected |

---

### TC-FEE-011: Delete Fee Structure

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the delete (trash) icon on a fee structure row |
| **Expected**| Confirmation dialog appears: "Delete Fee Structure" with the item name |

---

### TC-FEE-012: Confirm Delete Fee Structure

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Delete" in the confirmation dialog        |
| **Expected**| Snackbar shows "Deleted successfully"; item removed from the list |

---

### TC-FEE-013: Cancel Delete Fee Structure

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Cancel" in the confirmation dialog        |
| **Expected**| Dialog closes; item remains in the list          |

---

### TC-FEE-014: Fee Structure Form — Back Navigation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the back arrow button on the form page     |
| **Expected**| Navigated back to the fee structures list        |

---

## Fee Payments

### TC-PAY-001: Navigate to Fee Payments List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Fee Payments" in the sidebar navigation   |
| **Expected**| Fee Payments list page loads with columns: Student Name, Fee Structure, Amount Paid (₹), Payment Date, Payment Method, Status, Actions |

---

### TC-PAY-002: Search Fee Payments

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Type a student name in the search field          |
| **Expected**| Table filters to show only matching payment records |

---

### TC-PAY-003: Record Payment — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Record Payment" button                    |
| **Expected**| Payment form loads with title "Record Payment"; student and fee structure dropdowns populated |

---

### TC-PAY-004: Record Payment — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for Student, Fee Structure, Amount, Payment Date, and Payment Method |

---

### TC-PAY-005: Record Payment — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select Student, select Fee Structure, enter Amount, select Payment Date, select Payment Method (e.g., CASH), optionally enter Transaction ID; click Create |
| **Expected**| Snackbar shows "Payment recorded"; redirected to fee payments list; new entry visible |

---

### TC-PAY-006: Delete Fee Payment

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the delete icon on a payment row           |
| **Expected**| Confirmation dialog appears; confirming deletes the record |

---

### TC-PAY-007: Fee Payment Form — Cancel

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Cancel" on the payment form               |
| **Expected**| Navigated back to the fee payments list without creating a record |

---

### TC-PAY-008: Empty State

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View fee payments list when no payments exist    |
| **Expected**| Table shows "No data available" message          |

---

## Year-wise Fee Boxes (BR-2)

> **Business Requirement:** See [BR-2](../BUSINESS_REQUIREMENTS.md#br-2-year-wise-fee-boxes-per-program-duration)

### TC-FEE-015: Year-wise fee boxes appear based on program duration

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Precondition** | A 4-year program (e.g., B.Tech) and a 2-year program (e.g., MBA) exist |
| **Action**  | Navigate to Add Fee Structure → select the 4-year program |
| **Expected**| Four year-wise amount input boxes appear labeled "First Year", "Second Year", "Third Year", "Fourth Year" |

---

### TC-FEE-016: Year-wise fee boxes for 2-year program

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Precondition** | A 2-year program (e.g., MBA) exists |
| **Action**  | Navigate to Add Fee Structure → select the 2-year program |
| **Expected**| Two year-wise amount input boxes appear labeled "First Year", "Second Year" |

---

### TC-FEE-017: Year-wise fee amounts saved to database

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Precondition** | Fee structure form open with year-wise boxes visible for a 3-year program |
| **Action**  | Enter ₹1,20,000 for First Year, ₹1,00,000 for Second Year, ₹80,000 for Third Year → click Create |
| **Expected**| Snackbar shows "Created"; all three year-wise amounts are saved as separate records in the database |

---

### TC-FEE-018: Year-wise fee amounts shown on edit

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Precondition** | A fee structure with year-wise amounts exists |
| **Action**  | Click edit on the fee structure row |
| **Expected**| Year-wise amount boxes are pre-populated with the saved values |

---

### TC-FEE-019: Fee structure filtered by academic year

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Precondition** | Fee structures exist for multiple academic years |
| **Action**  | Use the Academic Year filter dropdown on the fee structures list |
| **Expected**| Only fee structures for the selected academic year are shown |

---

### TC-FEE-020: Fee structure scoped to program and academic year

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Precondition** | A program has fee structures for two different academic years |
| **Action**  | Send GET `/api/v1/fee-structures?programId={id}&academicYearId={yearId}` |
| **Expected**| Only fee structures matching both program and academic year are returned |

---

## TC-FEE-020: Create fee structure with year-wise amounts

**Preconditions:**
- User is logged in with ROLE_ADMIN
- A program with durationYears = 3 exists
- An academic year exists

**Steps:**
1. Send a POST request to `/api/v1/fee-structures` with body:
   ```json
   {
     "programId": 1,
     "academicYearId": 1,
     "feeType": "TUITION",
     "amount": 150000,
     "description": "Tuition fee",
     "isMandatory": true,
     "isActive": true,
     "yearAmounts": [
       { "yearNumber": 1, "yearLabel": "First Year", "amount": 55000 },
       { "yearNumber": 2, "yearLabel": "Second Year", "amount": 50000 },
       { "yearNumber": 3, "yearLabel": "Third Year", "amount": 45000 }
     ]
   }
   ```
2. Verify the response status is 201 Created
3. Verify response includes yearAmounts array with 3 entries

**Expected Result:**
- Fee structure is created with year-wise breakdown matching program duration

**Status:** NOT TESTED

---

## TC-FEE-021: Update fee structure year-wise amounts

**Preconditions:**
- A fee structure with year amounts exists

**Steps:**
1. Send a PUT request with updated yearAmounts values
2. Verify old year amounts are replaced with new ones

**Expected Result:**
- Year amounts are replaced (not appended) on update

**Status:** NOT TESTED

---

## TC-FEE-022: Get fee guideline for program and academic year

**Preconditions:**
- Fee structures exist for a program in a specific academic year

**Steps:**
1. Send a GET request to `/api/v1/fee-structures?programId=1&academicYearId=1`
2. Verify response includes fee structures with yearAmounts

**Expected Result:**
- Active fee structures for the program/academic year are returned with year-wise breakdowns
- This data serves as the fee guideline shown on the enquiry screen

**Status:** NOT TESTED

---

## TC-FEE-018: Create fee structure with courseId

**Preconditions:**
- User is logged in with ROLE_ADMIN
- At least one program and course exist

**Steps:**
1. Send a POST request to `/api/v1/fee-structures` with body:
   ```json
   {
     "programId": 1,
     "academicYearId": 1,
     "feeType": "TUITION",
     "amount": 50000.00,
     "courseId": 1
   }
   ```
2. Verify the response status is 201 Created
3. Verify the response contains `courseId` and `courseName`

**Expected Result:**
- Fee structure is created and associated with the specified course
- Response includes course details

**Status:** NOT TESTED

---

## TC-FEE-019: Filter fee structures by programId and courseId

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Fee structures exist for different courses under a program

**Steps:**
1. Send a GET request to `/api/v1/fee-structures?programId=1&courseId=2`
2. Verify the response status is 200 OK
3. Verify all returned fee structures match the specified program and course

**Expected Result:**
- Only fee structures matching both programId and courseId are returned

**Status:** NOT TESTED
