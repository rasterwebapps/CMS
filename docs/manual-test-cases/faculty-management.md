# Manual Test Cases — Faculty Management (R1-M2.5)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- At least one department exists in the system

---

### TC-FAC-001: Navigate to Faculty List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Faculty" in the sidebar navigation        |
| **Expected**| Faculty list page loads with columns: Employee Code, Name, Email, Department, Actions |

---

### TC-FAC-002: Search Faculty

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Type a search term in the search field           |
| **Expected**| Table filters to show only matching faculty members |

---

### TC-FAC-003: Sort Faculty

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click any sortable column header                 |
| **Expected**| Table sorts by the clicked column; click again to reverse |

---

### TC-FAC-004: Add Faculty — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Faculty" button                       |
| **Expected**| Faculty form loads with title "Add Faculty"; Department dropdown populated |

---

### TC-FAC-005: Add Faculty — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for Employee Code, First Name, Last Name, Email, and Department fields |

---

### TC-FAC-006: Add Faculty — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Employee Code, First Name, Last Name, Email, select Department; click Create |
| **Expected**| Snackbar shows "Created"; redirected to faculty list; new entry visible |

---

### TC-FAC-007: Faculty Detail View

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click on a faculty name or detail icon in the faculty list |
| **Expected**| Faculty detail page loads showing personal information, department, and assigned courses/labs |

---

### TC-FAC-008: Edit Faculty

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon on a faculty row             |
| **Expected**| Form loads with title "Edit Faculty"; all fields pre-populated |

---

### TC-FAC-009: Update Faculty

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Modify fields and click "Update"                 |
| **Expected**| Snackbar shows "Updated"; redirected to faculty list; changes reflected |

---

### TC-FAC-010: Delete Faculty

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the delete icon on a faculty row           |
| **Expected**| Confirmation dialog: "Delete Faculty" with faculty name; confirming removes the item |

---

### TC-FAC-011: Cancel Delete Faculty

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Cancel" in the delete confirmation dialog |
| **Expected**| Dialog closes; faculty member remains in the list |

---

### TC-FAC-012: Empty State

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View faculty list when no faculty members exist  |
| **Expected**| Table shows "No data available" message          |
