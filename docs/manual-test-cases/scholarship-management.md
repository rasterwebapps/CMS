# Scholarship Management — Manual Test Cases

## TC-SCHOL-001: Create a new scholarship type

**Preconditions:**
- User is logged in with a role that has `SCHOLARSHIP_MANAGE`.
- Backend and frontend are running against PostgreSQL.

**Steps:**
1. Navigate to Finance → Scholarships.
2. Click **Add Scholarship**.
3. Enter code `TEST_SCHOL`, name `Test Scholarship`, discount type `FIXED_AMOUNT`, value `10000`.
4. Save the form.
5. Return to the scholarship list.

**Expected Result:**
- Scholarship is created successfully and appears in the list.

**Status:** NOT TESTED

## TC-SCHOL-002: Mark student as First Graduate and detect FIRST_GRAD eligibility

**Preconditions:**
- User has `STUDENT_EDIT` and `SCHOLARSHIP_VIEW` permissions.
- At least one student exists.

**Steps:**
1. Edit a student.
2. In Scholarship Eligibility, tick **First Graduate in Family**.
3. Fill father/mother education.
4. Save the student.
5. Open the student detail page and go to the **Scholarships** tab.

**Expected Result:**
- First Graduate shows `Yes`.
- `FIRST_GRAD` appears in eligible scholarships.

**Status:** NOT TESTED

## TC-SCHOL-003: SC community detects SC_GOVT eligibility

**Preconditions:**
- User has `STUDENT_EDIT` and `SCHOLARSHIP_VIEW` permissions.

**Steps:**
1. Edit a student.
2. Set Community Category to `SC`.
3. Save the student.
4. Open Scholarships tab.

**Expected Result:**
- `SC_GOVT` appears in eligible scholarships.

**Status:** NOT TESTED

## TC-SCHOL-004: Income below ₹3L detects EWS eligibility

**Preconditions:**
- User has `SCHOLARSHIP_MANAGE` permission.

**Steps:**
1. Open a student's Scholarships tab.
2. Update eligibility with annual family income less than `300000` using the API/UI.
3. Reload the Scholarships tab.

**Expected Result:**
- `EWS` appears in eligible scholarships.

**Status:** NOT TESTED

## TC-SCHOL-005: Apply for scholarship

**Preconditions:**
- Student has at least one eligible scholarship.
- User has `SCHOLARSHIP_APPLY` permission.

**Steps:**
1. Open the student's Scholarships tab.
2. Click **Apply** for an eligible scholarship.
3. Confirm the application is submitted.

**Expected Result:**
- Application appears in the Applications table with status `PENDING`.

**Status:** NOT TESTED

## TC-SCHOL-006: Approve scholarship application

**Preconditions:**
- A `PENDING` application exists.
- User has `SCHOLARSHIP_APPROVE` permission.

**Steps:**
1. Navigate to Finance → Scholarship Applications.
2. Click approve for the pending application.
3. Enter an approved amount.
4. Reload the student's Scholarships tab.

**Expected Result:**
- Application status changes to `APPROVED`.
- Approved amount is saved.

**Status:** NOT TESTED

## TC-SCHOL-007: Reject scholarship application

**Preconditions:**
- A `PENDING` application exists.
- User has `SCHOLARSHIP_APPROVE` permission.

**Steps:**
1. Navigate to Finance → Scholarship Applications.
2. Click reject for the pending application.
3. Enter a rejection reason.

**Expected Result:**
- Application status changes to `REJECTED`.
- Rejection reason is saved.

**Status:** NOT TESTED

## TC-SCHOL-008: Prevent second scholarship in same academic year

**Preconditions:**
- Student already has a scholarship application for current academic year.
- User has `SCHOLARSHIP_APPLY` permission.

**Steps:**
1. Try applying for another scholarship for the same student and academic year.

**Expected Result:**
- Request is rejected with a validation error.
- No second application is created.

**Status:** NOT TESTED

## TC-SCHOL-009: Finalize student fee with approved scholarship

**Preconditions:**
- Student has one approved scholarship for current academic year.
- User has `STUDENT_FEE_MANAGE` permission.

**Steps:**
1. Finalize fee for the student using `/api/v1/student-fees/finalize`.
2. Include any manual discount or no manual discount.
3. Fetch semester breakdown for the student.

**Expected Result:**
- Approved scholarship amount is added to total discount.
- `scholarshipApplicationId`, `scholarshipDiscountAmount`, and `scholarshipDiscountReason` are present in the response.
- Net fee is reduced by scholarship amount.

**Status:** NOT TESTED

## TC-SCHOL-010: Record disbursement

**Preconditions:**
- Scholarship application is `APPROVED`.
- User has `SCHOLARSHIP_DISBURSE` permission.

**Steps:**
1. Call disbursement action for the approved application.
2. Enter amount, date, mode, and reference.
3. Open student's Scholarships tab.

**Expected Result:**
- Disbursement appears in the Disbursements table.

**Status:** NOT TESTED

## TC-SCHOL-011: View scholarship history grouped by academic year

**Preconditions:**
- Student has scholarship applications in one or more academic years.

**Steps:**
1. Open student detail.
2. Open Scholarships tab.

**Expected Result:**
- Applications show academic year, scholarship name, status, approved amount, and applied date.

**Status:** NOT TESTED

## TC-SCHOL-012: Renew approved renewable scholarship

**Preconditions:**
- Student has an approved `SC_GOVT`, `ST_GOVT`, `OBC_GOVT`, `BC_STATE`, or `EWS` scholarship.
- Next academic year exists.
- User has `SCHOLARSHIP_APPLY` permission.

**Steps:**
1. Click **Renew** for the approved renewable scholarship.
2. Reload the Scholarships tab.

**Expected Result:**
- A new application is created for next academic year with status `PENDING`.
- `renewedFromId` points to the source application.

**Status:** NOT TESTED

## TC-SCHOL-013: Block renewal for non-renewable scholarship

**Preconditions:**
- Student has approved `FIRST_GRAD` or `MERIT` scholarship.

**Steps:**
1. Attempt renewal using the renew endpoint.

**Expected Result:**
- Request is rejected because `renewalRequired = false`.

**Status:** NOT TESTED

## TC-SCHOL-014: Front Office cannot access approval queue

**Preconditions:**
- User has Front Office role without `SCHOLARSHIP_APPROVE`.

**Steps:**
1. Try to open `/scholarship-applications`.
2. Try to call `/api/v1/scholarship-applications`.

**Expected Result:**
- UI route redirects/blocks access.
- Backend returns 403.

**Status:** NOT TESTED

## TC-SCHOL-015: Student can only view own scholarships

**Preconditions:**
- Student login exists.
- Two students exist.

**Steps:**
1. Login as a student.
2. View own scholarship records.
3. Attempt to view another student's scholarship records.

**Expected Result:**
- Own records are visible.
- Other student's records are denied.

**Status:** NOT TESTED

