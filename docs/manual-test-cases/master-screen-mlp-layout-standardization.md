# Master Screen MLP Layout Standardization - Manual Test Cases

## TC-MLP-001: Department list uses standard MLP containers

**Preconditions:**
- User is logged in
- Application is running

**Steps:**
1. Navigate to `/departments` (or the route where department list template is rendered).
2. Verify page uses standard MLP header, toolbar, and table/card container spacing.
3. Switch between card and table views.
4. Confirm no layout shift in header/toolbar between view modes.

**Expected Result:**
- Screen follows MLP layout structure (`mlp-page`, `mlp-hdr`, `mlp-toolbar`, `mlp-table-card`).
- View toggle works and layout remains consistent.

**Status:** NOT TESTED

## TC-MLP-002: Designation and Speciality screens render with MLP layout

**Preconditions:**
- User is logged in with access to designation and speciality masters
- Application is running

**Steps:**
1. Navigate to `/designations`.
2. Verify header and toolbar match MLP structure and table view container style.
3. Navigate to `/specialities`.
4. Verify header and toolbar match MLP structure and table view container style.
5. Switch card/table modes on both screens.

**Expected Result:**
- Both screens use MLP structure and preserve behavior in card/table modes.
- Status toggle actions remain available and functional.

**Status:** NOT TESTED

## TC-MLP-003: Shared view toggle component is used on converted masters

**Preconditions:**
- User is logged in with access to all listed masters
- Application is running

**Steps:**
1. Open `/agents`, `/courses`, `/programs`, `/referral-types`, and `/staff-referrers`.
2. On each screen, switch from card to table and back using the view toggle.
3. Refresh each page.
4. Verify the last selected mode persists after refresh.

**Expected Result:**
- All listed screens use the shared view toggle behavior.
- View mode persistence works consistently after refresh.

**Status:** NOT TESTED

