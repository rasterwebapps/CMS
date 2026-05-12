# Table Sorting, Alignment, and Filter Toolbar Standards

## TC-TABLE-001: Data tables expose sorting on every data column

**Preconditions:**
- User is logged in with a role that can access list screens.
- Frontend application is running.

**Steps:**
1. Open representative list screens such as Students, Admissions, Enquiries, Fee Structures, Receipts, Faculty, Programs, Labs, Inventory, and Scholarship screens.
2. Switch to table view where a card/table toggle is available.
3. Inspect every data column header except the actions column.
4. Click each data header once, then again.

**Expected Result:**
- Every non-actions data header shows a sort affordance.
- Clicking a header sorts ascending and descending.
- Actions columns do not show sorting.

**Status:** NOT TESTED

## TC-TABLE-002: Column alignment follows text, numeric, and status rules

**Preconditions:**
- User is logged in with a role that can access list screens.
- Tables contain at least one row with text, numeric/currency, and status values.

**Steps:**
1. Open representative table screens.
2. Verify text columns such as Name, Program, Course, Email, and Description.
3. Verify numeric/currency columns such as Fee, Paid, Outstanding, Count, Semester, Capacity, Duration, Quantity, and Amount.
4. Verify status/enum-like columns such as Status, Payment Status, Type, Mode, Category, Priority, Grade, Active/Inactive, and allocation/result status.

**Expected Result:**
- Text headers and cells are left aligned.
- Numeric and currency headers and cells are right aligned with tabular figures.
- Status/enum-like headers and cells are center aligned.

**Status:** NOT TESTED

## TC-TABLE-003: Sort arrow placement follows column type

**Preconditions:**
- User is logged in with a role that can access table screens.
- Tables contain sortable numeric and non-numeric columns.

**Steps:**
1. Open a table with numeric columns such as Fee Structures, Fee Collection, Inventory, or Exam Results.
2. Hover or click numeric headers.
3. Hover or click text/status/enum/date headers.

**Expected Result:**
- Numeric headers display the sort arrow before/on the left side of the header label.
- Text, date, status, and enum-like headers display the sort arrow after/on the right side of the header label.

**Status:** NOT TESTED

## TC-TABLE-004: Filter toolbars place search first

**Preconditions:**
- User is logged in with a role that can access screens with filters.

**Steps:**
1. Open screens with filter toolbars such as Students, Admissions, Enquiries, Document Submission, Admission Completion, Fee Finalization, Fee Collection, and Fee Structures.
2. Inspect the toolbar layout from left to right.

**Expected Result:**
- The search box appears before dropdown/date/status filters.
- Additional filters, clear buttons, counts, column controls, and view toggles appear after the search box.

**Status:** NOT TESTED

## TC-TABLE-005: Search box uses white background in light theme

**Preconditions:**
- Application is in the light theme.
- User is logged in and can access list screens.

**Steps:**
1. Open multiple list screens with search boxes.
2. Inspect the search input container before and during focus.

**Expected Result:**
- Search boxes use a white/card background in light theme.
- Focus state keeps the white/card background while showing the configured focus border/ring.

**Status:** NOT TESTED

