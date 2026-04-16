# Manual Test Cases — Program & Course Management (R1-M2.2)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- At least one department exists in the system
- At least one program exists for course test cases

---

## Programs

### TC-PROG-001: Navigate to Program List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Programs" in the sidebar navigation       |
| **Expected**| Program list page loads with columns: Code, Name, Program Level, Duration (Years), Departments, Actions |

---

### TC-PROG-002: Search Programs

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Type a search term in the search field           |
| **Expected**| Table filters to show only matching programs     |

---

### TC-PROG-003: Add Program — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Program" button                       |
| **Expected**| Program form loads with title "Add Program"; Departments multi-select populated |

---

### TC-PROG-004: Add Program — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for Name, Code, Program Level, Duration Years, and Departments fields |

---

### TC-PROG-005: Add Program — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Name, Code, select Program Level, enter Duration Years (1-10), select Departments; click Create |
| **Expected**| Snackbar shows "Created"; redirected to program list; new entry visible with correct program level and duration |

---

### TC-PROG-006: Edit Program

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon on a program row             |
| **Expected**| Form loads with title "Edit Program"; all fields pre-populated including programLevel, durationYears, and selected departments |

---

### TC-PROG-007: Delete Program

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the delete icon on a program row           |
| **Expected**| Confirmation dialog: "Delete Program" with program name; confirming removes the item |

---

### TC-PROG-008: Program Duration Years Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | POST `/api/v1/programs` with `durationYears: 0` or `durationYears: 11` |
| **Expected**| 400 Bad Request with validation error. Valid range is 1–10 |

---

### TC-PROG-009: Program Duration Years Drives Fee Structure

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Preconditions** | A program with `durationYears: 4` exists, and a fee structure is created for it |
| **Action**  | Verify that fee structure year amounts (FeeStructureYearAmount) match the program's `durationYears` |
| **Expected**| 4 year-wise fee amounts are generated (First Year through Fourth Year) |

---

## Courses

### TC-COURSE-001: Navigate to Course List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Courses" in the sidebar navigation        |
| **Expected**| Course list page loads with columns: Code, Name, Specialization, Program, Actions |

---

### TC-COURSE-002: Search Courses

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Type a search term in the search field           |
| **Expected**| Table filters to show only matching courses      |

---

### TC-COURSE-003: Add Course — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for Name, Code, and Program fields. Specialization is optional |

---

### TC-COURSE-004: Add Course — Successful Create (with Specialization)

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Name: "M.Sc. Nursing — OBG", Code: "MSCOBG-C", Specialization: "Obs Gyn", select Program; click Create |
| **Expected**| Snackbar shows "Created"; redirected to course list; new entry visible with specialization "Obs Gyn" |

---

### TC-COURSE-005: Add Course — Successful Create (without Specialization)

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Name: "B.Sc. Nursing", Code: "BSCN-C", leave Specialization empty, select Program; click Create |
| **Expected**| Snackbar shows "Created"; course created with `specialization: null` |

---

### TC-COURSE-006: Edit/Delete Course

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon to modify a course, or click the delete icon to remove it |
| **Expected**| Edit: form loads pre-populated (including specialization); Delete: confirmation dialog shown, confirming removes the item |

---

### TC-COURSE-007: API — DegreeType no longer exists

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | POST `/api/v1/courses` with a `degreeType` field in the request body |
| **Expected**| The field is ignored; course is created without error. `degreeType` and `durationYears` are no longer part of the Course entity |

---

### TC-COURSE-008: Course Response includes Program with durationYears

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | GET `/api/v1/courses/1` |
| **Expected**| Response includes nested `program` object with `durationYears` field (e.g., `program.durationYears: 4`) |
