# Phase 3 Student Management - Manual Test Cases

## TC-COHORT-001: Confirm Admission - Creates New Cohort

**Preconditions:**
- Application running with ROLE_ADMIN or ROLE_FRONT_OFFICE token
- A Student and Program exist (programId=1, code="BCA", durationYears=3)
- An Admission exists for that student (admissionId=1)
- An IntakeRule exists for BCA with admissionWindowStartDate=2024-06-01, admissionWindowEndDate=2024-08-31, mappedAcademicYear="2024-2025", isActive=true

**Steps:**
1. POST `/admissions/1/confirm?admissionDate=2024-06-15`
2. Verify response is 200 OK
3. Inspect the response body

**Expected Result:**
- `studentId` matches the student's ID
- `cohortCode` is `BCA-2024-2027`
- `cohortDisplayName` is `BCA Program (2024-2027)`
- `firstSemesterNumber` is 1
- A new cohort row appears in the `cohorts` table

**Status:** NOT TESTED

---

## TC-COHORT-002: Confirm Admission - No Matching Intake Rule

**Preconditions:**
- Admission exists for student with program "BCA"
- No IntakeRule covers the date 2023-01-01

**Steps:**
1. POST `/admissions/1/confirm?admissionDate=2023-01-01`
2. Observe response

**Expected Result:**
- Response status is 422 Unprocessable Entity
- Error message contains "No active intake rule found for program BCA"

**Status:** NOT TESTED

---

## TC-ENROLL-001: Generate Enrollments When Term Instance Opens

**Preconditions:**
- Active cohort exists with admissionAcademicYear="2024-2025"
- Active students are assigned to the cohort
- A term instance exists for "2024-2025" ODD with status PLANNED (id=1)

**Steps:**
1. PUT `/term-instances/1` with body `{"status": "OPEN"}`
2. Verify response is 200 OK
3. Query `GET /api/student-term-enrollments?termInstanceId=1`

**Expected Result:**
- Term instance status is now OPEN
- Enrollments are generated for all active students in the cohort
- Each enrollment has semesterNumber=1, yearOfStudy=1

**Status:** NOT TESTED

---

## TC-ENROLL-002: Generate Enrollments - Manual Trigger

**Preconditions:**
- Active cohort with students exists
- A term instance exists with id=1

**Steps:**
1. POST `/api/student-term-enrollments/generate?termInstanceId=1`
2. Verify response is 200 OK

**Expected Result:**
- Response contains `{"enrollmentsCreated": N}` where N > 0

**Status:** NOT TESTED

---

## TC-ENROLL-003: Get Enrollments by Term Instance

**Preconditions:**
- Enrollments exist for termInstanceId=1

**Steps:**
1. GET `/api/student-term-enrollments?termInstanceId=1`
2. Verify response

**Expected Result:**
- Returns a list of enrollment objects with studentId, cohortCode, semesterNumber, status=ENROLLED

**Status:** NOT TESTED

---

## TC-ENROLL-004: Get Enrollments by Student

**Preconditions:**
- Enrollments exist for studentId=1

**Steps:**
1. GET `/api/student-term-enrollments?studentId=1`

**Expected Result:**
- Returns all enrollments for the student

**Status:** NOT TESTED

---

## TC-ENROLL-005: Get Enrollments - Bad Request (no params)

**Steps:**
1. GET `/api/student-term-enrollments`

**Expected Result:**
- Response status is 400 Bad Request

**Status:** NOT TESTED

---

## TC-COHORT-003: Confirm Admission - Reuses Existing Cohort

**Preconditions:**
- A cohort "BCA-2024-2027" already exists for program BCA and academic year 2024-2025
- A second student with admission is being confirmed for the same program and year

**Steps:**
1. POST `/admissions/2/confirm?admissionDate=2024-07-01`

**Expected Result:**
- No new cohort is created
- The existing cohort is assigned to the student
- `cohortCode` in response is `BCA-2024-2027`

**Status:** NOT TESTED
