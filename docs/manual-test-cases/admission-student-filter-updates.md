# Admission and Student Filter Updates — Manual Test Cases

## TC-ADM-FILTER-001: Submit Documents filters are visible and functional

**Preconditions:**
- User is logged in with ROLE_ADMIN, ROLE_COLLEGE_ADMIN, or ROLE_FRONT_OFFICE.
- Application is running.
- At least one enquiry is in Fees Paid or Partially Paid status.

**Steps:**
1. Navigate to Admission Management → Submit Documents.
2. Verify the toolbar shows Program, Payment Status, and Student Type dropdown filters before the search box.
3. Select a Program filter.
4. Select Payment Status = Fees Paid.
5. Select Student Type = Day Scholar or Hosteler.
6. Click Clear Filters.

**Expected Result:**
- Filter dropdowns are visible in the toolbar.
- Table rows update based on the selected filters.
- Candidate count reflects filtered results.
- Clear Filters resets Program, Payment Status, Student Type, and Search.

**Status:** NOT TESTED

---

## TC-ADM-FILTER-002: Complete Admission filters are visible and functional

**Preconditions:**
- User is logged in with ROLE_ADMIN, ROLE_COLLEGE_ADMIN, or ROLE_FRONT_OFFICE.
- Application is running.
- At least one enquiry is in Documents Submitted status.

**Steps:**
1. Navigate to Admission Management → Complete Admission.
2. Verify the toolbar shows Program and Course dropdown filters before the search box.
3. Select a Program filter.
4. Verify Course options are limited to courses under the selected program.
5. Select a Course filter.
6. Click Clear Filters.

**Expected Result:**
- Filter dropdowns are visible in the toolbar.
- Table rows update based on Program and Course.
- Course filter resets if the selected course is not valid for the selected program.
- Clear Filters resets Program, Course, and Search.

**Status:** NOT TESTED

---

## TC-ROLL-FILTER-001: Roll Number Assignment filters are redesigned and functional

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN.
- Application is running.
- At least one student exists without a roll number.

**Steps:**
1. Navigate to Students → Roll Number Assignment.
2. Verify the toolbar shows styled Program and Course dropdown filters.
3. Verify the pending count appears on the right side of the toolbar.
4. Select a Program.
5. Verify Course options refresh based on the selected Program.
6. Select a Course.
7. Click Clear Filters.

**Expected Result:**
- Filters are aligned and styled consistently with the CMS UI.
- Pending count updates after filter changes.
- Course list resets correctly when Program is cleared.
- Clear Filters resets both Program and Course.

**Status:** NOT TESTED

