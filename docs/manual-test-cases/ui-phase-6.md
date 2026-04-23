# UI Phase 6 — Polish & Micro-interactions

Manual test cases verifying the work delivered for Phase 6 of
`docs/Claude_UI_Redesign/UI_REDESIGN_PLAN.md`: route transitions,
toast notifications, animated form feedback, and print/CSV export.

---

## TC-UI-601: Route fade-slide transition

**Preconditions:**
- User is logged in (any role)
- Application is running

**Steps:**
1. Navigate to `/dashboard`.
2. Click the "Departments" link in the sidenav.
3. Observe the transition between routes.
4. Click "Programs" then "Courses" in quick succession.

**Expected Result:**
- Outgoing view fades out and slides up subtly (~120ms).
- Incoming view fades in and slides down into place (~200ms).
- No visible flicker, layout jump, or `ExpressionChangedAfterItHasBeenChecked` warnings in the console.

**Status:** NOT TESTED

---

## TC-UI-602: Toast stacking and auto-dismiss

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- A test department exists

**Steps:**
1. Navigate to `/departments`.
2. Click "Add Department".
3. Submit the form three times in rapid succession with valid data (or trigger via a script).
4. Observe the top-right area of the viewport.
5. Wait 3 seconds without interacting.
6. Trigger an error toast (e.g. delete a department that's referenced elsewhere).

**Expected Result:**
- Each success toast appears as a card in the top-right with a green left-border, check-circle icon, message text, and a thin progress bar that drains over ~3s.
- Multiple toasts stack vertically with consistent gap; no overlap.
- Each toast auto-dismisses when its progress bar reaches 0.
- Clicking the `×` button on a toast removes it immediately.
- Error toasts use the danger red accent and remain visible for ~6s.

**Status:** NOT TESTED

---

## TC-UI-603: Animated inline form error reveal

**Preconditions:**
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to `/departments/new`.
2. Click into the "Name" field, then click outside without typing.
3. Click into the "Code" field, then click outside without typing.

**Expected Result:**
- The "Name is required" error message slides down (translateY + opacity) over ~120ms rather than appearing instantly.
- Same animated reveal occurs for the "Code" field.
- Inputs receive the existing red border + soft ring on the same blur.

**Status:** NOT TESTED

---

## TC-UI-604: Loading button state and success checkmark

**Preconditions:**
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to `/departments/new`.
2. Fill in a unique name and code, leave description and HOD blank.
3. Click "Create Department".

**Expected Result:**
- Immediately after click: button label disappears, a small white spinner spins in its place, and the button is non-interactive (`aria-busy="true"`, `disabled`).
- On success: button briefly turns green with a white checkmark animating in (~200ms), then the page navigates to `/departments` after ~600ms.
- On failure (simulate by stopping the backend): button returns to its normal state and an error toast appears.

**Status:** NOT TESTED

---

## TC-UI-605: CSV export from a data table

**Preconditions:**
- A list page that consumes `<cms-data-table [exportable]="true" [exportFilename]="'…'">` is available
- The list contains at least one row

**Steps:**
1. Open the list page.
2. Click the "Export CSV" button in the table toolbar.
3. Open the downloaded file in Excel, Numbers, or Google Sheets.
4. Reload the page so the list is empty (e.g. apply a filter that yields no rows) — or test on an empty list — and click "Export CSV".

**Expected Result:**
- File downloads with the configured filename and `.csv` extension.
- Content opens cleanly with proper UTF-8 characters (BOM prefixed).
- Header row matches the column headers visible on screen; data rows match cell display values.
- Special characters (`,`, `"`, newlines) inside cells are properly RFC-4180 quoted.
- When the list is empty, the "Export CSV" button is disabled (does not trigger a download).

**Status:** NOT TESTED

---

## TC-UI-606: Print stylesheet hides app chrome

**Preconditions:**
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to any detail page (e.g. `/students/1`).
2. Press `Ctrl+P` (Windows/Linux) or `Cmd+P` (macOS).
3. Inspect the print preview.

**Expected Result:**
- Toolbar (`app-toolbar`) and sidenav are hidden.
- Toast host (`app-toast-host`) is hidden.
- Action buttons (`.btn-submit`, `.btn-cancel`, `.btn-ghost`, `.btn-icon-action`, row actions) are hidden.
- Background gradients/glows are removed; text is rendered black on white.
- Tables print full-width with visible row borders.
- Cards and stat blocks avoid awkward page breaks (`break-inside: avoid`).

**Status:** NOT TESTED

---

## TC-UI-607: PrintService.printElement renders only a subtree (smoke)

**Preconditions:**
- A component has been wired with `PrintService.printElement(elementRef)` (none ship with Phase 6 — this verifies the API contract).

**Steps:**
1. In a temporary component, inject `PrintService` and call `printElement(myRef)` from a button click handler.
2. Click the button.

**Expected Result:**
- A hidden iframe is appended to `<body>`, is populated with the element's HTML and the host page's stylesheets, and `window.print()` is invoked from inside the iframe.
- The browser's print preview shows only the targeted subtree, not the full app shell.
- The iframe is removed from the DOM ~1s later.

**Status:** NOT TESTED
