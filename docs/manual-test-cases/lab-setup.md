# Manual Test Cases — Lab Setup & Management (R1-M2.4)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- At least one department exists in the system
- At least one faculty member exists for staff assignment test cases

---

### TC-LAB-001: Navigate to Lab List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Labs" in the sidebar navigation           |
| **Expected**| Lab list page loads with columns: Name, Type, Department, Building, Room, Capacity, Status, Actions |

---

### TC-LAB-002: Search Labs

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Type a search term in the search field           |
| **Expected**| Table filters to show only matching labs         |

---

### TC-LAB-003: Filter Labs by Department

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select a department from the department filter dropdown |
| **Expected**| Table filters to show only labs belonging to the selected department |

---

### TC-LAB-004: Filter Labs by Type

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select a lab type from the type filter dropdown  |
| **Expected**| Table filters to show only labs matching the selected type |

---

### TC-LAB-005: Filter Labs by Status

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select a status from the status filter dropdown  |
| **Expected**| Table filters to show only labs matching the selected status |

---

### TC-LAB-006: Add Lab — Form and Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Lab" button; submit without filling required fields |
| **Expected**| Lab form loads with fields: Name, Lab Type, Department, Building, Room Number, Capacity, Status; validation errors shown for required fields |

---

### TC-LAB-007: Add Lab — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Name, select Lab Type, select Department, enter Building, Room Number, Capacity, and Status; click Create |
| **Expected**| Snackbar shows "Created"; redirected to lab list; new entry visible |

---

### TC-LAB-008: Lab Detail View

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click on a lab name or detail icon in the lab list |
| **Expected**| Lab detail page loads showing lab information and staff assignments section |

---

### TC-LAB-009: Assign In-Charge to Lab

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | On the lab detail page, click "Assign In-Charge"; select a faculty member from the dropdown; click Save |
| **Expected**| Faculty member is assigned as lab in-charge; assignment reflected on the lab detail page |

---

### TC-LAB-010: Edit Lab

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon on a lab row                 |
| **Expected**| Form loads with title "Edit Lab"; all fields pre-populated |

---

### TC-LAB-011: Delete Lab

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the delete icon on a lab row               |
| **Expected**| Confirmation dialog: "Delete Lab" with lab name; confirming removes the item |

---

### TC-LAB-012: Empty State

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View lab list when no labs exist                 |
| **Expected**| Table shows "No data available" message          |
