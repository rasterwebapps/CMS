# Manual Test Cases — Maintenance Management (R1-M4.3)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- At least one equipment item exists in the system

---

### TC-MAINT-001: Navigate to Maintenance List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Maintenance" in the sidebar navigation    |
| **Expected**| Maintenance list page loads with columns: Equipment, Requested By, Priority, Status, Assigned Technician, Created At, Actions |

---

### TC-MAINT-002: Search Maintenance Requests

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Type a search term in the search field           |
| **Expected**| Table filters to show only matching maintenance requests |

---

### TC-MAINT-003: Sort Maintenance Requests

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click any sortable column header                 |
| **Expected**| Table sorts by the clicked column                |

---

### TC-MAINT-004: Add Maintenance Request — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Request" button                       |
| **Expected**| Maintenance form loads with title "Add Maintenance Request"; Equipment dropdown populated |

---

### TC-MAINT-005: Add Maintenance Request — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for Equipment, Requested By, Description, and Priority fields |

---

### TC-MAINT-006: Add Maintenance Request — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select Equipment, enter Requested By, enter Description, select Priority (e.g., HIGH), optionally fill Status, Assigned Technician, Repair Cost, Completed Date; click Create |
| **Expected**| Snackbar shows "Created"; redirected to maintenance list; new request visible |

---

### TC-MAINT-007: Edit Maintenance Request

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon on a maintenance request row |
| **Expected**| Form loads with title "Edit Maintenance Request"; all fields pre-populated |

---

### TC-MAINT-008: Update Maintenance Request — Status Transition

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Change status from REQUESTED to ASSIGNED, fill in Assigned Technician, click "Update" |
| **Expected**| Snackbar shows "Updated"; redirected to list; status and technician updated |

---

### TC-MAINT-009: Delete Maintenance Request

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the delete icon on a maintenance request row |
| **Expected**| Confirmation dialog appears; confirming deletes the request |

---

### TC-MAINT-010: Priority Options

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Open the Priority dropdown on the maintenance form |
| **Expected**| Options shown: LOW, MEDIUM, HIGH, CRITICAL       |

---

### TC-MAINT-011: Status Options

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Open the Status dropdown on the maintenance form |
| **Expected**| Options shown: REQUESTED, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED |

---

### TC-MAINT-012: Maintenance Form — Description Field

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Enter a description in the textarea              |
| **Expected**| Description field accepts multi-line text up to 1000 characters |

---

### TC-MAINT-013: Maintenance Form — Repair Cost

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Enter a negative value in the Repair Cost field  |
| **Expected**| Validation error: value must be 0 or greater     |

---

### TC-MAINT-014: Maintenance Form — Back Navigation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the back arrow on the maintenance form     |
| **Expected**| Navigated back to the maintenance list           |

---

### TC-MAINT-015: Empty State

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View maintenance list when no requests exist     |
| **Expected**| Table shows "No data available" message          |
