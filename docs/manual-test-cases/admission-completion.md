# Complete Admission Manual Test Cases

These tests cover the complete admission flow which lets an Admin or Front Office user
pick an enquiry that is in `DOCUMENTS_SUBMITTED` status and complete the admission by
collecting all student and admission details. On submit, both the `students` and
`admissions` rows are created and the enquiry is moved to `ADMITTED`.

## TC-ADMCOMP-001: Complete Admission list shows only DOCUMENTS_SUBMITTED enquiries

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`.
- At least one enquiry exists in `DOCUMENTS_SUBMITTED` status, plus enquiries in other statuses
  (e.g. `INTERESTED`, `FEES_PAID`, `ADMITTED`).

**Steps:**
1. Open the side menu and click **Admission Management → Complete Admission**.
2. Wait for the list to load.

**Expected Result:**
- Only enquiries with status `DOCUMENTS_SUBMITTED` are shown.
- The header reads "Complete Admission" with subtitle about choosing one to complete.
- The count badge matches the number of rows.

**Status:** NOT TESTED

---

## TC-ADMCOMP-002: Empty state when no DOCUMENTS_SUBMITTED enquiries exist

**Preconditions:**
- User is logged in with `ROLE_ADMIN`.
- No enquiry is in `DOCUMENTS_SUBMITTED` status.

**Steps:**
1. Navigate to **Admission Management → Complete Admission**.

**Expected Result:**
- Empty-state card is shown with text "No enquiries pending complete admission".

**Status:** NOT TESTED

---

## TC-ADMCOMP-003: Complete admission with full student + admission data

**Preconditions:**
- One enquiry with a `programId` is in `DOCUMENTS_SUBMITTED` status.
- Logged in as `ROLE_ADMIN`.

**Steps:**
1. Open **Admission Management → Complete Admission**.
2. Click the "Complete admission" action on the chosen row.
3. On the conversion screen, confirm the prefilled fields (name, email, semester,
   academic year, application date) are correct.
4. Fill in **Personal Information**: Date of Birth, Gender, Aadhar Number.
5. Fill in **Demographics**: Nationality, Religion, Community Category, Caste, Blood Group.
6. Fill in **Family Information**: Father's Name, Mother's Name, Parent Mobile.
7. Fill in **Address**: Postal Address, Street, City, District, State, Pincode.
8. Fill in **Declaration**: Declaration Place, Declaration Date.
9. Tick both consent checkboxes.
10. Click **Create Admission**.

**Expected Result:**
- Snackbar shows "Admission created and student enrolled successfully".
- Browser is redirected to `/students`.
- A new row exists in the `students` table with all the values entered (DOB, gender,
  aadhar, nationality, religion, community, caste, blood group, father/mother/parent
  mobile, full address) and `status = ACTIVE`.
- A new row exists in the `admissions` table linked to the new student with the entered
  academic years, application date, declaration place/date, both consent flags `true`,
  and `status = APPROVED`.
- The original enquiry's status is now `ADMITTED` and `convertedStudentId` points to the
  new student.

**Status:** NOT TESTED

---

## TC-ADMCOMP-004: Complete admission with only mandatory fields

**Preconditions:**
- One enquiry with a `programId` is in `DOCUMENTS_SUBMITTED` status.
- Logged in as `ROLE_ADMIN`.

**Steps:**
1. From the Complete Admission list, click "Complete admission" on the chosen enquiry.
2. Leave all optional sections (Personal Information, Demographics, Family Information,
   Address, Declaration) untouched.
3. Click **Create Admission**.

**Expected Result:**
- Admission is created successfully.
- `students` row has only the basic fields set; optional columns are `NULL`.
- `admissions` row is created with academic year, application date, and `status = APPROVED`.
- Enquiry is moved to `ADMITTED`.

**Status:** NOT TESTED

---

## TC-ADMCOMP-005: Conversion is rejected when enquiry is not DOCUMENTS_SUBMITTED

**Preconditions:**
- An enquiry exists in any non-`DOCUMENTS_SUBMITTED` status (for example `INTERESTED`).
- Logged in as `ROLE_ADMIN`.

**Steps:**
1. With a tool like `curl` or Postman, send `POST /api/v1/enquiries/{id}/convert`
   with a valid `EnquiryConversionRequest` body, where `{id}` is the non-eligible enquiry.

**Expected Result:**
- HTTP `409 Conflict` (or equivalent error) is returned with a message containing
  `DOCUMENTS_SUBMITTED`.
- No new `students` or `admissions` row is created.
- Enquiry status is unchanged.

**Status:** NOT TESTED

---

## TC-ADMCOMP-006: Conversion is rejected when email already exists for a student

**Preconditions:**
- A `students` row already exists with email `existing@college.edu`.
- An enquiry is in `DOCUMENTS_SUBMITTED` status.
- Logged in as `ROLE_ADMIN`.

**Steps:**
1. From the Complete Admission list, click "Complete admission" on the enquiry.
2. Set the email field to `existing@college.edu`.
3. Submit the form.

**Expected Result:**
- An error snackbar is shown ("Failed to create admission").
- No new student or admission row is created.

**Status:** NOT TESTED

---

## TC-ADMCOMP-007: Non-admin / non-front-office user cannot see the menu item

**Preconditions:**
- Logged in as a user without `ROLE_ADMIN` or `ROLE_FRONT_OFFICE` (for example `ROLE_FACULTY`).

**Steps:**
1. Open the side menu under **Admission Management**.

**Expected Result:**
- The "Complete Admission" item is not shown.

**Status:** NOT TESTED
---
## TC-ADMCOMP-008: Verify Documents button opens document verification screen
**Preconditions:**
- At least one enquiry is in `DOCUMENTS_SUBMITTED` status.
- User is logged in with `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`.
**Steps:**
1. Navigate to **Admission Management → Complete Admission**.
2. Locate an enquiry in the list.
3. Click the **"Verify Docs"** button (green shield icon) in the actions column.
**Expected Result:**
- Browser navigates to `/enquiries/document-submission/{id}?mode=verify`.
- The document collection screen loads in **verify mode** — header shows "X/Y verified".
- Section hint reads: "Verify each document — verified docs are locked and cannot be changed".
- VERIFIED documents show a green "Locked" badge and have no edit/upload/status-change buttons.
- UPLOADED (not yet verified) documents show the full verify/reject/upload controls.
**Status:** NOT TESTED
---
## TC-ADMCOMP-009: Cannot complete admission without verifying all documents
**Preconditions:**
- An enquiry is in `DOCUMENTS_SUBMITTED` status with at least one document still in UPLOADED (not VERIFIED) status.
- User is logged in with `ROLE_ADMIN`.
**Steps:**
1. Navigate to **Admission Management → Complete Admission**.
2. Click the **"Complete"** button (or click the table row) for that enquiry.
**Expected Result:**
- A warning toast appears: "N document(s) still need verification. Please verify all documents first."
- The browser navigates to the document verification screen for that enquiry (`?mode=verify`).
- The convert/admission form screen is NOT opened.
**Status:** NOT TESTED
---
## TC-ADMCOMP-010: Create Admission screen shows blocking banner when docs unverified
**Preconditions:**
- An enquiry is in `DOCUMENTS_SUBMITTED` status with at least one document in UPLOADED status.
- User navigates directly to `/enquiries/{id}/convert` (bypassing the list check).
**Steps:**
1. Open the Create Admission screen via direct URL.
**Expected Result:**
- A yellow warning banner is displayed at the top of the form body.
- Banner reads: "Document Verification Required — N mandatory document(s) have not been verified yet."
- A "Go to Verify Documents" link is visible in the banner.
- The "Create Admission" button is replaced with an amber "Verify Documents First" button that links to the verify screen.
- Submitting the form is blocked (backend returns 400 with "mandatory documents must be verified").
**Status:** NOT TESTED
---
## TC-ADMCOMP-011: All documents verified — admission can be completed
**Preconditions:**
- All mandatory enquiry documents are marked as VERIFIED.
- User is logged in with `ROLE_ADMIN`.
**Steps:**
1. Navigate to **Admission Management → Complete Admission**.
2. Click **"Complete"** for the enquiry.
**Expected Result:**
- No warning is shown.
- Browser navigates directly to the Create Admission form.
- The document verification banner is NOT visible.
- The "Create Admission" button is enabled and submittable.
**Status:** NOT TESTED
---
## TC-ADMCOMP-012: Verified documents are locked in verify mode
**Preconditions:**
- An enquiry has at least one VERIFIED and one UPLOADED document.
- User opens the document verification screen via `?mode=verify`.
**Steps:**
1. Observe a document with status VERIFIED.
2. Attempt to click the verify/reject/upload buttons on it.
**Expected Result:**
- The VERIFIED document card shows a green "Locked" badge.
- No verify, reject, upload, replace, or delete buttons are visible for VERIFIED documents.
- Remarks input is disabled (read-only) for VERIFIED documents.
- Other UPLOADED documents still show the full set of action buttons.
**Status:** NOT TESTED
---
## TC-ADMCOMP-013: Backend enforces document verification on conversion API
**Preconditions:**
- An enquiry is in `DOCUMENTS_SUBMITTED` status with UPLOADED documents (not VERIFIED).
**Steps:**
1. Send a POST request directly to `/api/v1/enquiries/{id}/convert` with a valid conversion payload.
**Expected Result:**
- Response status is `400 Bad Request`.
- Response body contains: `"All mandatory documents must be verified before completing admission"`.
**Status:** NOT TESTED
