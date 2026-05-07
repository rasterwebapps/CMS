# Manual Test Cases — Subject Management

## Prerequisites

- Frontend running (`ng serve`) at `http://localhost:4200`
- Backend running at `http://localhost:8080`
- Keycloak running with `cms` realm configured
- User logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`
- At least one course exists in the system
- At least one department exists in the system

---

## Subject CRUD Operations

### TC-SUBJ-001: Create Subject — Success

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN` (has `COURSE_MANAGE` permission)
- At least one course and department exist

**Steps:**
1. Send POST request to `/api/v1/subjects` with valid payload:
   ```json
   {
     "name": "Anatomy and Physiology",
     "code": "ANAT101",
     "credits": 4,
     "theoryHours": 3,
     "labHours": 2,
     "courseId": 1,
     "departmentId": 1,
     "semester": 1
   }
   ```
2. Verify response status is `201 Created`
3. Verify response body contains created subject with generated ID
4. Verify all fields match the request data

**Expected Result:**
- Subject is created successfully
- Response includes `id`, `name`, `code`, `credits`, `theoryHours`, `labHours`, `course`, `department`, `semester`, `createdAt`, `updatedAt`

**Status:** NOT TESTED

---

### TC-SUBJ-002: Create Subject — Validation Error

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`

**Steps:**
1. Send POST request to `/api/v1/subjects` with invalid payload (missing required fields):
   ```json
   {
     "code": "ANAT101"
   }
   ```
2. Verify response status is `400 Bad Request`
3. Verify error message indicates validation failures for required fields

**Expected Result:**
- Request is rejected with validation errors
- Error response includes details about missing `name`, `courseId`, `departmentId`, and `semester` fields

**Status:** NOT TESTED

---

### TC-SUBJ-003: Create Subject — Unauthorized Access

**Preconditions:**
- User is logged in with `ROLE_STUDENT` (does not have `COURSE_MANAGE` permission)

**Steps:**
1. Send POST request to `/api/v1/subjects` with valid payload
2. Verify response status is `403 Forbidden`

**Expected Result:**
- Request is denied due to insufficient permissions

**Status:** NOT TESTED

---

### TC-SUBJ-004: Get All Subjects — Success

**Preconditions:**
- User is logged in (any authenticated role)
- At least one subject exists in the system

**Steps:**
1. Send GET request to `/api/v1/subjects`
2. Verify response status is `200 OK`
3. Verify response body is an array of subjects
4. Verify each subject contains all expected fields

**Expected Result:**
- List of all subjects is returned
- Each subject includes nested `course` and `department` details

**Status:** NOT TESTED

---

### TC-SUBJ-005: Get All Subjects — Empty List

**Preconditions:**
- User is logged in
- No subjects exist in the system

**Steps:**
1. Send GET request to `/api/v1/subjects`
2. Verify response status is `200 OK`
3. Verify response body is an empty array

**Expected Result:**
- Empty array is returned

**Status:** NOT TESTED

---

### TC-SUBJ-006: Get Subject by ID — Success

**Preconditions:**
- User is logged in
- Subject with ID 1 exists

**Steps:**
1. Send GET request to `/api/v1/subjects/1`
2. Verify response status is `200 OK`
3. Verify response body contains the subject details
4. Verify nested `course` and `department` objects are populated

**Expected Result:**
- Subject details are returned with ID 1
- All fields are populated correctly

**Status:** NOT TESTED

---

### TC-SUBJ-007: Get Subject by ID — Not Found

**Preconditions:**
- User is logged in
- Subject with ID 999 does not exist

**Steps:**
1. Send GET request to `/api/v1/subjects/999`
2. Verify response status is `404 Not Found`
3. Verify error message indicates subject not found

**Expected Result:**
- Error response with message "Subject not found with id: 999"

**Status:** NOT TESTED

---

### TC-SUBJ-008: Get Subjects by Course ID — Success

**Preconditions:**
- User is logged in
- Course with ID 1 exists
- At least one subject is mapped to course ID 1

**Steps:**
1. Send GET request to `/api/v1/subjects/course/1`
2. Verify response status is `200 OK`
3. Verify response body is an array of subjects
4. Verify all returned subjects have `course.id` equal to 1

**Expected Result:**
- List of subjects for the specified course is returned

**Status:** NOT TESTED

---

### TC-SUBJ-009: Get Subjects by Course ID — Empty List

**Preconditions:**
- User is logged in
- Course with ID 5 exists but has no subjects

**Steps:**
1. Send GET request to `/api/v1/subjects/course/5`
2. Verify response status is `200 OK`
3. Verify response body is an empty array

**Expected Result:**
- Empty array is returned

**Status:** NOT TESTED

---

### TC-SUBJ-010: Get Subjects by Department ID — Success

**Preconditions:**
- User is logged in
- Department with ID 1 exists
- At least one subject is mapped to department ID 1

**Steps:**
1. Send GET request to `/api/v1/subjects/department/1`
2. Verify response status is `200 OK`
3. Verify response body is an array of subjects
4. Verify all returned subjects have `department.id` equal to 1

**Expected Result:**
- List of subjects for the specified department is returned

**Status:** NOT TESTED

---

### TC-SUBJ-011: Get Subjects by Department ID — Empty List

**Preconditions:**
- User is logged in
- Department with ID 5 exists but has no subjects

**Steps:**
1. Send GET request to `/api/v1/subjects/department/5`
2. Verify response status is `200 OK`
3. Verify response body is an empty array

**Expected Result:**
- Empty array is returned

**Status:** NOT TESTED

---

### TC-SUBJ-012: Update Subject — Success

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`
- Subject with ID 1 exists

