# India Locations — Manual Test Cases

---

## TC-LOC-001: State dropdown loads on forms

**Preconditions:**
- Application is running
- V149 / V150 / V151 migrations have run
- Seed data is in `india_states` and `india_districts`

**Steps:**
1. Navigate to **Admissions → Add Enquiry**
2. Scroll to the address section
3. Observe the **State** dropdown

**Expected Result:**
- State dropdown is populated with all 28 states + 8 UTs in alphabetical order
- Free-text input is gone; only the dropdown is visible

**Status:** NOT TESTED

---

## TC-LOC-002: District dropdown cascades from state selection

**Preconditions:**
- Same as TC-LOC-001

**Steps:**
1. Navigate to **Add Enquiry** → address section
2. The District dropdown shows "— Select State first —" and is disabled
3. Select **Tamil Nadu** from the State dropdown
4. Observe the District dropdown

**Expected Result:**
- District dropdown becomes enabled
- All Tamil Nadu districts are listed (Ariyalur, Chennai, Coimbatore, etc.)

**Status:** NOT TESTED

---

## TC-LOC-003: Changing state clears previous district selection

**Steps:**
1. Open any new enquiry/student/admission form
2. Select **Tamil Nadu** → select **Chennai** as district
3. Now change state to **Karnataka**

**Expected Result:**
- District field is cleared to empty
- District dropdown now shows Karnataka districts (Bengaluru Urban, Mysuru, etc.)

**Status:** NOT TESTED

---

## TC-LOC-004: State and district values are saved to the database

**Steps:**
1. Create a new Enquiry with State = Tamil Nadu, District = Chennai
2. Open the saved enquiry
3. Check the state and district fields

**Expected Result:**
- Enquiry detail shows State: Tamil Nadu, District: Chennai
- Values in DB column `state` = "Tamil Nadu", `district` = "Chennai"

**Status:** NOT TESTED

---

## TC-LOC-005: Form validates State as required (enquiry form)

**Preconditions:**
- In the enquiry form, State is marked required (`*`)

**Steps:**
1. Open Add Enquiry
2. Fill all fields except State / District
3. Click Submit

**Expected Result:**
- Validation error shown under State: "State is required"
- Form is not submitted

**Status:** NOT TESTED

---

## TC-LOC-006: Admin can view India Locations list

**Preconditions:**
- Logged in as Admin (ROLE_ADMIN or ROLE_COLLEGE_ADMIN)

**Steps:**
1. Navigate to **Settings → India Locations** (sidebar)
2. Observe the list screen

**Expected Result:**
- All 36 states / UTs are shown in MLP table/card view
- Total count shows 36
- Code chips (TN, MH, DL, etc.) are visible

**Status:** NOT TESTED

---

## TC-LOC-007: Admin can add a new state

**Preconditions:**
- Logged in with `INDIA_LOCATION_MANAGE` permission

**Steps:**
1. Navigate to **India Locations → Add State / UT**
2. Enter Name: "Test State", Code: "TS2"
3. Click **Create State**

**Expected Result:**
- Success toast shown
- New state appears in the list
- State is available in all form dropdowns

**Status:** NOT TESTED

---

## TC-LOC-008: Admin can add a district to an existing state

**Steps:**
1. Navigate to **India Locations**
2. Click the **+** (Add District) button on "Tamil Nadu" row
3. Enter Name: "Test District"
4. Click **Create District**

**Expected Result:**
- District saved and appears when Tamil Nadu is selected in form dropdowns

**Status:** NOT TESTED

---

## TC-LOC-009: Admin can edit a state

**Steps:**
1. Navigate to **India Locations**
2. Click Edit on any state
3. Change the name or active status
4. Save

**Expected Result:**
- Changes reflected in list and in form dropdowns

**Status:** NOT TESTED

---

## TC-LOC-010: Inactive state hidden from form dropdowns

**Steps:**
1. Edit any state, toggle **Active** to OFF
2. Open Add Enquiry form

**Expected Result:**
- Deactivated state does NOT appear in the State dropdown
- It still appears in India Locations list (with "Inactive" badge)

**Status:** NOT TESTED

---

## TC-LOC-011: Non-admin user cannot see Add/Edit/Delete controls

**Preconditions:**
- Logged in as ROLE_FRONT_OFFICE or ROLE_FACULTY

**Steps:**
1. Navigate to **India Locations** (if route is visible)

**Expected Result:**
- No Add State / Edit / Delete buttons are shown
- Dropdowns in forms still work (view-only permission)

**Status:** NOT TESTED

---

## TC-LOC-012: Selector works in Student form

**Steps:**
1. Navigate to **Students → Add Student**
2. Scroll to the address section
3. Select a state and then a district
4. Save the student

**Expected Result:**
- Student record stores the selected state/district names

**Status:** NOT TESTED

---

## TC-LOC-013: Selector works in Faculty form

**Steps:**
1. Navigate to **Faculty → Add Faculty**
2. Scroll to the address section
3. Select a state and then a district
4. Save the faculty record

**Expected Result:**
- Faculty record stores the selected state/district names

**Status:** NOT TESTED

---

## TC-LOC-014: Selector works in Admission form

**Steps:**
1. Navigate to **Admissions → Add Admission (from enquiry)**
2. Fill the address section using the state/district dropdowns
3. Submit the form

**Expected Result:**
- Admission record stores the selected state/district names

**Status:** NOT TESTED

---

## TC-LOC-015: Location master add/edit forms follow entry-form UX pattern

**Preconditions:**
- Logged in with `INDIA_LOCATION_MANAGE` permission
- Application is running with at least one country, state / UT, and district

**Steps:**
1. Navigate to **Location Master**.
2. Open **Add Country**, **Add State / UT**, and **Add District** one by one.
3. Verify each screen has the entry-form header, back icon button, sectioned form card, Save/Cancel footer, live preview card, and guidance card on desktop.
4. Submit each form with required fields blank.
5. Fill valid values and verify the live preview updates while typing.
6. Repeat steps 2–5 for editing an existing country, state / UT, and district.
7. Check the same screens in light mode, dark mode, and a 360 x 740 viewport.

**Expected Result:**
- All six add/edit flows use the current `entry-form-*` visual pattern consistently.
- Required-field validation appears under the correct controls and prevents invalid submission.
- Save buttons show a spinner while saving, then show the success toast and return to `/india-locations`.
- Mobile layout has no horizontal page scroll, clipped controls, or unreachable primary actions.

**Status:** NOT TESTED

---

## TC-LOC-016: Location Master list action icons match master list design

**Preconditions:**
- Logged in with `INDIA_LOCATION_MANAGE` permission.
- Application is running with at least one country, state / UT, and district.

**Steps:**
1. Navigate to **Location Master**.
2. Switch to card view and expand a country and a state / UT.
3. Verify country and state action buttons use compact rounded icon buttons for edit, add, and delete.
4. Verify district chips use SVG edit/delete icon buttons, not raw text glyphs.
5. Switch to table view, expand a state / UT row, and verify row actions align with other master tables.
6. Repeat in light mode, dark mode, and a 360 x 740 viewport.

**Expected Result:**
- Location Master action icons are visually consistent with Speciality / Academic Year master list patterns.
- Edit, add, and delete controls are clear, aligned, and not oversized or oddly rendered.
- Mobile layout has no horizontal page scroll, clipped controls, or unreachable primary actions.

**Status:** NOT TESTED

