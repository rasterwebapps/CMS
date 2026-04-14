# Manual Test Cases — Curriculum Management (R1-M2.6)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- At least one course exists in the system
- At least one syllabus exists for experiment test cases
- At least one experiment and course outcome exist for CO/PO mapping test cases

---

## Syllabus

### TC-SYL-001: Navigate to Syllabus List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Syllabus" in the sidebar navigation       |
| **Expected**| Syllabus list page loads with columns: Title, Course, Version, Status, Actions |

---

### TC-SYL-002: Add Syllabus — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Syllabus" button                      |
| **Expected**| Syllabus form loads with title "Add Syllabus"; Course dropdown populated |

---

### TC-SYL-003: Add Syllabus — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for required fields including Title and Course |

---

### TC-SYL-004: Add Syllabus — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Title, select Course, enter Version and content details; click Create |
| **Expected**| Snackbar shows "Created"; redirected to syllabus list; new entry visible |

---

### TC-SYL-005: Edit/Delete Syllabus

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon to modify a syllabus, or click the delete icon to remove it |
| **Expected**| Edit: form loads pre-populated; Delete: confirmation dialog shown, confirming removes the item |

---

## Experiments

### TC-EXP-001: Navigate to Experiment List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Experiments" in the sidebar navigation    |
| **Expected**| Experiment list page loads with columns: Title, Syllabus, Order, Actions |

---

### TC-EXP-002: Add Experiment — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Experiment" button                    |
| **Expected**| Experiment form loads with title "Add Experiment"; Syllabus dropdown populated |

---

### TC-EXP-003: Add Experiment — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for required fields including Title and Syllabus |

---

### TC-EXP-004: Add Experiment — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Title, select Syllabus, enter Order number, optionally add Aim, Procedure, and Resources; click Create |
| **Expected**| Snackbar shows "Created"; redirected to experiment list; new entry visible |

---

### TC-EXP-005: Edit/Delete Experiment

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon to modify an experiment, or click the delete icon to remove it |
| **Expected**| Edit: form loads pre-populated; Delete: confirmation dialog shown, confirming removes the item |

---

## CO/PO Mapping

### TC-MAP-001: Navigate to CO/PO Mapping List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "CO/PO Mapping" in the sidebar navigation  |
| **Expected**| Mapping list page loads with columns: Course Outcome, Program Outcome, Outcome Type, Mapping Level, Actions |

---

### TC-MAP-002: Add Mapping — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Mapping" button                       |
| **Expected**| Mapping form loads with Outcome Type and Mapping Level dropdowns |

---

### TC-MAP-003: Add Mapping — Outcome Type Options

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Open the Outcome Type dropdown on the mapping form |
| **Expected**| Options shown: COURSE_OUTCOME, PROGRAM_OUTCOME, PROGRAM_SPECIFIC_OUTCOME |

---

### TC-MAP-004: Add Mapping — Mapping Level Options

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Open the Mapping Level dropdown on the mapping form |
| **Expected**| Options shown: LOW, MEDIUM, HIGH                 |

---

### TC-MAP-005: Add Mapping — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select Course Outcome, Program Outcome, Outcome Type (e.g., COURSE_OUTCOME), Mapping Level (e.g., HIGH); click Create |
| **Expected**| Snackbar shows "Created"; redirected to mapping list; new entry visible |

---

### TC-MAP-006: Edit/Delete Mapping

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon to modify a mapping, or click the delete icon to remove it |
| **Expected**| Edit: form loads pre-populated; Delete: confirmation dialog shown, confirming removes the item |
