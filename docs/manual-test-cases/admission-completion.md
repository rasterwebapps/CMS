# Admission Completion Manual Test Cases

These tests cover the admission completion flow which lets an Admin or Front Office user
pick an enquiry that is in `DOCUMENTS_SUBMITTED` status and complete the admission by
collecting all student and admission details. On submit, both the `students` and
`admissions` rows are created and the enquiry is moved to `ADMITTED`.

## TC-ADMCOMP-001: Admission Completion list shows only DOCUMENTS_SUBMITTED enquiries

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`.
- At least one enquiry exists in `DOCUMENTS_SUBMITTED` status, plus enquiries in other statuses
  (e.g. `INTERESTED`, `FEES_PAID`, `ADMITTED`).

**Steps:**
1. Open the side menu and click **Admission Management → Admission Completion**.
2. Wait for the list to load.

**Expected Result:**
- Only enquiries with status `DOCUMENTS_SUBMITTED` are shown.
- The header reads "Admission Completion" with subtitle about choosing one to complete.
- The count badge matches the number of rows.

**Status:** NOT TESTED

---

## TC-ADMCOMP-002: Empty state when no DOCUMENTS_SUBMITTED enquiries exist

**Preconditions:**
- User is logged in with `ROLE_ADMIN`.
- No enquiry is in `DOCUMENTS_SUBMITTED` status.

**Steps:**
1. Navigate to **Admission Management → Admission Completion**.

**Expected Result:**
- Empty-state card is shown with text "No enquiries pending admission completion".

**Status:** NOT TESTED

---

## TC-ADMCOMP-003: Complete admission with full student + admission data

**Preconditions:**
- One enquiry with a `programId` is in `DOCUMENTS_SUBMITTED` status.
- Logged in as `ROLE_ADMIN`.

**Steps:**
1. Open **Admission Management → Admission Completion**.
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
1. From the Admission Completion list, click "Complete admission" on the chosen enquiry.
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
1. From the Admission Completion list, click "Complete admission" on the enquiry.
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
- The "Admission Completion" item is not shown.

**Status:** NOT TESTED
