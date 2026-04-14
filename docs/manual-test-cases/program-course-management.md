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
| **Expected**| Program list page loads with columns: Name, Code, Degree Type, Duration (Years), Department, Actions |

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
| **Expected**| Program form loads with title "Add Program"; Department dropdown populated |

---

### TC-PROG-004: Add Program — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for Name, Code, Degree Type, Duration Years, and Department fields |

---

### TC-PROG-005: Add Program — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Name, Code, select Degree Type, enter Duration Years, select Department; click Create |
| **Expected**| Snackbar shows "Created"; redirected to program list; new entry visible |

---

### TC-PROG-006: Edit Program

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon on a program row             |
| **Expected**| Form loads with title "Edit Program"; all fields pre-populated |

---

### TC-PROG-007: Delete Program

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the delete icon on a program row           |
| **Expected**| Confirmation dialog: "Delete Program" with program name; confirming removes the item |

---

## Courses

### TC-COURSE-001: Navigate to Course List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Courses" in the sidebar navigation        |
| **Expected**| Course list page loads with columns: Name, Code, Credits, Program, Semester, Actions |

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
| **Expected**| Validation errors shown for Name, Code, Credits, Program, and Semester fields |

---

### TC-COURSE-004: Add Course — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Name, Code, Credits, select Program, enter Semester; click Create |
| **Expected**| Snackbar shows "Created"; redirected to course list; new entry visible |

---

### TC-COURSE-005: Edit/Delete Course

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon to modify a course, or click the delete icon to remove it |
| **Expected**| Edit: form loads pre-populated; Delete: confirmation dialog shown, confirming removes the item |
