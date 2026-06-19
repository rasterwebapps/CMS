# Speciality List - Manual Test Cases

## TC-SPL-001: Toggle speciality status from card view

**Preconditions:**
- User is logged in with a role that can manage specialities
- At least one speciality exists in active state
- Application is running

**Steps:**
1. Navigate to `/specialities`.
2. Ensure card view is selected.
3. Click the status toggle action on an active speciality card.
4. Confirm the dialog by clicking `Deactivate`.
5. Verify success toast appears.
6. Verify the same speciality now shows as inactive after reload.

**Expected Result:**
- Confirmation dialog appears with correct speciality name and code.
- Status update succeeds and UI refreshes with updated status.

**Actual Result:**
- 

**Status:** NOT TESTED

## TC-SPL-002: Toggle speciality status from table view

**Preconditions:**
- User is logged in with a role that can manage specialities
- At least one speciality exists
- Application is running

**Steps:**
1. Navigate to `/specialities`.
2. Switch to table view.
3. Verify `Status` column is visible between `Head of Speciality` and actions.
4. Click the `Toggle Status` action for any row.
5. Confirm the dialog.
6. Verify success toast and updated status chip (`Active` or `Inactive`).

**Expected Result:**
- Table includes the status column and row status chips.
- Toggle action updates backend and UI reflects latest status.

**Actual Result:**
- 

**Status:** NOT TESTED

## TC-SPL-003: Search and empty-state behavior after refactor

**Preconditions:**
- User is logged in
- Application is running

**Steps:**
1. Navigate to `/specialities`.
2. Enter a non-matching search term.
3. Verify empty-state message appears in card or table view.
4. Click `Clear Search` from empty state.
5. Verify list data returns.

**Expected Result:**
- Search filters both views correctly.
- Empty state appears for no results and clear action restores the list.

**Actual Result:**
- 

**Status:** NOT TESTED

