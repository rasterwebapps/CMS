# Fee Structure Management — Manual Test Cases

## TC-FEE-001: Add fee structures for a course — all types pre-populated

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- At least one Program, one Course, and one Academic Year exist in the system
- Application is running

**Steps:**
1. Navigate to **Fee Structures** from the sidebar
2. Click **Add Fee Structure**
3. Select a Program from the dropdown
4. Select a Course from the dropdown (loads after program is selected)
5. Select an Academic Year
6. Observe that all 8 fee type rows are pre-populated (Tuition Fee, Lab Fee, Library Fee, Examination Fee, Hostel Fee, Transport Fee, Miscellaneous, Late Fee)
7. Enter `50000` for **Tuition Fee**, `5000` for **Lab Fee**, `2000` for **Library Fee**; leave the rest at `0`
8. Verify the **Grand Total** shows `₹57,000`
9. Click **Save All**
10. Verify a success snackbar appears
11. Verify the Fee Structures list shows **one grouped row** for the selected course/academic year with Total Fee `₹57,000`

**Expected Result:**
- All 8 fee types are shown on load — no need to click "Add Fee Type"
- Only fee types with amount > 0 are saved (3 in this case)
- The list shows one grouped row per (Program + Course + Academic Year), not one row per fee type

**Status:** NOT TESTED

---

## TC-FEE-002: Prevent saving if all fee types have zero amount

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Application is running

**Steps:**
1. Navigate to **Fee Structures → Add Fee Structure**
2. Select a Program and Academic Year
3. Leave all amounts at `0`
4. Click **Save All**

**Expected Result:**
- A snackbar message appears: "Enter an amount for at least one fee type"
- No API call is made
- User remains on the form

**Status:** NOT TESTED

---

## TC-FEE-003: Year-wise amounts shown per fee type row for a multi-year course

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- A Course exists that belongs to a Program with `durationYears = 4`
- Application is running

**Steps:**
1. Navigate to **Fee Structures → Add Fee Structure**
2. Select the Program linked to the multi-year course
3. Select the 4-year Course from the Course dropdown
4. Select an Academic Year
5. Observe the fee item rows

**Expected Result:**
- Instead of a single Amount field in each row, **four year-wise amount inputs** appear below each fee item row (Year 1, Year 2, Year 3, Year 4)
- The per-row total updates as year amounts are entered
- The Grand Total sums all rows' totals

**Steps (continued):**
6. Enter `25000` in Year 1 and `25000` in Year 2 for **Tuition Fee** (row total = 50000)
7. Enter `5000` in Year 1 and `5000` in Year 2 for **Lab Fee** (row total = 10000)
8. Verify Grand Total = `₹60,000`
9. Click **Save All**

**Expected Result:**
- Fee structures are created with year-wise amounts saved correctly

**Status:** NOT TESTED

---

## TC-FEE-004: Grand total updates dynamically as amounts are entered

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Application is running

**Steps:**
1. Navigate to **Fee Structures → Add Fee Structure**
2. Select a Program and Academic Year (no course needed for single-year)
3. In the Tuition Fee row, type `30000` — Grand Total should show `₹30,000`
4. In the Lab Fee row, type `5000` — Grand Total should show `₹35,000`
5. Change the Tuition Fee amount to `40000` — Grand Total should update to `₹45,000`

**Expected Result:**
- Grand Total updates in real time as amounts are entered or changed in any row

**Status:** NOT TESTED

---

## TC-FEE-005: Cancel discards all unsaved data and returns to list

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Application is running

**Steps:**
1. Navigate to **Fee Structures → Add Fee Structure**
2. Select a Program and Academic Year
3. Enter amounts for multiple fee types
4. Click **Cancel**

**Expected Result:**
- The form is discarded without saving
- User is redirected back to the **Fee Structures** list
- No new fee structure records appear in the list

**Status:** NOT TESTED

---

## TC-FEE-006: Edit a fee structure group — bulk form opens pre-populated

**Preconditions:**
- At least one fee structure group exists in the system
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to **Fee Structures**
2. Click the **Edit** (pencil) button on any grouped row
3. Verify the form opens with all 8 fee types shown — existing types pre-filled with their saved amounts, missing types at `0`
4. Change the Tuition Fee amount
5. Click **Update**

**Expected Result:**
- The fee structure group is updated (existing records deleted and replaced with the new values)
- A success snackbar "Updated successfully" appears
- User is returned to the Fee Structures list with the updated total amount visible in the grouped row

**Status:** NOT TESTED

---

## TC-FEE-007: Fee Structures list shows grouped rows (one per Program / Academic Year / Course)

