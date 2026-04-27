# Academic Calendar — Manual Test Cases

Covers all improvements added in the Academic Calendar overhaul: year selector, semester
progress bar, role-based button visibility, stats strip, semester overlap validation,
calendar event / holiday management (full-stack), visual month-grid view, print/export,
and upcoming event alerts.

---

## TC-CAL-001: Academic Year Selector — Switch Years

**Preconditions:**
- At least two academic years exist.
- User is logged in (any role).

**Steps:**
1. Navigate to `/academic-calendar`.
2. Observe that the current academic year is pre-selected in the year dropdown.
3. Change the dropdown to a different academic year.

**Expected Result:**
- The semester timeline and events reload for the selected year without a full page refresh.
- Stats strip updates to reflect the new year.

**Status:** NOT TESTED

---

## TC-CAL-002: Academic Year Selector — Default to Current Year

**Preconditions:**
- One academic year is marked as `isCurrent = true`.
- User is logged in.

**Steps:**
1. Navigate to `/academic-calendar`.

**Expected Result:**
- The year dropdown shows the current year pre-selected.
- Timeline shows that year's semesters.

**Status:** NOT TESTED

---

## TC-CAL-003: Semester Progress Bar — In-Progress Semester

**Preconditions:**
- A semester exists whose `startDate` ≤ today ≤ `endDate`.

**Steps:**
1. Navigate to `/academic-calendar`.
2. Locate the "In Progress" semester card in the timeline.

**Expected Result:**
- A horizontal progress bar is visible inside the card.
- The bar label shows e.g. "38 / 90 days (42%)".
- The bar is filled in the primary color (not gray).

**Status:** NOT TESTED

---

## TC-CAL-004: Semester Progress Bar — Completed Semester

**Preconditions:**
- A semester whose `endDate` is in the past exists.

**Steps:**
1. Navigate to `/academic-calendar`.

**Expected Result:**
- The progress bar for the completed semester shows 100% filled (gray/muted color).
- Label shows "X / X days (100%)".

**Status:** NOT TESTED

---

## TC-CAL-005: Semester Progress Bar — Upcoming Semester

**Preconditions:**
- A semester whose `startDate` is in the future exists.

**Steps:**
1. Navigate to `/academic-calendar`.

**Expected Result:**
- The progress bar shows 0% / empty.
- Label shows "Starts <date>".

**Status:** NOT TESTED

---

