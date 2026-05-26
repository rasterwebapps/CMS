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

**Status:** NOT TESTED

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

**Status:** NOT TESTED

