# Manual Test Cases — Attendance Management (R1-M3.3)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- At least one course, student, and lab schedule exist in the system
- Students are enrolled in a course for attendance marking

---

### TC-ATT-001: Navigate to Attendance List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Attendance" in the sidebar navigation     |
| **Expected**| Attendance list page loads with columns: Student, Course, Date, Status, Actions |

---

### TC-ATT-002: Filter Attendance by Student

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select a student from the student filter dropdown |
| **Expected**| Table filters to show only attendance records for the selected student |

---

### TC-ATT-003: Filter Attendance by Course

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select a course from the course filter dropdown  |
| **Expected**| Table filters to show only attendance records for the selected course |

---

### TC-ATT-004: Filter Attendance by Date

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select a date or date range from the date picker |
| **Expected**| Table filters to show only attendance records matching the selected date(s) |

---

### TC-ATT-005: Mark Attendance — Select Course and Date

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Mark Attendance"; select a Course and Date from the dropdowns |
| **Expected**| Student list loads for the selected course showing each enrolled student with an attendance status toggle (Present/Absent) |

---

### TC-ATT-006: Mark Attendance — Individual Student

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Toggle the attendance status for an individual student to Present or Absent; click Save |
| **Expected**| Snackbar shows "Saved"; attendance record created for the student on the selected date |

---

### TC-ATT-007: Bulk Attendance Marking

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Mark All Present" or "Mark All Absent" to set attendance for all students at once; click Save |
| **Expected**| All students' attendance status updated accordingly; snackbar shows "Saved" |

---

### TC-ATT-008: Edit Attendance Record

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon on an attendance record; change the status; click Update |
| **Expected**| Snackbar shows "Updated"; attendance record reflects the new status |

---

### TC-ATT-009: Attendance Report View

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to the attendance report page; select a course and date range |
| **Expected**| Report displays attendance summary with percentage per student and overall course attendance statistics |

---

### TC-ATT-010: Delete Attendance Record

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the delete icon on an attendance record    |
| **Expected**| Confirmation dialog shown; confirming removes the attendance record from the list |

---

### TC-ATT-011: Empty State

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | View attendance list when no records exist       |
| **Expected**| Table shows "No data available" message          |

---

### TC-ATT-012: Mark Attendance — No Students Enrolled

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select a course with no enrolled students on the mark attendance page |
| **Expected**| Message displayed: "No students enrolled in this course" |
