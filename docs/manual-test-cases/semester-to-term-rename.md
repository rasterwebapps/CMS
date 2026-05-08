# Manual Test Cases — Semester to Term/Installment Rename

This document covers the refactoring that renamed all "semester" terminology to
neutral equivalents throughout the backend and frontend.

---

## TC-RENAME-001: Program list shows "Terms" instead of "Semesters"

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN
- At least one program exists

**Steps:**
1. Navigate to `/programs`
2. View the table column headers
3. Check the card view for each program

**Expected Result:**
- Column header shows "Terms" (not "Semesters")
- Card view shows term count, not semester count

**Status:** NOT TESTED

---

## TC-RENAME-002: Program form uses TERM_BASED assessment pattern

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN

**Steps:**
1. Navigate to `/programs/new`
2. Find the "Assessment Pattern" dropdown
3. Check the available options

**Expected Result:**
- Option label "Term-based (installment per term)" exists (value: TERM_BASED)
- Option label "Yearly" exists
- "Semester-wise" option no longer exists

**Status:** NOT TESTED

---

## TC-RENAME-003: Student form uses "Year of Study" field

**Preconditions:**
- User is logged in with ROLE_ADMIN or ROLE_COLLEGE_ADMIN

**Steps:**
1. Navigate to `/students/new`
2. Find the field previously labelled "Semester"

**Expected Result:**
- Field is labelled "Year of Study" (not "Semester")
- Field control name is `yearOfStudy`

**Status:** NOT TESTED

---

## TC-RENAME-004: Student list filter uses "Year of Study"

**Preconditions:**
- At least one student exists

**Steps:**
1. Navigate to `/students`
2. Check filter dropdowns
3. Check table column header for year column

**Expected Result:**
- Filter drop-down label says "Year of Study" or similar
- Column header is "Year of Study"
- Data in cells shows correct year values

**Status:** NOT TESTED

---

## TC-RENAME-005: Fee detail page shows "Installments" not "Semesters"

**Preconditions:**
- Student exists with a finalized fee allocation

**Steps:**
1. Navigate to `/finance/students/{id}`
2. Review the installments section

**Expected Result:**
- Section/rows show "installmentLabel" values (e.g., "First Installment", "Second Installment")
- No references to "semester" in labels

**Status:** NOT TESTED

---

## TC-RENAME-006: Collect payment shows installment breakdown

**Preconditions:**
- Student exists with pending fee balance

**Steps:**
1. Navigate to `/finance/students/{id}`
2. Click "Collect Payment"
3. Enter an amount and submit

**Expected Result:**
- Success toast references installment labels (not semester labels)
- Breakdown shows `installmentLabel` values

**Status:** NOT TESTED

---

## TC-RENAME-007: Receipt page uses installment fields

**Preconditions:**
- At least one receipt exists

**Steps:**
1. Navigate to `/finance/receipts`
2. Check the column header for the installments column

**Expected Result:**
- Column shows "Installments Covered" (not "Semesters Covered")
- Print receipt renders installment labels correctly

**Status:** NOT TESTED

---

## TC-RENAME-008: Enquiry fee status uses installment breakdown

**Preconditions:**
- An enquiry with FEES_FINALIZED status exists

**Steps:**
1. Navigate to `/finance/collection`
2. Select an enquiry entry
3. Review the breakdown rows

**Expected Result:**
- Rows show `installmentLabel` values
- No "semesterLabel" text visible

**Status:** NOT TESTED

---

## TC-RENAME-009: Curriculum map uses "Term" labels

**Preconditions:**
- A curriculum version exists with courses mapped

**Steps:**
1. Navigate to `/curriculum-versions`
2. Click "View Map" for any curriculum version

**Expected Result:**
- Cards show "Term 1", "Term 2", etc. (not "Semester 1", "Semester 2")
- Add course button works correctly

**Status:** NOT TESTED

---

## TC-RENAME-010: Backend JSON API returns renamed fields

**Preconditions:**
- Application is running

**Steps:**
1. `GET /api/v1/student-fees/{id}/semester-status`
2. Check the JSON response body
3. `GET /api/v1/programs`

**Expected Result:**
- `GET student-fees`: Response contains `installmentFees` array (not `semesterFees`),
  each item has `installmentLabel` and `sequence` (not `semesterLabel`/`semesterSequence`)
- `GET programs`: Response contains `totalTerms` (not `totalSemesters`),
  `assessmentPattern` values are `TERM_BASED` or `YEARLY`

**Status:** NOT TESTED

---

## TC-RENAME-011: Flyway migration V118 runs cleanly on PostgreSQL

**Preconditions:**
- Docker Compose services running (`docker compose up -d`)
- No existing database (or fresh schema)

**Steps:**
1. Run `SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun`
2. Check Flyway migration logs
3. Verify tables in PostgreSQL

**Expected Result:**
- V118 migration executes without errors
- Tables `installment_fees`, `term_results`, `curriculum_term_courses` exist
- Columns `term_number`, `installment_label`, `sequence`, `year_of_study` exist in respective tables
- `assessment_pattern = 'TERM_BASED'` for programs previously set to `'SEMESTER'`

**Status:** NOT TESTED

---

## TC-RENAME-012: Scholarship disbursement uses termNumber

**Preconditions:**
- A student scholarship exists in APPROVED status

**Steps:**
1. Navigate to `/scholarships`
2. Click "Disburse" for a scholarship
3. Check the disbursement frequency options

**Expected Result:**
- Disbursement frequency options include "Per Term" (not "Per Semester")
- Disbursement form field is labelled `termNumber`

**Status:** NOT TESTED

