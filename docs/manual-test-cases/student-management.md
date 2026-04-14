# Manual Test Cases — Student & Admission Management (R1-M3.1)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- At least one department and program exist in the system
- At least one student exists for admission test cases

---

## Students

### TC-STU-001: Navigate to Student List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Students" in the sidebar navigation       |
| **Expected**| Student list page loads with columns: Roll Number, Name, Email, Program, Status, Actions |

---

### TC-STU-002: Search Students

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Type a search term in the search field           |
| **Expected**| Table filters to show only matching students     |

---

### TC-STU-003: Sort Students

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click any sortable column header                 |
| **Expected**| Table sorts by the clicked column; click again to reverse |

---

### TC-STU-004: Add Student — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Student" button; submit the form without filling required fields |
| **Expected**| Validation errors shown for required fields including First Name, Last Name, Email, and Program |

---

### TC-STU-005: Add Student — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in personal details (First Name, Last Name, Email, Date of Birth, Gender), demographics, and address information; select Program; click Create |
| **Expected**| Snackbar shows "Created"; redirected to student list; new entry visible |

---

### TC-STU-006: Student Detail View

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click on a student name or detail icon in the student list |
| **Expected**| Student detail page loads showing personal details, demographics, address, program enrollment, and admission information |

---

### TC-STU-007: Edit Student

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon on a student row             |
| **Expected**| Form loads with title "Edit Student"; all fields pre-populated |

---

### TC-STU-008: Delete Student

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the delete icon on a student row           |
| **Expected**| Confirmation dialog: "Delete Student" with student name; confirming removes the item |

---

## Admissions

### TC-ADM-001: Create Admission

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to admissions; click "Add Admission"; fill in student, program, admission date, and academic qualifications; click Create |
| **Expected**| Snackbar shows "Created"; new admission record visible with status APPLIED |

---

### TC-ADM-002: Admission Status Workflow — Approve

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select an admission with status APPLIED; click "Approve" |
| **Expected**| Admission status changes from APPLIED to APPROVED |

---

### TC-ADM-003: Admission Status Workflow — Enroll

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select an admission with status APPROVED; click "Enroll" |
| **Expected**| Admission status changes from APPROVED to ENROLLED; student is fully enrolled in the program |

---

### TC-ADM-004: Admission Academic Qualifications

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | On the admission form, add academic qualifications (degree, institution, year, percentage) |
| **Expected**| Academic qualifications are saved and displayed on the admission detail view |
