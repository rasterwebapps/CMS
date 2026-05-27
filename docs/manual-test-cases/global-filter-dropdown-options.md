# Global Filter Dropdown Options — Manual Test Cases

## TC-GLOBAL-FILTER-001: Student list filter dropdowns populate after data load

**Preconditions:**
- User is logged in with a role that can access Student Management.
- At least two students exist with different programs and/or years of study.

**Steps:**
1. Navigate to Students.
2. Open the **All Programs** dropdown.
3. Open the **All Semesters** dropdown.
4. Select one program and verify the table filters to matching students.
5. Clear filters, then select one semester/year and verify the table filters to matching students.

**Expected Result:**
- Program and semester dropdowns show the individual values from the loaded student records.
- Selecting an option filters the table correctly.
- Clearing filters restores the full student list.

**Status:** NOT TESTED

---

## TC-GLOBAL-FILTER-002: Enquiry list program/course dropdowns populate after data load

**Preconditions:**
- User is logged in with a role that can access Enquiry Management.
- The selected enquiry date range contains enquiries for at least two programs and courses.

**Steps:**
1. Navigate to Enquiries.
2. Adjust the date range if needed so matching enquiries are visible.
3. Open the **All Programs** dropdown.
4. Select a program.
5. Open the **All Courses** dropdown.
6. Select a course and verify the table filters to matching enquiries.
7. Clear filters and verify the full date-range list returns.

**Expected Result:**
- Program dropdown shows individual programs from the loaded enquiry date range.
- Course dropdown shows individual courses and narrows to the selected program when applicable.
- Selecting program/course options filters the table correctly.
- Clearing filters restores the full date-range list.

**Status:** NOT TESTED

---

## TC-GLOBAL-FILTER-003: Existing service-backed list filter dropdowns still populate

**Preconditions:**
- User is logged in with access to academic, finance, faculty, lab, and admission screens.
- Master data exists for programs, courses, departments, academic years, and fee structures.

**Steps:**
1. Open Course List and verify **All Programs** shows individual programs.
2. Open Admission List and verify **All Programs** shows admission programs.
3. Open Fee Structure List and verify **All Academic Years**, **All Programs**, and program-specific **All Courses** options populate.
4. Open Faculty List and verify **All Departments** shows individual departments.
5. Open Lab List and verify **All Departments**, **All Types**, and **All Statuses** options populate.
6. Open Document Submission, Document Verification, and Admission Completion lists and verify their program/course/type/status dropdowns show applicable values.

**Expected Result:**
- Each dropdown opens with the applicable individual entries, not only the default **All ...** option.
- Selecting an entry filters the current list without breaking search, status filters, sorting, or pagination.
- Screens with no matching data show their normal empty state rather than a broken dropdown.

**Status:** NOT TESTED
