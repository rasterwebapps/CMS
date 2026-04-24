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

---

## TC-FAC-013: Phone and Email columns are visible in Faculty list

**Preconditions:**
- User is logged in with ROLE_ADMIN
- At least one faculty member with phone and email exists

**Steps:**
1. Navigate to **Faculty** from the sidebar
2. Observe the table columns

**Expected Result:**
- The Faculty table shows **Phone** and **Email** columns between Name and Department
- Faculty members with phone/email show the values; those without show `—`

**Status:** NOT TESTED

---

## TC-FAC-MLP-001: MLP card/table view toggle persists preference

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Navigate to Faculty list page

**Steps:**
1. Observe default view is card mode
2. Click the "Table" segment button
3. Verify list switches to table view
4. Refresh the page
5. Verify table view persists (localStorage key `faculty-view-mode`)

**Expected Result:**
- View mode persists across refreshes

**Status:** NOT TESTED

---

## TC-FAC-MLP-002: Faculty card shows employee code chip, name, designation, department badge, and status pill

**Preconditions:**
- At least one faculty member exists

**Steps:**
1. Navigate to Faculty list (card view)
2. Observe a card

**Expected Result:**
- Employee code shown as monospace code chip
- Full name bold and prominent
- Designation in muted text (title-cased)
- Department shown as blue badge
- Status shown as coloured pill (green=ACTIVE, amber=ON_LEAVE/SABBATICAL, gray=others)

**Status:** NOT TESTED

---

## TC-FAC-MLP-003: Clicking a faculty card navigates to detail view

**Preconditions:**
- Faculty list in card view with at least one member

**Steps:**
1. Click anywhere on a faculty card (not the action buttons)

**Expected Result:**
- Navigates to `/faculty/{id}` detail page

**Status:** NOT TESTED

---

## TC-FAC-MLP-004: Department and status filters work client-side without extra API calls

**Preconditions:**
- Multiple faculty members across different departments and statuses

**Steps:**
1. Navigate to Faculty list
2. Select a department from the department dropdown
3. Observe cards/rows filtered immediately
4. Select a status from the status dropdown
5. Observe further filtering

**Expected Result:**
- Filtering is instant (client-side) — no network request triggered on filter change
- "Clear Filters" button appears when any filter is active

**Status:** NOT TESTED

---

## TC-FAC-MLP-005: Clear Filters resets all active filters

**Preconditions:**
- Department filter, status filter, or search is active

**Steps:**
1. Apply a department filter and a status filter
2. Click "Clear Filters" button

**Expected Result:**
- All filters reset to "All Departments" / "All Statuses"
- Search input cleared
- Full faculty list shown again

**Status:** NOT TESTED
