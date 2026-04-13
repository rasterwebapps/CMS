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
