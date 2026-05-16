# Admission Number Generation — Manual Test Cases

## TC-ADMNO-001: Generate admission number on successful admission completion

**Preconditions:**
- User is logged in with admission completion permission.
- Enquiry is in `DOCUMENTS_SUBMITTED` status.
- Mandatory documents are verified.
- Joining academic year `2025-2026` exists.

**Steps:**
1. Open Admission Management → Complete Admission.
2. Open a document-verified enquiry and complete admission.
3. Open the created student profile.
4. Open the admission detail page for the student.

**Expected Result:**
- Admission completion succeeds.
- Student profile shows admission number in format `ADM-2526-0001` or the next available sequence.
- Admission detail shows the same admission number.
- Roll number may remain blank until roll number generation.

**Status:** NOT TESTED

## TC-ADMNO-002: Admission number is not editable

**Preconditions:**
- A student exists with an admission number.

**Steps:**
1. Open the student edit screen.
2. Check identity fields on the form.
3. Open the admission edit screen for the same student.

**Expected Result:**
- Admission number is displayed only where appropriate and is not editable.
- Saving student/admission edits does not change the admission number.

**Status:** NOT TESTED

## TC-ADMNO-003: Search by admission number

**Preconditions:**
- At least one admitted student has an admission number.

**Steps:**
1. Open Admissions list.
2. Search using the admission number.
3. Open Students list.
4. Search using the same admission number.
5. Open Receipts list after the student has a receipt.
6. Search using the same admission number.

**Expected Result:**
- Admissions list filters to the matching admission.
- Students list filters to the matching student.
- Receipts list filters to the student's receipts when admission number is present.

**Status:** NOT TESTED

## TC-ADMNO-004: Receipt prints receipt number and admission number

**Preconditions:**
- A student exists with an admission number.
- A student fee payment receipt exists for that student.

**Steps:**
1. Open Finance → Receipts.
2. Find the student receipt.
3. Click Print Receipt.

**Expected Result:**
- Printed receipt shows the receipt number.
- Printed receipt shows Admission No. with the student's admission number.
- If roll number is not generated yet, receipt still prints correctly with admission number.

**Status:** NOT TESTED

## TC-ADMNO-005: View number sequence registry

**Preconditions:**
- User has `NUMBER_SEQUENCE_VIEW` permission.
- At least one admission number or receipt number has been generated.

**Steps:**
1. Open Preferences → Number Sequences.
2. Search for `ADMISSION_NUMBER`.
3. Search for the academic-year scope, e.g. `2526`.

**Expected Result:**
- Number Sequences screen lists admission and receipt number series.
- Admission row shows last generated number and next preview number.
- Screen is read-only.

**Status:** NOT TESTED

## TC-ADMNO-006: Failed admission attempt does not consume admission number

**Preconditions:**
- Number Sequences screen shows current admission `lastSequence` for the target academic year.
- An enquiry exists that will fail admission completion validation, such as unverified mandatory documents.

**Steps:**
1. Attempt to complete admission for the invalid enquiry.
2. Confirm the admission completion fails.
3. Reopen Number Sequences.
4. Fix validation issues and complete admission successfully.

**Expected Result:**
- Failed attempt does not increment `lastSequence`.
- Successful retry generates exactly the next available admission number.

**Status:** NOT TESTED