**Steps:**
1. Send PUT request to `/api/v1/subjects/1` with updated payload:
   ```json
   {
     "name": "Human Anatomy and Physiology",
     "code": "ANAT101",
     "credits": 5,
     "theoryHours": 4,
     "labHours": 2,
     "courseId": 1,
     "departmentId": 1,
     "semester": 1
   }
   ```
2. Verify response status is `200 OK`
3. Verify response body contains updated subject
4. Verify `name` is now "Human Anatomy and Physiology"
5. Verify `credits` is now 5
6. Verify `theoryHours` is now 4
7. Verify `updatedAt` timestamp has changed

**Expected Result:**
- Subject is updated successfully
- All modified fields reflect the new values

**Status:** NOT TESTED

---

### TC-SUBJ-013: Update Subject — Not Found

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`
- Subject with ID 999 does not exist

**Steps:**
1. Send PUT request to `/api/v1/subjects/999` with valid payload
2. Verify response status is `404 Not Found`
3. Verify error message indicates subject not found

**Expected Result:**
- Error response with message "Subject not found with id: 999"

**Status:** NOT TESTED

---

### TC-SUBJ-014: Update Subject — Unauthorized Access

**Preconditions:**
- User is logged in with `ROLE_FACULTY` (does not have `COURSE_MANAGE` permission)
- Subject with ID 1 exists

**Steps:**
1. Send PUT request to `/api/v1/subjects/1` with valid payload
2. Verify response status is `403 Forbidden`

**Expected Result:**
- Request is denied due to insufficient permissions

**Status:** NOT TESTED

---

### TC-SUBJ-015: Delete Subject — Success

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`
- Subject with ID 1 exists

**Steps:**
1. Send DELETE request to `/api/v1/subjects/1`
2. Verify response status is `204 No Content`
3. Send GET request to `/api/v1/subjects/1`
4. Verify response status is `404 Not Found`

**Expected Result:**
- Subject is deleted successfully
- Subject no longer exists in the system

**Status:** NOT TESTED

---

### TC-SUBJ-016: Delete Subject — Not Found

**Preconditions:**
- User is logged in with `ROLE_ADMIN` or `ROLE_COLLEGE_ADMIN`
- Subject with ID 999 does not exist

**Steps:**
1. Send DELETE request to `/api/v1/subjects/999`
2. Verify response status is `404 Not Found`
3. Verify error message indicates subject not found

**Expected Result:**
- Error response with message "Subject not found with id: 999"

**Status:** NOT TESTED

---

### TC-SUBJ-017: Delete Subject — Unauthorized Access

**Preconditions:**
- User is logged in with `ROLE_STUDENT` (does not have `COURSE_MANAGE` permission)
- Subject with ID 1 exists

**Steps:**
1. Send DELETE request to `/api/v1/subjects/1`
2. Verify response status is `403 Forbidden`

**Expected Result:**
- Request is denied due to insufficient permissions

**Status:** NOT TESTED

---

## Subject Business Rules

### TC-SUBJ-018: Subject Code Uniqueness

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Subject with code "ANAT101" already exists

**Steps:**
1. Attempt to create a new subject with the same code "ANAT101"
2. Verify the system prevents duplicate subject codes (either via unique constraint or validation)

**Expected Result:**
- Duplicate subject codes are not allowed
- Error message indicates code must be unique

**Status:** NOT TESTED

---

### TC-SUBJ-019: Credits Calculation

**Preconditions:**
- User is logged in with `ROLE_ADMIN`

**Steps:**
1. Create a subject with `credits: 4`, `theoryHours: 3`, `labHours: 2`
2. Verify the total credits align with the theory and lab hours

**Expected Result:**
- Credits are correctly set based on theory and lab hours
- The system accepts the specified credits value

**Status:** NOT TESTED

---

