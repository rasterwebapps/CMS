# Global Empty State Consistency — Manual Test Cases

## TC-GLOBAL-EMPTY-001: Empty table surfaces use one neutral layout

**Preconditions:**
- User is logged in with a role that can access list/master screens.
- Application is running in light and dark mode for visual comparison.

**Steps:**
1. Navigate to at least three table-based screens with no visible rows, for example Enquiries, Admissions, and Fee Collection.
2. Use search/date/status filters if needed to force the table into an empty state.
3. Move the pointer across the empty-state message area, the blank area below it, and the area above the paginator.
4. Repeat in dark mode.

**Expected Result:**
- The same neutral empty-state surface fills the full empty table/card area.
- No screen uses a different pink/highlight tone or a half-highlight where only part of the empty area is tinted.
- Header and paginator remain readable and visually separated.

**Status:** READY TO TEST

---

## TC-GLOBAL-EMPTY-002: Standalone shared empty states use the same neutral surface

**Preconditions:**
- User is logged in with a role that can access card/list screens.
- At least one card-view list can be filtered to zero results.

**Steps:**
1. Open a card-view list screen that uses the shared empty-state component, such as Specialities or Courses.
2. Apply a search that returns no records.
3. Verify the empty-state panel color and spacing.
4. Click the empty-state action button if present.

**Expected Result:**
- Standalone shared empty states use the same neutral surface as empty tables.
- The empty-state content remains centered and readable.
- Existing action buttons still work.

**Status:** READY TO TEST

---

## TC-GLOBAL-EMPTY-003: Empty-state content follows shared pattern

**Preconditions:**
- User is logged in with a role that can access academic, lab, curriculum, examination, user, and role management screens.

**Steps:**
1. Force empty states on older list screens such as Attendance, Inventory, Examinations, Exam Results, Syllabus, Experiments, CO-PO Mapping, Maintenance, and Lab Schedules.
2. Open contextual empty panels such as Faculty Detail Courses/Lab Schedules, Lab Detail Staff Assignments, Academic Calendar with no terms, User Management with no matching users, and Role Management with no matching roles.
3. Compare icon, title, subtitle, action button placement, and neutral surface.

**Expected Result:**
- Full-screen/table/card empty states use the shared `<cms-empty-state>` pattern.
- Filtered empty states use consistent filter/search copy and a short corrective subtitle.
- True zero-data states use `No {module} yet` plus guidance on when records will appear or how to create one.
- Existing actions such as Add User, Add Role, Add Assignment, View Courses, and Clear Filters still work.

**Status:** READY TO TEST

---

## TC-GLOBAL-EMPTY-004: Admission workflow screens use matching empty-state copy and actions

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`.
- Application is running.

**Steps:**
1. Navigate to Enquiries, Fee Finalization, Fee Collection, Submit Documents, Verify Documents, Complete Admission, and Admission Explorer.
2. On each screen, apply filters/search values that produce zero visible rows.
3. Compare the empty-state title, subtitle tone, icon size, CTA placement, and background surface.
4. Click the empty-state action button where it is shown.
5. Repeat the visual check in dark mode.

**Expected Result:**
- Filtered empty states consistently show `No records match your filters`.
- Filtered empty states consistently use the `Clear Filters` action.
- Clear action resets all active filters/search values for that screen.
- True zero-data states use neutral queue wording such as `No candidates pending`, `No enquiries available`, or `No admissions available` with “will appear here automatically” guidance.
- All listed screens use the same neutral empty-state surface in light and dark mode.

**Status:** READY TO TEST

---

## TC-GLOBAL-EMPTY-005: Clear Filters button label and placement are consistent

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`.
- Application is running.

**Steps:**
1. Open Enquiries, Admission Explorer, Submit Documents, Verify Documents, Complete Admission, Fee Finalization, and Fee Collection.
2. Apply at least one filter/search value on each screen.
3. Observe the visible clear action in the toolbar.
4. For screens with a Columns button, compare the clear action placement against the filter controls and the Columns button.

**Expected Result:**
- Every visible toolbar clear action is labelled `Clear Filters`.
- No scoped screen shows only `Clear` or `Clear Search` as the visible toolbar/empty-state action.
- On screens with Columns, `Clear Filters` appears with the filter controls, not attached to the right-side Columns cluster.
- The Columns button remains right-aligned with row count where applicable.

**Status:** READY TO TEST

---

## TC-GLOBAL-EMPTY-006: Admission management tables keep pagination pinned at the bottom

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`.
- Application is running on a desktop viewport.

**Steps:**
1. Open Enquiries, Fee Finalization, Fee Collection, Submit Documents, Verify Documents, Complete Admission, and Admission Explorer.
2. Verify each list/table screen with zero rows, a few rows, and enough rows to require vertical scrolling.
3. Scroll inside the table area where rows overflow.
4. Compare the table card height and paginator location against the Enquiries screen.

**Expected Result:**
- Each table card fills the available vertical space below the screen header and toolbar.
- Rows scroll inside the table body/card instead of growing the page height.
- The paginator/footer remains visible at the bottom of the card/screen with zero rows, few rows, and many rows.
- The browser page itself does not gain an extra vertical scroll just because the table has many rows.

**Status:** READY TO TEST
