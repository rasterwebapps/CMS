# Column Visibility Toggle — Manual Test Cases

Feature: Column visibility toggle on all list pages

---

## TC-COL-001: Toggle a column off

**Preconditions:**
- User is logged in with any role that can view a list page (e.g., navigate to `/departments`)

**Steps:**
1. Open any list page (e.g., `/departments`)
2. Click the **Columns** button in the top-right of the command bar
3. Uncheck one column (e.g., "Code") from the dropdown panel

**Expected Result:**
- The panel stays open
- The unchecked column is immediately hidden from the table
- The column header and all data cells for that column are gone

**Status:** NOT TESTED

---

## TC-COL-002: Toggle a column back on

**Preconditions:**
- TC-COL-001 has been completed (a column is hidden)

**Steps:**
1. With the Columns panel open, re-check the previously unchecked column

**Expected Result:**
- The column reappears in the table immediately

**Status:** NOT TESTED

---

## TC-COL-003: Preferences are persisted across page reloads

**Preconditions:**
- User is on a list page and has hidden at least one column

**Steps:**
1. Hide column "Code" on the Departments list
2. Refresh the browser (F5)
3. Navigate back to `/departments`

**Expected Result:**
- The "Code" column remains hidden after the page reload

**Status:** NOT TESTED

---

## TC-COL-004: Cannot hide the last visible column

**Preconditions:**
- User is on a list page

**Steps:**
1. Open the Columns panel
2. Uncheck all columns one by one until only one remains

**Expected Result:**
- When only one column is visible, clicking its checkbox has no effect — at least one column is always visible
- The last remaining checkbox cannot be unchecked

**Status:** NOT TESTED

---

## TC-COL-005: Count badge shows filtered count

**Preconditions:**
- User is on a list page that has a search bar (e.g., `/departments`)

**Steps:**
1. Note the count badge (e.g., "12 departments")
2. Type a search term that matches a subset of rows (e.g., "comp")
3. Observe the count badge

**Expected Result:**
- Count badge changes to show "N of M departments" (filtered count of total)

**Status:** NOT TESTED

---

## TC-COL-006: Count badge shows total when no filter active

**Preconditions:**
- User is on a list page with a search bar

**Steps:**
1. Clear the search bar (or navigate fresh)
2. Observe the count badge

**Expected Result:**
- Count badge shows just the total number (e.g., "12 departments")

**Status:** NOT TESTED

---

## TC-COL-007: Column preferences are per-list (not shared)

**Preconditions:**
- User is logged in

**Steps:**
1. On the Departments list, hide the "HOD" column
2. Navigate to the Students list
3. Observe that the Students list still shows all columns

**Expected Result:**
- Each list page stores its own column preferences independently

**Status:** NOT TESTED

---

## TC-COL-008: Columns panel closes on outside click

**Preconditions:**
- User has the Columns panel open

**Steps:**
1. Click anywhere outside the Columns panel (e.g., on the table)

**Expected Result:**
- The Columns panel closes

**Status:** NOT TESTED

---

## TC-COL-009: Exam result list — column toggle with exam selector

**Preconditions:**
- User has `ROLE_FACULTY` or `ROLE_ADMIN`
- Navigate to `/exam-results`

**Steps:**
1. Select an examination from the selector in the command bar
2. Results table appears
3. Click Columns button and hide the "Grade" column

**Expected Result:**
- The Grade column disappears from the results table
- The exam selector in the command bar remains fully functional

**Status:** NOT TESTED

---

## TC-COL-010: Document submission list — column toggle

**Preconditions:**
- User has `ROLE_ADMIN` or `ROLE_FACULTY`
- Navigate to the document submission page

**Steps:**
1. Confirm the Columns button is visible in the top command bar
2. Uncheck the "Net Fee" column

**Expected Result:**
- The Net Fee column is hidden from the document submission table

**Status:** NOT TESTED
