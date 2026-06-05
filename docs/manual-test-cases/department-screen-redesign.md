# Speciality Screen Redesign — Manual Test Cases

Covers the visual and functional changes introduced for:
- Speciality List page (card/table toggle, hero header, stat chips, styled table)
- Speciality Form page (two-column layout, live preview, code uppercase, char count, HOD avatar)
- Admin Dashboard hero (academic-year tag pill, italic username)

---

## TC-DEPT-UI-001: Card view renders on first visit

**Preconditions:**
- User is logged in with ROLE_ADMIN
- `localStorage` key `dept-view-mode` is absent or set to `"card"`
- Application is running

**Steps:**
1. Navigate to `/specialities`
2. Observe the view that appears

**Expected Result:**
- Page loads with the card grid layout (speciality cards visible)
- Hero header shows "Academic *Specialities*" with the word "Specialities" italicised in the accent colour
- Subtitle "Manage specialities and their heads across the college" is displayed

**Status:** NOT TESTED

---

## TC-DEPT-UI-002: Stat chips show correct counts

**Preconditions:**
- At least 3 specialities exist; at least 1 has a non-empty HOD name

**Steps:**
1. Navigate to `/specialities` (card view)
2. Observe the stat chip row below the subtitle

**Expected Result:**
- First chip shows the total number of specialities (e.g., "3 Specialities")
- Second chip shows the number of specialities that have a head assigned (e.g., "2 Heads Assigned")
- Both chips have a small coloured dot (green for Specialities, accent for Heads Assigned)

**Status:** NOT TESTED

---

## TC-DEPT-UI-003: View toggle persists to localStorage

**Preconditions:**
- User is on `/specialities`

**Steps:**
1. Click the "Table" button in the segmented control
2. Verify the table view renders
3. Navigate away to `/dashboard`
4. Navigate back to `/specialities`
5. Observe which view is shown

**Expected Result:**
- After clicking "Table", rows in a `mat-table` are shown
- After navigating away and back, the table view is restored (not the card view)

**Status:** NOT TESTED

---

## TC-DEPT-UI-004: Speciality card hover reveals action buttons

**Preconditions:**
- At least one speciality exists
- Card view is active

**Steps:**
1. Navigate to `/specialities`
2. Hover the mouse over a speciality card
3. Observe the top-right area of the card

**Expected Result:**
- Two small icon buttons (edit pencil, delete trash) appear on hover
- The card lifts slightly (translateY) and shows the accent-coloured top border
- Moving the mouse away hides the action buttons again

**Status:** NOT TESTED

---

## TC-DEPT-UI-005: Delete from card view shows confirmation and removes card

**Preconditions:**
- At least one speciality exists (e.g., "Test Speciality")
- Card view is active

**Steps:**
1. Hover over the "Test Speciality" card
2. Click the trash-bin icon button
3. Confirm in the dialog by clicking "Delete"
4. Observe the card grid

**Expected Result:**
- A confirmation dialog appears with title "Delete Speciality" and the speciality name
- After confirming, the card is removed from the grid
- A success snackbar "Speciality deleted successfully" appears

**Status:** NOT TESTED

---

## TC-DEPT-UI-006: Search filters both card view and table view

**Preconditions:**
- Multiple specialities exist with different names/codes
- Card view is active

**Steps:**
1. Type "CS" in the search field
2. Observe the card grid — only matching cards should show
3. Switch to table view
4. Observe the table — only matching rows should show

**Expected Result:**
- Card view shows only specialities whose name, code, or HOD matches "CS"
- Table view similarly filters rows
- Clearing the search field (click ✕) restores all items

**Status:** NOT TESTED

---

## TC-DEPT-UI-007: Empty state appears when no results match search

**Preconditions:**
- Card view is active

**Steps:**
1. Type "XYZNOTFOUND999" in the search field

**Expected Result:**
- The card grid is hidden
- An empty-state panel appears with icon, "No specialities found" title, and "No specialities match your search. Try a different term." subtitle
- A "Clear Search" button is visible; clicking it clears the search and restores the grid

**Status:** NOT TESTED

---

## TC-DEPT-UI-008: Table view shows code-chip and HOD avatar

**Preconditions:**
- At least one speciality with a HOD name exists
- Table view is active

**Steps:**
1. Navigate to `/specialities` and switch to table view
2. Observe the "Code" column
3. Observe the "Head of Speciality" column

**Expected Result:**
- The code is rendered as a rounded, accent-tinted badge (`.code-chip`)
- The HOD column shows a circular avatar with the person's initials, their name in bold, and the sublabel "Head of Speciality"
- Specialities without a HOD show "—" in that column

**Status:** NOT TESTED

---

## TC-DEPT-UI-009: Speciality Form — live preview and code auto-uppercase

**Preconditions:**
- User navigates to `/specialities/new`

**Steps:**
1. Type "computer science" in the "Speciality Name" field
2. Type "csc" (lowercase) in the "Speciality Code" field
3. Type "Dr. Anita Rao" in the "Head of Speciality" field
4. Type a description in the "Description" field
5. Observe the Live Preview card on the right panel

**Expected Result:**
- Code field auto-converts "csc" to "CSC" as typed
- Character counter below the Code field shows "3/20"
- Live Preview card shows:
  - Code badge: "CSC"
  - Name: "computer science"
  - HOD row: initials avatar "AR" + "Dr. Anita Rao"
  - Description text
- The HOD avatar inside the input shows initials "AR"

**Status:** NOT TESTED

---

## TC-DEPT-UI-010: Admin Dashboard hero — academic year tag and italic greeting

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Navigate to `/dashboard`
2. Observe the hero banner

**Expected Result:**
- An "AY YYYY–YY" tag pill appears to the left of the live badge in the eyebrow row (e.g., "AY 2025–26")
- The greeting reads "Welcome back, *<username>*! 👋" where the username is italicised
- A "View Reports" outline button appears next to "Generate Report →"

**Status:** NOT TESTED
