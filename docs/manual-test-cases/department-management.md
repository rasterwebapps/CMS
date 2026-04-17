# Manual Test Cases — Department Management (R1-M2.1)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- At least one department exists in the system

---

### TC-DEPT-001: Navigate to Department List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Departments" in the sidebar navigation    |
| **Expected**| Department list page loads with columns: Name, Code, Description, Actions |

---

### TC-DEPT-002: Search Departments

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Type a search term in the search field           |
| **Expected**| Table filters to show only matching departments  |

---

### TC-DEPT-003: Sort Departments

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click any sortable column header                 |
| **Expected**| Table sorts by the clicked column; click again to reverse |

---

### TC-DEPT-004: Add Department — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Department" button                    |
| **Expected**| Department form loads with title "Add Department" |

---

### TC-DEPT-005: Add Department — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for Name and Code fields |

---

### TC-DEPT-006: Add Department — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Name, Code, and optionally Description; click Create |
| **Expected**| Snackbar shows "Created"; redirected to department list; new entry visible |

---

### TC-DEPT-007: Edit Department

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon on a department row          |
| **Expected**| Form loads with title "Edit Department"; all fields pre-populated |

---

### TC-DEPT-008: Update Department

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Modify fields and click "Update"                 |
| **Expected**| Snackbar shows "Updated"; redirected to department list; changes reflected |

---

### TC-DEPT-009: Delete Department — Confirmation Dialog

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the delete icon on a department row        |
| **Expected**| Confirmation dialog: "Delete Department" with department name displayed |

---

### TC-DEPT-010: Delete Department — Successful Delete

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Confirm" in the delete confirmation dialog |
| **Expected**| Snackbar shows "Deleted"; department removed from the list |

---

### TC-DEPT-011: Cancel Delete Department

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Cancel" in the delete confirmation dialog |
| **Expected**| Dialog closes; department remains in the list    |

---

### TC-DEPT-012: Empty State

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View department list when no departments exist   |
| **Expected**| Table shows "No data available" message          |

---

## TC-DEPT-013: Duplicate department shows meaningful error

**Preconditions:**
- User is logged in with ROLE_ADMIN
- A department with code "CS" already exists

**Steps:**
1. Navigate to **Departments → Add Department**
2. Enter the same code ("CS") as an existing department
3. Click **Save**

**Expected Result:**
- The form shows a snackbar with the message "A record with the same name or code already exists." (not a generic "Failed" message)
- The form stays open so the user can correct the code

**Status:** NOT TESTED