### TC-SUBJ-020: Semester Validation

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Course with 4 semesters (2 years) exists

**Steps:**
1. Attempt to create a subject with `semester: 10` for a 4-semester course
2. Verify the system validates semester against course duration (if applicable)

**Expected Result:**
- If validation exists, semester must be within valid range for the course
- If no validation, subject is created with any semester value

**Status:** NOT TESTED

---

## Integration Tests

### TC-SUBJ-021: Subject with Course and Department

**Preconditions:**
- User is logged in with `ROLE_ADMIN`
- Course with ID 1 exists (name: "B.Sc. Nursing", code: "BSCN")
- Department with ID 1 exists (name: "Nursing", code: "MSN")

**Steps:**
1. Create a subject linked to course ID 1 and department ID 1
2. Get the created subject by ID
3. Verify the response includes nested `course` object with course details
4. Verify the response includes nested `department` object with department details

**Expected Result:**
- Subject response includes full course and department details
- Course name, code, and program information are populated
- Department name and code are populated

**Status:** NOT TESTED

---

### TC-SUBJ-022: Subject List Filtering by Course

**Preconditions:**
- User is logged in
- Multiple subjects exist for different courses
- Course ID 1 has 3 subjects
- Course ID 2 has 2 subjects

**Steps:**
1. Get all subjects for course ID 1: `GET /subjects/course/1`
2. Verify 3 subjects are returned
3. Get all subjects for course ID 2: `GET /subjects/course/2`
4. Verify 2 subjects are returned

**Expected Result:**
- Filtering by course ID returns only subjects for that specific course
- Each filtered list contains the correct number of subjects

**Status:** NOT TESTED

---

### TC-SUBJ-023: Subject List Filtering by Department

**Preconditions:**
- User is logged in
- Multiple subjects exist for different departments
- Department ID 1 has 5 subjects
- Department ID 2 has 3 subjects

**Steps:**
1. Get all subjects for department ID 1: `GET /subjects/department/1`
2. Verify 5 subjects are returned
3. Get all subjects for department ID 2: `GET /subjects/department/2`
4. Verify 3 subjects are returned

**Expected Result:**
- Filtering by department ID returns only subjects for that specific department
- Each filtered list contains the correct number of subjects

**Status:** NOT TESTED

---

## Summary

| Test Case ID | Category | Description | Status |
|--------------|----------|-------------|--------|
| TC-SUBJ-001 | CRUD | Create Subject — Success | NOT TESTED |
| TC-SUBJ-002 | CRUD | Create Subject — Validation Error | NOT TESTED |
| TC-SUBJ-003 | CRUD | Create Subject — Unauthorized Access | NOT TESTED |
| TC-SUBJ-004 | CRUD | Get All Subjects — Success | NOT TESTED |
| TC-SUBJ-005 | CRUD | Get All Subjects — Empty List | NOT TESTED |
| TC-SUBJ-006 | CRUD | Get Subject by ID — Success | NOT TESTED |
| TC-SUBJ-007 | CRUD | Get Subject by ID — Not Found | NOT TESTED |
| TC-SUBJ-008 | CRUD | Get Subjects by Course ID — Success | NOT TESTED |
| TC-SUBJ-009 | CRUD | Get Subjects by Course ID — Empty List | NOT TESTED |
| TC-SUBJ-010 | CRUD | Get Subjects by Department ID — Success | NOT TESTED |
| TC-SUBJ-011 | CRUD | Get Subjects by Department ID — Empty List | NOT TESTED |
| TC-SUBJ-012 | CRUD | Update Subject — Success | NOT TESTED |
| TC-SUBJ-013 | CRUD | Update Subject — Not Found | NOT TESTED |
| TC-SUBJ-014 | CRUD | Update Subject — Unauthorized Access | NOT TESTED |
| TC-SUBJ-015 | CRUD | Delete Subject — Success | NOT TESTED |
| TC-SUBJ-016 | CRUD | Delete Subject — Not Found | NOT TESTED |
| TC-SUBJ-017 | CRUD | Delete Subject — Unauthorized Access | NOT TESTED |
| TC-SUBJ-018 | Business Rules | Subject Code Uniqueness | NOT TESTED |
| TC-SUBJ-019 | Business Rules | Credits Calculation | NOT TESTED |
| TC-SUBJ-020 | Business Rules | Semester Validation | NOT TESTED |
| TC-SUBJ-021 | Integration | Subject with Course and Department | NOT TESTED |
| TC-SUBJ-022 | Integration | Subject List Filtering by Course | NOT TESTED |
| TC-SUBJ-023 | Integration | Subject List Filtering by Department | NOT TESTED |

**Total Test Cases:** 23  
**Passed:** 0  
**Failed:** 0  
**Not Tested:** 23

