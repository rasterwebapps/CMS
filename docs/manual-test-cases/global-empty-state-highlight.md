# Global Empty State Highlight — Manual Test Cases

## TC-GLOBAL-EMPTY-001: Empty table surfaces use full highlight color

**Preconditions:**
- User is logged in with a role that can access list/master screens.
- Application is running in light and dark mode for visual comparison.

**Steps:**
1. Navigate to at least three table-based screens with no visible rows, for example Enquiries, Departments, and Fee Structures.
2. Use search/date/status filters if needed to force the table into an empty state.
3. Move the pointer across the empty-state message area, the blank area below it, and the area above the paginator.
4. Repeat in dark mode.

**Expected Result:**
- The preferred highlighted surface color fills the full empty table/card area.
- No screen shows a half-highlight where only the top or message row is tinted.
- Header and paginator remain readable and visually separated.

**Status:** READY TO TEST

---

## TC-GLOBAL-EMPTY-002: Standalone shared empty states use the same highlight surface

**Preconditions:**
- User is logged in with a role that can access card/list screens.
- At least one card-view list can be filtered to zero results.

**Steps:**
1. Open a card-view list screen that uses the shared empty-state component, such as Departments or Courses.
2. Apply a search that returns no records.
3. Verify the empty-state panel color.
4. Click the empty-state action button if present.

**Expected Result:**
- Standalone shared empty states use the same highlighted surface color as empty tables.
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
3. Compare icon, title, subtitle, action button placement, and highlight surface.

**Expected Result:**
- Full-screen/table/card empty states use the shared `<cms-empty-state>` pattern.
- Search empty states use `No {module} found` plus a short search-specific subtitle.
- True zero-data states use `No {module} yet` plus guidance on when records will appear or how to create one.
- Existing actions such as Add User, Add Role, Add Assignment, View Courses, and Clear Search still work.

**Status:** READY TO TEST

