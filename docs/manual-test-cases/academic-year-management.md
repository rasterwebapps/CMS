# Manual Test Cases — Academic Year & Calendar Management (R1-M2.3)

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in as admin
- At least one academic year exists for calendar test cases

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

### TC-AY-004A: Add Academic Year — Cohort Seat Allocations

**Preconditions:**
- User is logged in with permission `ACADEMIC_YEAR_MANAGE`.
- At least two active programs/courses exist, for example BCA and B.Com.

**Steps:**
1. Navigate to **Academic Years** and click **Add Academic Year**.
2. Enter academic-year information, term dates, and billing schedule.
3. In **Course / Program Seat Allocations**, enter management and counselling seats for each active program, for example BCA = 45 management + 15 counselling.
4. Click **Create Academic Year**.
5. Open the created academic-year detail screen.

**Expected Result:**
- The create form displays one seat-allocation row per active program/course.
- The academic year is created successfully.
- Cohort rows are created for the submitted programs.
- The detail screen shows the same management and counselling seat values and correct totals.

**Status:** NOT TESTED

---

### TC-AY-004B: Add Academic Year — Negative Seat Validation

**Preconditions:**
- User is logged in with permission `ACADEMIC_YEAR_MANAGE`.
- At least one active program/course exists.

**Steps:**
1. Navigate to **Academic Years** and click **Add Academic Year**.
2. Fill all required academic-year, term, and billing fields.
3. Enter `-1` in a management or counselling seat field.
4. Click **Create Academic Year**.

**Expected Result:**
- The form remains on screen and does not submit while a negative seat value is present.
- No academic year or cohort is created from the invalid request.

**Status:** NOT TESTED

---

### TC-AY-005: Edit/Delete Academic Year

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Click the edit icon to modify an academic year, or click the delete icon to remove it |
| **Expected**| Edit: form loads pre-populated; Delete: confirmation dialog shown, confirming removes the item |

---

## Academic Calendar

### TC-CAL-001: Academic Calendar View

| Field       | Value                                            |
|-------------|--------------------------------------------------|
| **Action**  | Navigate to the academic calendar page           |
| **Expected**| Calendar view displays terms and key dates for the selected academic year |