## TC-CAL-006: Role-Based Button Visibility — Admin/College Admin

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`.

**Steps:**
1. Navigate to `/academic-calendar`.

**Expected Result:**
- Buttons "Years", "Semesters", and "Add Event" are visible in the page header.
- Export and Print buttons are also visible.

**Status:** NOT TESTED

---

## TC-CAL-007: Role-Based Button Visibility — Faculty/Student/Parent

**Preconditions:**
- User is logged in with `ROLE_FACULTY`, `ROLE_STUDENT`, or `ROLE_PARENT`.

**Steps:**
1. Navigate to `/academic-calendar`.

**Expected Result:**
- "Years", "Semesters", and "Add Event" buttons are NOT visible.
- Read-only timeline/grid and events are visible.
- Export and Print buttons remain visible.

**Status:** NOT TESTED

---

## TC-CAL-008: Stats Strip — Values

**Preconditions:**
- An academic year with at least one semester and at least one event exists.

**Steps:**
1. Navigate to `/academic-calendar`.

**Expected Result:**
- Four stat cards are visible: Total Weeks, Semesters, Days Left in Semester, Events / Holidays.
- Values are computed correctly from date math.
- "Days Left" shows `—` if no semester is currently in progress.

**Status:** NOT TESTED

---

## TC-CAL-009: Add Calendar Event — Success (Admin)

**Preconditions:**
- User has `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`.
- At least one academic year and one semester exist.

**Steps:**
1. Navigate to `/academic-calendar`.
2. Click **Add Event**.
3. Fill in Title: "Diwali Holiday", Start Date: (a date), End Date: (same date), Type: Holiday.
4. Optionally select a semester.
5. Click **Create**.

**Expected Result:**
- Dialog closes.
- Toast "Event created" appears.
- Event appears in the events list under the selected semester (or "Year-Level Events" if no semester selected).
- Event badge color matches Holiday type (amber).

**Status:** NOT TESTED

---

## TC-CAL-010: Add Calendar Event — Validation

**Preconditions:**
- User has `ROLE_ADMIN`.

**Steps:**
1. Click **Add Event**.
2. Leave Title blank and click **Create**.

**Expected Result:**
- Form shows validation error on Title field.
- Dialog remains open; no API call is made.

**Status:** NOT TESTED

---

## TC-CAL-011: Edit Calendar Event

**Preconditions:**
- At least one calendar event exists.
- User has `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`.

**Steps:**
1. Navigate to `/academic-calendar`.
2. Click the pencil (edit) icon next to an event.
3. Change the title and click **Update**.

**Expected Result:**
- Toast "Event updated" appears.
- Event list refreshes with the updated title.

**Status:** NOT TESTED

---

## TC-CAL-012: Delete Calendar Event

**Preconditions:**
- At least one calendar event exists.
- User has `ROLE_ADMIN`.

**Steps:**
1. Navigate to `/academic-calendar`.
2. Click the delete (trash) icon next to an event.
3. Confirm the browser confirm dialog.

**Expected Result:**
- Toast "Event deleted" appears.
- Event is removed from the list.

**Status:** NOT TESTED

---

## TC-CAL-013: Calendar Event — Backend Validation (End Date Before Start Date)

**Preconditions:**
- Backend running.

**Steps:**
1. Send `POST /api/v1/calendar-events` with `startDate: "2024-10-10"` and `endDate: "2024-10-05"`.

**Expected Result:**
- Response status `400 Bad Request`.
- Error message: "End date must not be before start date".

**Status:** NOT TESTED

---

## TC-CAL-014: Calendar Events — Filter by Event Type (API)

**Steps:**
1. Send `GET /api/v1/calendar-events/academic-year/{id}?eventType=EXAM`.

**Expected Result:**
- Only events with `eventType = EXAM` are returned for that academic year.

**Status:** NOT TESTED

---

## TC-CAL-015: Calendar Events — Filter by Semester (API)

**Steps:**
1. Send `GET /api/v1/calendar-events/semester/{semesterId}`.

**Expected Result:**
- Only events linked to the given semester are returned.

**Status:** NOT TESTED

---

## TC-CAL-016: Visual Month Grid View — Toggle

**Preconditions:**
- Academic year with semesters exists.

**Steps:**
1. Navigate to `/academic-calendar`.
2. Click the **Month Grid** toggle button.

**Expected Result:**
- The view switches from the semester timeline to a month-by-month calendar grid.
- Months covered by the academic year are displayed.
- Days belonging to ONGOING semesters are highlighted in a primary color background.
- Days belonging to UPCOMING semesters use amber background.
- Days belonging to COMPLETED semesters use a muted/teal background.
- Days not in any semester have no background.

**Status:** NOT TESTED

---

## TC-CAL-017: Visual Month Grid View — Event Dots

**Preconditions:**
- At least one calendar event exists in the academic year.

**Steps:**
1. Switch to **Month Grid** view.

**Expected Result:**
- Colored dots appear on dates that have events.
- Dot color corresponds to the event type (amber for holiday, red for exam, violet for cultural, etc.).
- Hovering over a dot shows the event title in a tooltip (via day cell title attribute).

**Status:** NOT TESTED

---

## TC-CAL-018: Timeline Toggle (Switch Back)

**Steps:**
1. Switch to **Month Grid** view.
2. Click **Timeline** toggle.

**Expected Result:**
- View reverts to the semester timeline.

**Status:** NOT TESTED

---

## TC-CAL-019: Upcoming Events Alert Panel

**Preconditions:**
- At least one calendar event exists with `startDate` within the next 30 days.

**Steps:**
1. Navigate to `/academic-calendar`.

**Expected Result:**
- An "Upcoming in the next 30 days" banner appears below the stats strip.
- Up to 5 upcoming events are listed with title, date, and type badge.
- No banner appears if there are no upcoming events in the next 30 days.

**Status:** NOT TESTED

---

## TC-CAL-020: Export Events to CSV

**Preconditions:**
- At least one calendar event exists.

**Steps:**
1. Navigate to `/academic-calendar`.
2. Click the **Download** (export) icon button.

**Expected Result:**
- A CSV file is downloaded with columns: Title, Type, Start Date, End Date, Description, Semester.
- Filename contains the academic year name.

**Status:** NOT TESTED

---

## TC-CAL-021: Print Calendar

**Steps:**
1. Navigate to `/academic-calendar`.
2. Click the **Print** icon button.

**Expected Result:**
- Browser print dialog opens.
- The print preview shows the academic calendar content.

**Status:** NOT TESTED

---

## TC-CAL-022: Semester Overlap Validation (Backend)

**Preconditions:**
- An academic year "2024-2025" exists (start: 2024-08-01, end: 2025-05-31).
- Semester "Fall 2024" exists (start: 2024-08-01, end: 2024-12-15) in that year.

**Steps:**
1. Send `POST /api/v1/semesters` with:
   ```json
   {
     "name": "Overlap Semester",
     "academicYearId": 1,
     "startDate": "2024-10-01",
     "endDate": "2025-01-31",
     "semesterNumber": 2
   }
   ```

**Expected Result:**
- Response status `400 Bad Request`.
- Error message: "The semester dates overlap with an existing semester in this academic year".

**Status:** NOT TESTED

---

## TC-CAL-023: Semester Status — Backend Field Exposed

**Steps:**
1. Send `GET /api/v1/semesters/{id}` for a semester whose `endDate` is in the past.

**Expected Result:**
- Response JSON contains `"status": "COMPLETED"`.

**Steps:**
1. Send `GET /api/v1/semesters/{id}` for a semester whose `startDate` is in the future.

**Expected Result:**
- Response JSON contains `"status": "UPCOMING"`.

**Status:** NOT TESTED

---

## TC-CAL-024: No Academic Year Found

**Preconditions:**
- No academic years exist in the system.

**Steps:**
1. Navigate to `/academic-calendar`.

**Expected Result:**
- An error card is displayed: "No Academic Year Found".
- "Create Academic Year" link is visible for ADMIN/COLLEGE_ADMIN.

**Status:** NOT TESTED
