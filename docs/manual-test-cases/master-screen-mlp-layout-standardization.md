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

## TC-MLP-004: Enquiry pipeline lists use MLP structure

**Preconditions:**
- User is logged in with access to enquiry workflow lists
- Application is running

**Steps:**
1. Open `/enquiries`, `/enquiries/document-submission`, `/enquiries/document-verification`, and `/enquiries/admission-completion`.
2. Verify each screen shows MLP header and toolbar spacing.
3. Verify table area uses the standard table card layout and paginator placement.

**Expected Result:**
- All four enquiry workflow lists render with MLP structure and existing actions continue to work.

**Status:** NOT TESTED

## TC-MLP-005: Mid-priority list screens migrated to MLP

**Preconditions:**
- User is logged in with access to lab schedule, student, commission explorer, settings, scholarship applications, and curriculum versions
- Application is running

**Steps:**
1. Open `/lab-schedules`, `/students`, `/commission-explorer`, `/settings`, `/scholarship-applications`, and `/curriculum-versions`.
2. Verify each screen uses MLP page/header/toolbar/table-card classes.
3. For `/settings`, switch card/table and refresh to confirm view-mode persistence.

**Expected Result:**
- All listed screens follow MLP layout standards and maintain prior user workflows.

**Status:** NOT TESTED

## TC-MLP-006: Library list screens use MLP table-card container

**Preconditions:**
- User is logged in with access to library module
- Application is running

**Steps:**
1. Open `/library/books`, `/library/issues`, and `/library/periodicals`.
2. Verify each page has MLP page/header/toolbar structure.
3. Verify table region uses the standard MLP table-card container behavior.

**Expected Result:**
- All three library list screens align with MLP layout standards without functional regression.

**Status:** NOT TESTED

## TC-MLP-007: Shared view toggle migration for remaining card/table lists

**Preconditions:**
- User is logged in with access to equipment, faculty, fee structure, and lab modules
- Application is running

**Steps:**
1. Open `/equipment`, `/faculty`, `/fee-structures`, and `/labs`.
2. On each screen, toggle card/table view.
3. Refresh each page and verify selected view mode persists.

**Expected Result:**
- Inline segmented toggles are replaced by shared view toggle behavior.
- View mode persistence and list actions continue working.

**Status:** NOT TESTED

