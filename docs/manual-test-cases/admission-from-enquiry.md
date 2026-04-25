# Admission Creation from Enquiry — Manual Test Cases

These test cases cover the "Create from Enquiry" flow added to the **New Admission** screen
(`/admissions/new`). The screen now has a mode toggle: **From Enquiry** (default) and
**Existing Student** (manual).

---

## TC-ADM-ENQ-001: Mode toggle renders correctly on New Admission screen

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`
- Application is running

**Steps:**
1. Navigate to **Admissions → New Admission** (`/admissions/new`).
2. Observe the mode toggle at the top of the form.

**Expected Result:**
- Two toggle buttons are visible: **From Enquiry** and **Existing Student**.
- **From Enquiry** is active by default (highlighted).
- The enquiry dropdown and student detail fields are shown.
- The student picker dropdown is not shown.

**Status:** NOT TESTED

---

## TC-ADM-ENQ-002: Mode toggle is hidden on Edit Admission screen

**Preconditions:**
- At least one admission exists.
- User is logged in with `ROLE_ADMIN`.

**Steps:**
1. Navigate to the admission list and click **Edit** on any record.
2. Observe the form.

**Expected Result:**
- The mode toggle is **not** shown on the edit screen.
- The existing student picker and standard admission fields are displayed as before.

**Status:** NOT TESTED

---

## TC-ADM-ENQ-003: Enquiry dropdown lists only DOCUMENTS_SUBMITTED enquiries

**Preconditions:**
- At least one enquiry is in `DOCUMENTS_SUBMITTED` status.
- At least one enquiry is in `FEES_PAID` or `PARTIALLY_PAID` status (not yet document-submitted).
- User is logged in with `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`.

**Steps:**
1. Navigate to **Admissions → New Admission**.
2. Open the **Select Enquiry** dropdown.
3. Note the enquiries listed.

**Expected Result:**
- Only enquiries in `DOCUMENTS_SUBMITTED` status appear in the list.
- Enquiries in `FEES_PAID`, `PARTIALLY_PAID`, `ADMITTED`, or other statuses do not appear.
- Each option shows the student name, program, and course (if present).

**Status:** NOT TESTED

---

## TC-ADM-ENQ-004: Selecting an enquiry pre-fills student and admission details

**Preconditions:**
- At least one enquiry is in `DOCUMENTS_SUBMITTED` status with a program assigned.
- User is logged in with `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`.

**Steps:**
1. Navigate to **Admissions → New Admission**.
2. Select an enquiry from the **Select Enquiry** dropdown.
3. Observe the form fields below.

**Expected Result:**
- An enquiry summary strip appears below the dropdown showing Program, Course (if any), Net Fee, and Status.
- **First Name** and **Last Name** are pre-filled from the enquiry's name.
- **Email** and **Phone** are pre-filled if available.
- **Semester** is set to the suggested value.
- **Admission Date** is set to today's date.
- **Academic Year From / To** and **Application Date** are pre-filled from the prefill response.

**Status:** NOT TESTED

---

## TC-ADM-ENQ-005: Submitting the from-enquiry form creates a Student and Admission atomically

**Preconditions:**
- An enquiry in `DOCUMENTS_SUBMITTED` status with a program exists.
- User is logged in with `ROLE_ADMIN`.

**Steps:**
1. Navigate to **Admissions → New Admission**.
2. Select the enquiry from the dropdown.
3. Verify pre-fill data; correct any fields if needed (e.g., First Name, Last Name, Email).
4. Optionally fill in Personal Information, Demographics, Family, Address.
5. Click **Create Admission**.

**Expected Result:**
- A success toast message is shown: *"Admission created and student enrolled successfully"*.
- The user is redirected to the Admissions list (`/admissions`).
- A new **Student** record exists in the Students list with the entered name and email.
- A new **Admission** record exists in the Admissions list with status `APPROVED`.
- The enquiry's status changes to `ADMITTED` (visible in the Enquiry list / Enquiry detail).

**Status:** NOT TESTED

---

## TC-ADM-ENQ-006: Validation prevents submission when no enquiry is selected

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_FRONT_OFFICE`.

**Steps:**
1. Navigate to **Admissions → New Admission** (From Enquiry mode).
2. Do not select an enquiry.
3. Click **Create Admission**.

**Expected Result:**
- The form does not submit.
- A validation error appears under the **Select Enquiry** dropdown: *"Please select an enquiry"*.

**Status:** NOT TESTED

---

## TC-ADM-ENQ-007: Validation prevents submission when required student fields are empty

**Preconditions:**
- An enquiry in `DOCUMENTS_SUBMITTED` status exists.
- The enquiry's stored name has no email.

**Steps:**
1. Navigate to **Admissions → New Admission** and select the enquiry.
2. Clear the **Email** field.
3. Click **Create Admission**.

**Expected Result:**
- The form does not submit.
- A validation error appears: *"Valid email is required"*.

**Status:** NOT TESTED

---

## TC-ADM-ENQ-008: Enquiry already in ADMITTED status does not appear in the dropdown

**Preconditions:**
- An enquiry was previously admitted (status = `ADMITTED`).
- User is logged in with `ROLE_ADMIN`.

**Steps:**
1. Navigate to **Admissions → New Admission**.
2. Open the **Select Enquiry** dropdown.

**Expected Result:**
- The already-admitted enquiry does **not** appear in the list.

**Status:** NOT TESTED

---

## TC-ADM-ENQ-009: Switching to "Existing Student" mode resets the form and shows student picker

**Preconditions:**
- An enquiry is selected in From Enquiry mode.
- User is logged in with `ROLE_ADMIN`.

**Steps:**
1. Navigate to **Admissions → New Admission**.
2. Select an enquiry (form pre-fills).
3. Click the **Existing Student** toggle button.

**Expected Result:**
- The mode switches to **Existing Student**.
- The enquiry dropdown, summary strip, and student detail fields are hidden.
- The student picker dropdown appears.
- The previously selected enquiry selection is cleared.

**Status:** NOT TESTED

---

## TC-ADM-ENQ-010: Existing Student (manual) mode — admission creation still works

**Preconditions:**
- At least one student record exists.
- User is logged in with `ROLE_ADMIN`.

**Steps:**
1. Navigate to **Admissions → New Admission**.
2. Click **Existing Student** mode.
3. Select a student from the dropdown.
4. Fill in Academic Year From, To, Application Date.
5. Click **Save**.

**Expected Result:**
- A success toast message is shown: *"Admission saved successfully"*.
- A new admission record appears in the Admissions list for the selected student.

**Status:** NOT TESTED

---

## TC-ADM-ENQ-011: Empty enquiry list shows a helpful hint message

**Preconditions:**
- No enquiries are in `DOCUMENTS_SUBMITTED` status (all are in earlier stages or already admitted).
- User is logged in with `ROLE_ADMIN`.

**Steps:**
1. Navigate to **Admissions → New Admission**.
2. Observe the **Select Enquiry** area.

**Expected Result:**
- The hint message is displayed: *"No enquiries are currently awaiting admission (documents must be submitted first)."*
- The dropdown is present but empty.

**Status:** NOT TESTED
