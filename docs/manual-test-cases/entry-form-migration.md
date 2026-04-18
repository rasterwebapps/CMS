# Manual Test Cases — Entry Form Pattern Migration

All ADD/EDIT forms have been migrated from Angular Material `mat-form-field` / `mat-card` to the
global `entry-form-page` / `entry-form-card` / `field-group` / `field-input` design system.

---

## TC-EFM-001: Department — Create a new department

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Application is running

**Steps:**
1. Navigate to Departments → click **Add Department**
2. Verify the page shows the new entry-form layout (white card, no Material card chrome)
3. Verify there is a page subtitle below the title
4. Fill in **Name**: `Test Department`
5. Fill in **Code**: `TD`
6. Fill in **Description**: `Test Description`
7. Click **Create Department**

**Expected Result:**
- Form submits successfully; snackbar shows "Department created"
- User is redirected to the departments list; new department appears

**Status:** NOT TESTED

---

## TC-EFM-002: Department — Validation errors display correctly

**Preconditions:**
- On the Department create form

**Steps:**
1. Click **Create Department** without filling in any fields
2. Observe the Name and Code fields

**Expected Result:**
- `.field-error` text appears below each required field (no Material `mat-error` elements)
- Fields get a red border (`field-input--error` class applied)

**Status:** NOT TESTED

---

## TC-EFM-003: Course — Create with 2-column row

**Preconditions:**
- At least one Program exists
- Navigate to Courses → **Add Course**

**Steps:**
1. Verify **Course Code** and **Specialization** appear side-by-side in a `field-row` layout
2. Select a Program from the native `<select>` dropdown
3. Fill in Name and Code; click **Create Course**

**Expected Result:**
- Two-column layout renders correctly; form submits successfully

**Status:** NOT TESTED

---

## TC-EFM-004: Academic Year — Date inputs are native type="date"

**Preconditions:**
- Navigate to Academic Years → **Add Academic Year**

**Steps:**
1. Verify Start Date and End Date are native `<input type="date">` (browser date picker, no Material datepicker calendar icon)
2. Enter a name, start date, and end date
3. Toggle **Set as Current Academic Year** slide toggle
4. Click **Create Academic Year**

**Expected Result:**
- Native date pickers work; form submits; academic year created

**Status:** NOT TESTED

---

## TC-EFM-005: Semester — All three dropdowns use native select

**Preconditions:**
- At least one Academic Year exists
- Navigate to Semesters → **Add Semester**

**Steps:**
1. Verify Academic Year, Semester Number dropdowns are native `<select>` elements
2. Select values; fill in Name and dates
3. Click **Create Semester**

**Expected Result:**
- No `mat-select` panels; form submits; semester created

**Status:** NOT TESTED

---

## TC-EFM-006: Lab — 2-column rows render correctly

**Preconditions:**
- At least one Department exists
- Navigate to Labs → **Add Lab**

**Steps:**
1. Verify Lab Type/Department, Building/Room Number, Capacity/Status are each in 2-column `field-row` layout
2. Fill in all fields; click **Create Lab**

**Expected Result:**
- Side-by-side fields display correctly; form submits

**Status:** NOT TESTED

---

## TC-EFM-007: Fee Structure — Selection dropdowns converted

**Preconditions:**
- Navigate to Fee Structures → **Add Fee Structure**

**Steps:**
1. Verify Academic Year, Program, and Course dropdowns in the "Select Criteria" panel are now native `<select>` with `field-group` / `field-label` styling
2. Verify the fee item grid below the panel is unchanged
3. Select Academic Year, Program, and Course; add a fee item; click **Save**

**Expected Result:**
- Selection panel uses new input style; fee items remain unchanged; form saves successfully

**Status:** NOT TESTED

---

## TC-EFM-008: Equipment — Dates are native type="date"

**Preconditions:**
- At least one Lab exists
- Navigate to Equipment → **Add Equipment**

**Steps:**
1. Verify Purchase Date and Warranty Expiry are native `<input type="date">`
2. Fill in all required fields; click **Create Equipment**

**Expected Result:**
- Form uses native date pickers; saves successfully

**Status:** NOT TESTED

---

## TC-EFM-009: Settings — slide-toggle wrapped in field-group