**Preconditions:**
- Multiple fee structures exist for the same Program, Academic Year, and Course
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to **Fee Structures**
2. Observe the table

**Expected Result:**
- Each row shows: Program, Course (or —), Academic Year, fee type count badge (e.g. "3 types"), Total Fee (accumulated sum)
- There is **one row per (Program + Course + Academic Year)** combination, not one row per fee type
- Clicking Edit opens the bulk form for that group

**Status:** NOT TESTED

---

## TC-FEE-008: Filter by Academic Year

**Preconditions:**
- Fee structures exist for multiple academic years
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to **Fee Structures**
2. Select an Academic Year from the "Academic Year" filter dropdown

**Expected Result:**
- The table immediately refreshes to show only groups belonging to the selected academic year
- Selecting "All" shows all groups

**Status:** NOT TESTED

---

## TC-FEE-009: Filter by Program (cascading to Course)

**Preconditions:**
- Fee structures exist for multiple programs
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to **Fee Structures**
2. Select a Program from the "Program" filter dropdown
3. Verify the **Course** filter dropdown appears and is populated with courses under that program
4. Select a Course

**Expected Result:**
- After selecting a Program, the table refreshes to show only groups for that program
- After selecting a Course, the table narrows further to groups for that specific course
- Clicking **Clear Filters** resets all dropdowns and shows all groups

**Status:** NOT TESTED

---

## TC-FEE-010: Delete a fee structure group

**Preconditions:**
- At least one grouped fee structure exists
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to **Fee Structures**
2. Click the **Delete** (trash) button on any grouped row
3. Confirm the deletion in the dialog

**Expected Result:**
- All fee structures belonging to that group (all fee types) are deleted
- A success snackbar "Deleted successfully" appears
- The deleted row disappears from the list

**Status:** NOT TESTED


## TC-FEE-001: Add multiple fee types for a course in one screen and save all

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- At least one Program, one Course, and one Academic Year exist in the system
- Application is running

**Steps:**
1. Navigate to **Fee Structures** from the sidebar
2. Click **Add Fee Structure**
3. Select a Program from the dropdown
4. Select a Course from the dropdown (loads after program is selected)
5. Select an Academic Year
6. In the first fee item row, select **Tuition Fee** as the Fee Type and enter `50000` as the amount
7. Click **+ Add Fee Type**
8. In the second row, select **Lab Fee** and enter `5000`
9. Click **+ Add Fee Type**
10. In the third row, select **Library Fee** and enter `2000`
11. Verify the **Grand Total** shows `₹57,000`
12. Click **Save All**
13. Verify a success snackbar appears with "3 fee structure(s) created"
14. Verify the Fee Structures list shows three new rows for the selected course/academic year

**Expected Result:**
- Three separate fee structure records are created (one per fee type) all linked to the same program, course, and academic year
- Grand total is computed correctly
- Success snackbar confirms the count

**Status:** NOT TESTED

---

## TC-FEE-002: Prevent saving if a fee item row has no fee type selected

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Application is running

**Steps:**
1. Navigate to **Fee Structures → Add Fee Structure**
2. Select a Program and Academic Year
3. Leave the Fee Type dropdown in the first row empty
4. Enter `10000` in the amount field
5. Click **Save All**

**Expected Result:**
- Form validation triggers, the Fee Type field in the first row shows a "Required" error
- No API call is made
- User remains on the form

**Status:** NOT TESTED

---

## TC-FEE-003: Duplicate fee type in the same batch is blocked

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Application is running

**Steps:**
1. Navigate to **Fee Structures → Add Fee Structure**
2. Select a Program and Academic Year
3. In the first row, select **Tuition Fee** and enter `50000`
4. Click **+ Add Fee Type**
5. Attempt to select **Tuition Fee** again in the second row

**Expected Result:**
- **Tuition Fee** does not appear in the dropdown of the second row (it is already used in the first row)
- Each row's Fee Type dropdown only lists types not yet selected in any other row
- Attempting to submit with programmatic duplicates returns a 400 error from the API with a "Duplicate fee type" message

**Status:** NOT TESTED

---

## TC-FEE-004: Year-wise amounts shown per fee type row for a multi-year course

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- A Course exists that belongs to a Program with `durationYears = 4`
- Application is running

**Steps:**
1. Navigate to **Fee Structures → Add Fee Structure**
2. Select the Program linked to the multi-year course
3. Select the 4-year Course from the Course dropdown
4. Select an Academic Year
5. Observe the first fee item row

