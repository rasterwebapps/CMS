# Enquiry Admission Conversion — Manual Test Cases

These test cases verify the end-to-end "Create Admission" workflow that replaced the old "Convert to Student" action.

---

## TC-EAC-001: Successful admission creation from DOCUMENTS_SUBMITTED enquiry

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- An enquiry exists in `DOCUMENTS_SUBMITTED` status with mandatory documents verified
- The application is running

**Steps:**
1. Navigate to **Enquiries** list.
2. Locate the enquiry in `DOCUMENTS_SUBMITTED` status.
3. Click the **Create Admission** icon button in the Actions column (graduation-cap icon).
4. Verify the browser navigates to `/enquiries/{id}/convert`.
5. Verify all fields are pre-filled from the enquiry: First Name, Last Name, Email, Phone, Semester, Academic Year From (current year), Academic Year To (current year + 1), Application Date (today), Admission Date (today).
6. Check **Parent / Guardian consent given** and **Applicant consent given**.
7. Click **Create Admission**.

**Expected Result:**
- A success snack bar "Admission created and student enrolled successfully" appears.
- The browser navigates to `/students`.
- A new Student record exists with the submitted details.
- A new Admission record linked to that student exists with status `APPROVED`, correct academic years, and both consent flags set to `true`.
- The enquiry status is now `ADMITTED`.

**Status:** NOT TESTED

---

## TC-EAC-002: Create Admission button is visible only for DOCUMENTS_SUBMITTED status

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Enquiries exist in various statuses: ENQUIRED, INTERESTED, FEES_FINALIZED, FEES_PAID, DOCUMENTS_SUBMITTED, ADMITTED

**Steps:**
1. Navigate to **Enquiries** list.
2. Inspect each row's action buttons.

**Expected Result:**
- The **Create Admission** button is visible only for rows with status `DOCUMENTS_SUBMITTED`.
- Rows with status `ADMITTED` show no conversion button.
- Rows with status `ENQUIRED`, `INTERESTED`, `FEES_FINALIZED`, `FEES_PAID` do not show the button.

**Status:** NOT TESTED

---

## TC-EAC-003: Enquiry detail page — Create Admission button label

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- An enquiry exists in `DOCUMENTS_SUBMITTED` status

**Steps:**
1. Navigate to the **Enquiry Detail** page for a `DOCUMENTS_SUBMITTED` enquiry.
2. Inspect the action buttons in the header area.

**Expected Result:**
- A button labelled **Create Admission** with a graduation-cap icon is displayed.
- There is no button labelled "Convert to Student".
- Clicking **Create Admission** navigates to `/enquiries/{id}/convert`.

**Status:** NOT TESTED

---

## TC-EAC-004: Pre-filled admission fields match suggested values

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- An enquiry exists in `DOCUMENTS_SUBMITTED` status

**Steps:**
1. Navigate to `/enquiries/{id}/convert` for a DOCUMENTS_SUBMITTED enquiry.
2. Observe the pre-filled values in the form.

**Expected Result:**
- `academicYearFrom` is pre-filled with the current calendar year (e.g., 2026).
- `academicYearTo` is pre-filled with current year + 1 (e.g., 2027).
- `applicationDate` is pre-filled with today's date.
- `admissionDate` is pre-filled with today's date.
- Student fields (First Name, Last Name, Email, Phone) are pre-filled from enquiry data.

**Status:** NOT TESTED

---

## TC-EAC-005: Enquiry status becomes ADMITTED after successful submission

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- An enquiry exists in `DOCUMENTS_SUBMITTED` status

**Steps:**
1. Complete the **Create Admission** form (TC-EAC-001).
2. After redirection to `/students`, navigate back to the Enquiries list.
3. Find the original enquiry.

**Expected Result:**
- The enquiry's status badge shows **ADMITTED** (not CONVERTED).
- The enquiry status in the detail page shows **ADMITTED**.
- The enquiry Status History tab shows a transition from `DOCUMENTS_SUBMITTED` → `ADMITTED`.

**Status:** NOT TESTED

---

## TC-EAC-006: Standalone Admissions form still works independently

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- At least one student exists without an admission record

**Steps:**
1. Navigate to **Admissions** → **New Admission** (`/admissions/new`).
2. Select a student, fill in academic year, application date.
3. Click **Save**.

**Expected Result:**
- Admission is created successfully.
- The standalone admissions form continues to function independently of the enquiry conversion flow.

**Status:** NOT TESTED

---

## TC-EAC-007: Both Student and Admission records are created atomically

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- An enquiry exists in `DOCUMENTS_SUBMITTED` status

**Steps:**
1. Complete the **Create Admission** form with valid data.
2. After success, navigate to **Students** list and verify the new student appears.
3. Navigate to **Admissions** list and verify a new admission linked to that student appears.

**Expected Result:**
- Both a Student record and an Admission record are created in the same transaction.
- The Admission record references the correct student ID.
- The Admission status is `APPROVED`.
- If either creation fails, neither record should be persisted (transactional rollback).

**Status:** NOT TESTED

---

## TC-EAC-008: Validation prevents submission with missing required fields

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- An enquiry exists in `DOCUMENTS_SUBMITTED` status

**Steps:**
1. Navigate to `/enquiries/{id}/convert`.
2. Clear the **Email** field.
3. Click **Create Admission**.

**Expected Result:**
- The form does not submit.
- A validation error message "Valid email is required" appears below the Email field.
- No Student or Admission records are created.

**Status:** NOT TESTED

---

## TC-EAC-009: Duplicate email is rejected

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- An enquiry exists in `DOCUMENTS_SUBMITTED` status
- A student already exists with the email "existing@college.edu"

**Steps:**
1. Navigate to `/enquiries/{id}/convert`.
2. Change the Email field to "existing@college.edu".
3. Click **Create Admission**.

**Expected Result:**
- The API returns a 409/400 error.
- The snack bar shows "Failed to create admission".
- No duplicate student or admission is created.
- The enquiry status remains `DOCUMENTS_SUBMITTED`.

**Status:** NOT TESTED

---

## TC-EAC-010: Focus mode title shows "Create Admission"

**Preconditions:**
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Navigate to `/enquiries/{id}/convert` for a `DOCUMENTS_SUBMITTED` enquiry.
2. Observe the focus mode header bar at the top of the page.

**Expected Result:**
- The header title reads **Create Admission** (not "Convert Enquiry to Student").

**Status:** NOT TESTED
