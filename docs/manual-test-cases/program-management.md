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
- Form shows: Program Name, Program Code, Duration (Years) fields
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

---

## TC-PROG-008: Add Program form — modern UI layout and field behaviour

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Application is running and navigated to Programs → Add Program

**Steps:**
1. Observe the Add Program page layout
2. Verify the page title ("Add Program") and subtitle text are visible beneath the back arrow
3. Verify no Material card shadow/elevation is present around the form
4. Verify each field has a label rendered **above** the input box (not floating inside the border)
5. Verify the "Program Code" and "Duration (Years)" fields each display a helper text line below the input
6. Click the "Program Name" input and verify a subtle indigo focus ring appears
7. Submit the form empty; verify inline error messages appear below the respective inputs
8. Fill in valid values and click "Create Program"; verify success snackbar and redirect to list

**Expected Result:**
- Flat form card with a whisper-thin border and no box shadow is displayed
- Labels are positioned above each input with a dark gray style
- Helper text renders as small muted text below the relevant inputs
- Focus state shows an indigo ring without heavy Material outline borders
- Error messages appear below the input (not inside a Material error slot)
- Successful submission redirects to the Programs list

**Status:** NOT TESTED

---

## TC-PROG-009: Add Program form — Cancel button navigates back

**Preconditions:**
- User is on the Add Program page

**Steps:**
1. Click the "Cancel" button
2. Observe navigation

**Expected Result:**
- User is navigated back to the Programs list without any data being saved

**Status:** NOT TESTED

---

## TC-PROG-010: Edit Program form — form pre-fills existing values

**Preconditions:**
- A program "Bachelor" with code "BACHELOR" and durationYears 4 exists
- User navigates to the Edit Program page for that program

**Steps:**
1. Observe the form fields on load
2. Verify the page title reads "Edit Program"
3. Verify the subtitle reads "Update the program details below."
4. Verify all three fields are pre-populated with existing values
5. Change the name and click "Update Program"

**Expected Result:**
- Form loads pre-filled values
- Successful update shows a snackbar and redirects to the list

**Status:** NOT TESTED

