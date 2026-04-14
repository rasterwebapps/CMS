# Manual Test Cases — Lab Scheduling (R1-M3.2)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- At least one lab, course, faculty member, and semester exist in the system
- At least one lab slot exists for schedule test cases

---

## Lab Schedules

### TC-SCHED-001: Navigate to Lab Schedule List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Lab Schedules" in the sidebar navigation  |
| **Expected**| Lab schedule page loads displaying a weekly timetable view with day and time slot columns |

---

### TC-SCHED-002: Search Schedules

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Type a search term in the search/filter field    |
| **Expected**| Timetable filters to show only matching schedule entries |

---

### TC-SCHED-003: Filter Schedules by Lab

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select a lab from the lab filter dropdown        |
| **Expected**| Timetable updates to show only schedules for the selected lab |

---

### TC-SCHED-004: Add Schedule — Navigate to Form

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Schedule" button                      |
| **Expected**| Schedule form loads with dropdowns for Lab, Course, Faculty, Lab Slot, Semester, and fields for Batch Name and Day of Week |

---

### TC-SCHED-005: Add Schedule — Validation

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Submit the form without filling required fields  |
| **Expected**| Validation errors shown for Lab, Course, Faculty, Lab Slot, Batch Name, Day of Week, and Semester fields |

---

### TC-SCHED-006: Add Schedule — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Select Lab, Course, Faculty, Lab Slot, Semester; enter Batch Name; select Day of Week; click Create |
| **Expected**| Snackbar shows "Created"; redirected to schedule view; new entry visible in the timetable |

---

### TC-SCHED-007: Schedule Conflict Detection

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Attempt to create a schedule that overlaps with an existing schedule (same lab, same slot, same day) |
| **Expected**| Error message displayed indicating a scheduling conflict; schedule is not created |

---

### TC-SCHED-008: Edit Schedule

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon on a schedule entry          |
| **Expected**| Form loads with title "Edit Schedule"; all fields pre-populated |

---

### TC-SCHED-009: Delete Schedule

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the delete icon on a schedule entry        |
| **Expected**| Confirmation dialog: "Delete Schedule" with details; confirming removes the entry from the timetable |

---

## Lab Slots

### TC-SLOT-001: Navigate to Lab Slot List

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Lab Slots" in the sidebar navigation      |
| **Expected**| Lab slot list page loads with columns: Slot Name, Start Time, End Time, Actions |

---

### TC-SLOT-002: Add Lab Slot — Successful Create

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click "Add Lab Slot"; fill in Slot Name, Start Time, End Time; click Create |
| **Expected**| Snackbar shows "Created"; redirected to lab slot list; new entry visible |

---

### TC-SLOT-003: Edit/Delete Lab Slot

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon to modify a lab slot, or click the delete icon to remove it |
| **Expected**| Edit: form loads pre-populated; Delete: confirmation dialog shown, confirming removes the item |
