## TC-FILTER-001: List filters use the compact 32px toolbar height

**Preconditions:**
- User is logged in with a role that can access list screens, such as `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`.
- Frontend application is running.

**Steps:**
1. Open Submit Documents and visually note the filter/search/toolbar control height.
2. Open Verify Documents and confirm its controls still match Submit Documents.
3. Open representative list screens that use global filters: Enquiries, Admissions, Students, Roll Number Assignment, Receipts, User Management, and Role Management.
4. Compare search boxes, native select filters, date filters, Clear Filters buttons, and Columns buttons on each screen.
5. Toggle between light mode and dark mode and repeat the visual comparison.
6. Resize the browser to a mobile-width viewport around 360 × 740 and confirm controls remain aligned and do not clip or cause page-level horizontal scroll.

**Expected Result:**
- Filter/search/toolbar controls use the same compact 32px visual height as Submit Documents and Verify Documents.
- Controls remain cleanly aligned in light mode, dark mode, and mobile-width layouts.
- Existing filter, search, clear, and column-toggle interactions continue to work.

**Status:** NOT TESTED
