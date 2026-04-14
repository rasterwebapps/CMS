# Manual Test Cases — Academic Year, Semester & Calendar Management (R1-M2.3)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- At least one academic year exists for semester test cases

---

## Academic Years

### TC-AY-001: Navigate to Academic Year List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Academic Years" in the sidebar navigation |
| **Expected**| Academic year list page loads with columns: Year Label, Start Date, End Date, Status, Actions |

---

### TC-AY-002: Add Academic Year — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Academic Year" button                 |
| **Expected**| Academic year form loads with title "Add Academic Year" |

---

### TC-AY-003: Add Academic Year — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for Year Label, Start Date, and End Date fields |

---

### TC-AY-004: Add Academic Year — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Year Label (e.g., "2025-2026"), Start Date, End Date; click Create |
| **Expected**| Snackbar shows "Created"; redirected to academic year list; new entry visible |

---

### TC-AY-005: Edit/Delete Academic Year

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon to modify an academic year, or click the delete icon to remove it |
| **Expected**| Edit: form loads pre-populated; Delete: confirmation dialog shown, confirming removes the item |

---

## Semesters

### TC-SEM-001: Navigate to Semester List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Semesters" in the sidebar navigation      |
| **Expected**| Semester list page loads with columns: Name, Academic Year, Start Date, End Date, Status, Actions |

---

### TC-SEM-002: Add Semester — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Semester" button                      |
| **Expected**| Semester form loads with title "Add Semester"; Academic Year dropdown populated |

---

### TC-SEM-003: Add Semester — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for Name, Academic Year, Start Date, and End Date fields |

---

### TC-SEM-004: Add Semester — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Name, select Academic Year, enter Start Date and End Date; click Create |
| **Expected**| Snackbar shows "Created"; redirected to semester list; new entry visible |

---

### TC-SEM-005: Edit/Delete Semester

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon to modify a semester, or click the delete icon to remove it |
| **Expected**| Edit: form loads pre-populated; Delete: confirmation dialog shown, confirming removes the item |

---

## Academic Calendar

### TC-CAL-001: Academic Calendar View

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to the academic calendar page           |
| **Expected**| Calendar view displays semesters and key dates for the selected academic year |