**Preconditions:**
- Navigate to Settings → **Add Configuration**

**Steps:**
1. Verify Data Type and Category appear side-by-side in a `field-row`
2. Verify the **Editable** slide-toggle is wrapped in a `field-group` div for consistent spacing
3. Fill in all required fields; click **Create Configuration**

**Expected Result:**
- Toggle has consistent spacing with other fields; form saves

**Status:** NOT TESTED

---

## TC-EFM-010: Agent — form layout and submit

**Preconditions:**
- Navigate to Agents → **Add Agent**

**Steps:**
1. Verify Phone/Email and Area/Locality are in 2-column `field-row` layout
2. Verify **Active** toggle is wrapped in `field-group`
3. Fill in Name; click **Create Agent**

**Expected Result:**
- Layout renders correctly; agent created

**Status:** NOT TESTED

---

## TC-EFM-011: Referral Type — conditional commission field

**Preconditions:**
- Navigate to Referral Types → **Add Referral Type**

**Steps:**
1. Verify the **Has Commission** slide toggle is in a `field-group`
2. Toggle **Has Commission** ON
3. Verify **Commission Amount** field appears below
4. Fill in Name, Code, and Commission Amount; click **Create Referral Type**

**Expected Result:**
- Conditional field appears/disappears correctly; form saves

**Status:** NOT TESTED

---

## TC-EFM-012: Faculty — 2-column rows and native date

**Preconditions:**
- At least one Department exists
- Navigate to Faculty → **Add Faculty**

**Steps:**
1. Verify Employee Code/Email, First Name/Last Name, Phone/Department, Designation/Joining Date pairs are in `field-row` layout
2. Verify Joining Date is a native `<input type="date">` (not mat-datepicker)
3. Fill in all required fields; click **Create Faculty**

**Expected Result:**
- Two-column layout works; native date picker works; faculty created

**Status:** NOT TESTED

---

## TC-EFM-013: Student — sections, dividers, and field-rows

**Preconditions:**
- At least one Program exists
- Navigate to Students → **Add Student**

**Steps:**
1. Verify four sections: **Basic Information**, **Personal Details**, **Family Details**, **Address**
2. Verify section titles use `.form-section-title` style (small caps, muted colour)
3. Verify `<hr class="form-section-divider">` separates sections (no `mat-divider`)
4. Verify field pairs are in `field-row` layout throughout
5. Fill in required fields (Roll Number, First/Last Name, Email, Program, Semester, Admission Date)
6. Click **Create Student**

**Expected Result:**
- Sections and dividers render correctly; form saves; student appears in list

**Status:** NOT TESTED

---

## TC-EFM-014: Enquiry — sidebar preserved with new form style

**Preconditions:**
- At least one Program, Course, Fee Structure, and Referral Type exist
- Navigate to Enquiries → **Add Enquiry**

**Steps:**
1. Verify the main form uses the `entry-form-card` style
2. Verify Enquiry Date is a native `<input type="date">`
3. Select a Program → verify courses dropdown appears
4. Select a Course → verify the Fee Structure sidebar appears on the right
5. Select Student Type (e.g., DAY_SCHOLAR) → verify total fee updates
6. Fill in Name, Referral Type; click **Create Enquiry**

**Expected Result:**
- Two-column layout (form + sidebar) works correctly; fee sidebar shows correct total; enquiry created

**Status:** NOT TESTED

---

## TC-EFM-015: All forms — page subtitles present

**Preconditions:**
- Open each migrated form

**Steps:**
1. For each form, verify a `<p class="page-subtitle">` is visible below the page title
2. Verify "Create" forms say "Fill in the details below to register..."
3. Verify "Edit" forms say "Update the ... details below."

**Expected Result:**
- All 13 migrated forms have appropriate subtitles in both create and edit mode

**Status:** NOT TESTED

---

## TC-EFM-016: Responsive — field-row collapses on small screens

**Preconditions:**
- Any form with `field-row` layout (e.g., Faculty, Student, Lab)

**Steps:**
1. Resize browser to < 600px width
2. Observe field-row pairs

**Expected Result:**
- Two-column `field-row` collapses to single column; no horizontal overflow

**Status:** NOT TESTED