**Expected Result:**
- Instead of a single Amount field in the row, **four year-wise amount inputs** appear below each fee item row (Year 1, Year 2, Year 3, Year 4)
- The per-row total updates as year amounts are entered
- The Grand Total sums all rows' totals

**Steps (continued):**
6. Enter `25000` in Year 1 and `25000` in Year 2 for the first fee item (Tuition Fee → row total = 50000)
7. Add a second fee item (Lab Fee), enter `5000` in Year 1 and `5000` in Year 2 (row total = 10000)
8. Verify Grand Total = `₹60,000`
9. Click **Save All**

**Expected Result:**
- Fee structures are created with year-wise amounts saved correctly
- Grand Total = sum of all year amounts across all fee items

**Status:** NOT TESTED

---

## TC-FEE-005: Grand total updates dynamically as amounts are entered

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Application is running

**Steps:**
1. Navigate to **Fee Structures → Add Fee Structure**
2. Select a Program and Academic Year (no course needed for single-year)
3. In the first row, type `30000` in the Amount field — Grand Total should show `₹30,000`
4. Click **+ Add Fee Type**, select Lab Fee, type `5000` — Grand Total should show `₹35,000`
5. Change the first row amount to `40000` — Grand Total should update to `₹45,000`

**Expected Result:**
- Grand Total updates in real time as amounts are entered or changed in any row

**Status:** NOT TESTED

---

## TC-FEE-006: Cancel discards all unsaved rows and returns to list

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Application is running

**Steps:**
1. Navigate to **Fee Structures → Add Fee Structure**
2. Select a Program and Academic Year
3. Enter `50000` in the first row
4. Add two more fee type rows with amounts
5. Click **Cancel**

**Expected Result:**
- The form is discarded without saving
- User is redirected back to the **Fee Structures** list
- No new fee structure records appear in the list

**Status:** NOT TESTED

---

## TC-FEE-007: Edit an existing fee structure (single-item edit mode unchanged)

**Preconditions:**
- At least one fee structure exists in the system
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to **Fee Structures**
2. Click the **Edit** (pencil) button on any existing fee structure row
3. Change the amount
4. Click **Update**

**Expected Result:**
- The fee structure is updated with the new amount
- A success snackbar "Updated" appears
- User is returned to the Fee Structures list with the updated amount visible

**Status:** NOT TESTED

---

## TC-FEE-008: Add Fee Type button is disabled when all fee types are used

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Application is running

**Steps:**
1. Navigate to **Fee Structures → Add Fee Structure**
2. Select a Program and Academic Year
3. Add rows for all 8 fee types (TUITION, LAB_FEE, LIBRARY_FEE, EXAMINATION_FEE, HOSTEL_FEE, TRANSPORT_FEE, MISCELLANEOUS, LATE_FEE)

**Expected Result:**
- After 8 rows are added, the **+ Add Fee Type** button becomes disabled
- No more rows can be added

**Status:** NOT TESTED

---

## TC-FEE-009: Remove a fee item row reduces grand total

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Application is running

**Steps:**
1. Navigate to **Fee Structures → Add Fee Structure**
2. Select Program and Academic Year
3. Add two fee items: Tuition Fee `50000` and Lab Fee `5000` (Grand Total = `₹55,000`)
4. Click the delete (trash) icon on the Lab Fee row

**Expected Result:**
- The Lab Fee row is removed
- Grand Total updates to `₹50,000`
- The delete button on the remaining single row is disabled (must keep at least one row)

**Status:** NOT TESTED

---

## TC-FEE-STRUCT-009: Amount column always visible and Add Fee Type button at top-right

**Preconditions:**
- User is logged in with ROLE_ADMIN
- At least one Program with a multi-year course (durationYears > 1) exists

**Steps:**
1. Navigate to **Fee Structures → Add Fee Structure**
2. Select a Program → select a 4-year Course → select an Academic Year
3. Observe the Fee Items section header — verify the **+ Add Fee Type** button appears at the top-right of the "Fee Items" card
4. Verify there are **no Mandatory or Active toggle columns** in the fee items table
5. Verify the **Amount (₹)** column header is visible
6. Observe that the Amount field in each row is readonly (greyed out) when a multi-year course is selected; year-wise fields appear below
7. Enter year-wise amounts; verify the Amount field in the row auto-populates with the sum
8. For a single-year course: select a 1-year program/course, verify the Amount field is editable directly

**Expected Result:**
- "+ Add Fee Type" button is at the top-right of the Fee Items header
- No Mandatory/Active toggles
- Amount column is always visible
- For multi-year: Amount is readonly and auto-computed from year fields

**Status:** NOT TESTED
