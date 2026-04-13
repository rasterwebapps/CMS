# Manual Test Cases — Examination Management (R1-M5.1)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- At least one course and one semester exist in the system
- At least one student exists for exam result testing

---

## Examinations

### TC-EXAM-001: Navigate to Examinations List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Examinations" in the sidebar navigation   |
| **Expected**| Examinations list page loads with columns: Name, Course, Type, Date, Duration (mins), Max Marks, Actions |

---

### TC-EXAM-002: Search Examinations

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Type a search term in the search field           |
| **Expected**| Table filters to show only matching examinations |

---

### TC-EXAM-003: Sort Examinations

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click any sortable column header                 |
| **Expected**| Table sorts by the clicked column                |

---

### TC-EXAM-004: Add Examination — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Examination" button                   |
| **Expected**| Examination form loads with title "Add Examination"; Course and Semester dropdowns populated |

---

### TC-EXAM-005: Add Examination — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for Name, Course, and Exam Type fields |

---

### TC-EXAM-006: Add Examination — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Fill in Name, select Course, select Exam Type (e.g., THEORY), optionally fill Date, Duration, Max Marks, Semester; click Create |
| **Expected**| Snackbar shows "Created"; redirected to examinations list; new examination visible |

---

### TC-EXAM-007: Edit Examination

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon on an examination row        |
| **Expected**| Form loads with title "Edit Examination"; all fields pre-populated |

---

### TC-EXAM-008: Update Examination

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Modify fields and click "Update"                 |
| **Expected**| Snackbar shows "Updated"; redirected to examinations list; changes reflected |

---

### TC-EXAM-009: Delete Examination

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the delete icon on an examination row      |
| **Expected**| Confirmation dialog appears; confirming deletes the examination |

---

### TC-EXAM-010: Exam Type Options

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Open the Exam Type dropdown on the form          |
| **Expected**| Options shown: THEORY, PRACTICAL, VIVA           |

---

### TC-EXAM-011: Examination Form — Duration Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Enter 0 or a negative value in Duration field    |
| **Expected**| Validation error: Duration must be at least 1    |

---

### TC-EXAM-012: Examination Form — Max Marks Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Enter a negative value in the Max Marks field    |
| **Expected**| Validation error: value must be 0 or greater     |

---

### TC-EXAM-013: Examination Form — Back Navigation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the back arrow on the examination form     |
| **Expected**| Navigated back to the examinations list          |

---

### TC-EXAM-014: Empty State

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View examinations list when no examinations exist |
| **Expected**| Table shows "No data available" message          |

---

## Exam Results

### TC-RESULT-001: Navigate to Exam Results

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Exam Results" in the sidebar navigation   |
| **Expected**| Exam Results page loads with an examination select dropdown and empty table |

---

### TC-RESULT-002: Select Examination to View Results

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select an examination from the dropdown          |
| **Expected**| Table populates with results for the selected examination; columns: Roll Number, Student Name, Marks Obtained, Grade, Status |

---

### TC-RESULT-003: Search Exam Results

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | With results loaded, type a student name in the search field |
| **Expected**| Table filters to show only matching results      |

---

### TC-RESULT-004: Sort Exam Results

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click any sortable column header                 |
| **Expected**| Table sorts by the clicked column                |

---

### TC-RESULT-005: No Results for Examination

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select an examination that has no results        |
| **Expected**| Table shows "No data available" message          |

---

### TC-RESULT-006: Paginate Results

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | With many results loaded, use paginator controls |
| **Expected**| Table shows correct page of results              |

---

### TC-RESULT-007: Change Examination Selection

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select a different examination from the dropdown |
| **Expected**| Table updates to show results for the newly selected examination |
