# Manual Test Cases: Program Management

## TC-PROG-001: Create a new program (Bachelor)

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Application is running

**Steps:**
1. Send a POST request to `/programs` with body: `{"name": "Bachelor", "code": "BACHELOR", "durationYears": 4, "departmentIds": []}`
2. Verify the response status is 201 Created
3. Verify the response body contains `id`, `name: "Bachelor"`, `code: "BACHELOR"`, `durationYears: 4`
4. Confirm no `programLevel` field is present in the response

**Expected Result:**
- Program is created successfully; response contains name/code/durationYears only

**Status:** NOT TESTED

---

## TC-PROG-002: Programs are seeded as Bachelor, Master, Diploma, Certificate, Doctoral

**Preconditions:**
- Application is started fresh (local profile with H2)

**Steps:**
1. Send a GET request to `/programs`
2. Inspect the list of programs

**Expected Result:**
- Response contains five programs: Bachelor, Master, Diploma, Certificate, Doctoral

**Status:** NOT TESTED

---

## TC-PROG-003: Add Program form has no Program Level dropdown

**Preconditions:**
- User is logged in and navigates to the frontend Add Program page

**Steps:**
1. Open the Angular application and navigate to Programs → Add Program
2. Inspect the form fields

**Expected Result:**
- Form shows: Name, Code, Duration (Years), Departments fields
- No "Program Level" dropdown is present

**Status:** NOT TESTED

---

## TC-PROG-004: Program list page shows no Program Level column

**Preconditions:**
- User is logged in

**Steps:**
1. Navigate to the Programs list page

**Expected Result:**
- Table columns shown: Code, Name, Duration, Departments, Actions
- No "Program Level" column is present

**Status:** NOT TESTED

---

## TC-PROG-005: Create a program without departments

**Preconditions:**
- User is logged in with ROLE_ADMIN

**Steps:**
1. Send a POST request to `/programs` with body: `{"name": "Doctoral", "code": "DOCTORAL", "durationYears": 3, "departmentIds": []}`
2. Verify the response status is 201 Created

**Expected Result:**
- Program is created with empty departments list

**Status:** NOT TESTED

---

## TC-PROG-006: Update a program

**Preconditions:**
- A program with id=1 exists

**Steps:**
1. Send a PUT request to `/programs/1` with body: `{"name": "Bachelor Updated", "code": "BACHELOR", "durationYears": 4, "departmentIds": []}`
2. Verify the response status is 200 OK
3. Verify the response body shows the updated name

**Expected Result:**
- Program is updated successfully with no programLevel field required

**Status:** NOT TESTED

---

## TC-PROG-007: Course still references Program correctly

**Preconditions:**
- Programs are seeded (Bachelor, Master, etc.)
- Courses exist referencing those programs

**Steps:**
1. Send a GET request to `/courses`
2. Inspect the `program` object within each course response

**Expected Result:**
- Each course's `program` contains: `id`, `name`, `code`, `durationYears`, `departments` — no `programLevel` field

**Status:** NOT TESTED
